package pe.edu.vallegrande.app.service.impl;

import pe.edu.vallegrande.app.dto.SolicitudRequest;
import pe.edu.vallegrande.app.model.Solicitud;
import pe.edu.vallegrande.app.model.Solicitante;
import pe.edu.vallegrande.app.model.SeguimientoTramite;
import pe.edu.vallegrande.app.repository.SolicitudRepository;
import pe.edu.vallegrande.app.repository.SolicitanteRepository;
import pe.edu.vallegrande.app.repository.SeguimientoTramiteRepository;
import pe.edu.vallegrande.app.repository.TipoTramiteRepository;
import pe.edu.vallegrande.app.service.SolicitudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Implementation of the SolicitudService.
 * Handles all FUT request operations including registration, querying,
 * status updates, logical deletion, and restoration.
 */
@Service
public class SolicitudServiceImpl implements SolicitudService {

    private static final Set<String> VALID_STATUSES = Set.of(
            "RECIBIDO",
            "EN_REVISION",
            "OBSERVADO",
            "ATENDIDO",
            "RECHAZADO"
    );

    private final SolicitudRepository solicitudRepository;
    private final SolicitanteRepository solicitanteRepository;
    private final SeguimientoTramiteRepository seguimientoRepository;
    private final TipoTramiteRepository tipoTramiteRepository;

    public SolicitudServiceImpl(
            SolicitudRepository solicitudRepository,
            SolicitanteRepository solicitanteRepository,
            SeguimientoTramiteRepository seguimientoRepository,
            TipoTramiteRepository tipoTramiteRepository) {
        this.solicitudRepository = solicitudRepository;
        this.solicitanteRepository = solicitanteRepository;
        this.seguimientoRepository = seguimientoRepository;
        this.tipoTramiteRepository = tipoTramiteRepository;
    }

    // =========================================================
    // REGISTER FUT REQUEST
    // =========================================================

    @Override
    @Transactional
    public Map<String, Object> create(SolicitudRequest request) {
        validateRequest(request);
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Optional<Solicitud> existing = solicitudRepository.findByIdempotencyKey(request.getIdempotencyKey().trim());
            if (existing.isPresent()) {
                return detail(existing.get().getIdSolicitud());
            }
        }

        LocalDate declaredDate = parseDeclaredDate(request.getDeclaredDate());
        String[] names = splitName(request.getFullName());

        // Find the applicant by document number or register a new one.
        Solicitante person = solicitanteRepository
                .findByNumeroDocumento(request.getDni().trim())
                .orElseGet(() -> {
                    Solicitante newPerson = new Solicitante();
                    String documentType = normalizeDocumentType(request.getTipoDocumento());
                    Integer documentTypeId = solicitanteRepository
                            .findTipoDocumentoIdByCodigo(documentType)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Document type " + documentType
                                            + " does not exist in dbo.tipo_documento. Please run the compatibility migration."));
                    newPerson.setIdTipoDocumento(documentTypeId);
                    Integer ubigeoId = solicitanteRepository.findUbigeoId(
                            valueOr(request.getDepartment(), "Lima"),
                            request.getProvince().trim(),
                            request.getDistrict().trim())
                            .orElse(1);
                    newPerson.setIdUbigeo(ubigeoId);
                    newPerson.setNumeroDocumento(request.getDni().trim());
                    newPerson.setNombres(names[0]);
                    newPerson.setApellidoPaterno(names[1]);
                    newPerson.setApellidoMaterno(names[2]);
                    newPerson.setTelefono(request.getCellphone().trim());
                    newPerson.setCorreo(blankToNull(request.getEmail()));
                    newPerson.setDireccion(request.getAddress().trim());
                    newPerson.setEstado("A");
                    newPerson.setFechaRegistro(LocalDateTime.now());
                    return solicitanteRepository.save(newPerson);
                });

        // Update the applicant's data.
        person.setIdTipoDocumento(solicitanteRepository
                .findTipoDocumentoIdByCodigo(normalizeDocumentType(request.getTipoDocumento()))
                .orElseThrow(() -> new IllegalStateException(
                        "Document type does not exist in dbo.tipo_documento. Please run the compatibility migration.")));
        // Fill in names/last names if blank (legacy applicant with incomplete data)
        if (person.getNombres() == null || person.getNombres().isBlank()) {
            person.setNombres(names[0]);
        }
        if (person.getApellidoPaterno() == null || person.getApellidoPaterno().isBlank()) {
            person.setApellidoPaterno(names[1]);
        }
        if (person.getApellidoMaterno() == null || person.getApellidoMaterno().isBlank()) {
            person.setApellidoMaterno(names[2]);
        }
        person.setTelefono(request.getCellphone().trim());
        person.setCorreo(blankToNull(request.getEmail()));
        person.setDireccion(request.getAddress().trim());
        person.setFechaActualizacion(LocalDateTime.now());
        person = solicitanteRepository.save(person);

        // Create the FUT request.
        Solicitud solicitud = new Solicitud();
        solicitud.setIdSolicitante(person.getIdSolicitante());
        solicitud.setCodigoSeguimiento(newTrackingCode());
        solicitud.setSolicito(request.getSolicitante().trim());
        solicitud.setNombreCompleto(request.getFullName().trim());
        solicitud.setDni(request.getDni().trim());
        applyAdditionalFormFields(solicitud, request);
        solicitud.setIdempotencyKey(blankToNull(request.getIdempotencyKey()));
        solicitud.setCorreo(blankToNull(request.getEmail()));
        solicitud.setCelular(request.getCellphone().trim());
        solicitud.setDomicilio(request.getAddress().trim());
        solicitud.setDistrito(request.getDistrict().trim());
        solicitud.setProvincia(request.getProvince().trim());
        solicitud.setFundamento(request.getBasis().trim());
        solicitud.setDocumentosAdjuntos(blankToNull(request.getAttachedDocuments()));
        solicitud.setFechaDeclarada(declaredDate);
        String requestedType = blankToNull(request.getTipoTramite());
        if (requestedType == null) requestedType = blankToNull(request.getSolicitante());
        solicitud.setIdTipoTramite(requestedType == null ? null : tipoTramiteRepository
                .findByNombreIgnoreCase(requestedType)
                .map(pe.edu.vallegrande.app.model.TipoTramite::getIdTipoTramite)
                .orElse(null));
        solicitud.setNotaFirma(blankToNull(request.getSignatureNote()));
        solicitud.setEstado("RECIBIDO");

        // Save the request.
        solicitud = solicitudRepository.save(solicitud);

        // Record history entry.
        addHistory(solicitud.getIdSolicitud(), null, "RECIBIDO", "Request registered.");

        // Return detail including email.
        return detail(solicitud.getIdSolicitud());
    }

    // =========================================================
    // QUERY STATUS BY TRACKING CODE
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> track(String code) {
        Solicitud solicitud = solicitudRepository
                .findByCodigoSeguimientoIgnoreCase(code.trim())
                .orElseThrow(() -> new NoSuchElementException(
                        "No request found with that tracking code."));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("trackingCode", solicitud.getCodigoSeguimiento());
        response.put("status", solicitud.getEstado());
        response.put("createdAt", solicitud.getFechaRegistro());
        return response;
    }

    // =========================================================
    // LIST REQUESTS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String status) {
        List<Solicitud> records;
        if (status == null || status.isBlank()) {
            records = solicitudRepository.findAllByOrderByFechaRegistroDesc();
        } else {
            records = solicitudRepository.findByEstadoIgnoreCaseOrderByFechaRegistroDesc(status.trim());
        }

        return records.stream()
                .filter(s -> s.getFechaEliminacion() == null)
                .map(this::summary)
                .toList();
    }

    // =========================================================
    // RETRIEVE REQUEST DETAIL
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long id) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Request not found with ID " + id));
        Map<String, Object> result = summary(solicitud);
        result.put("procedureRequested", solicitud.getSolicito());
        result.put("fullName", solicitud.getNombreCompleto());
        result.put("documentNumber", solicitud.getDni());

        // Retrieve email stored in the request.
        String email = solicitud.getCorreo();

        // If no email in the request, look it up in the applicant table.
        if ((email == null || email.isBlank())
                && solicitud.getIdSolicitante() != null) {
            email = solicitanteRepository
                    .findById(solicitud.getIdSolicitante())
                    .map(Solicitante::getCorreo)
                    .orElse(null);
        }

        result.put("email", email);
        result.put("mobilePhone", solicitud.getCelular());
        result.put("homeAddress", solicitud.getDomicilio());
        result.put("district", solicitud.getDistrito());
        result.put("province", solicitud.getProvincia());
        result.put("basis", solicitud.getFundamento());
        result.put("attachedDocuments", solicitud.getDocumentosAdjuntos());
        result.put("declaredDate", solicitud.getFechaDeclarada());
        result.put("signatureNote", solicitud.getNotaFirma());
        result.put("observation", solicitud.getObservacion());

        // Status change history.
        result.put(
                "history",
                seguimientoRepository
                        .findByIdSolicitudOrderByFechaCambioDesc(id)
                        .stream()
                        .map(history -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("changedAt", history.getFechaCambio());
                            row.put("previousStatus", history.getEstadoAnterior());
                            row.put("newStatus", history.getEstadoNuevo());
                            row.put("observation", history.getObservacion());
                            return row;
                        })
                        .toList()
        );
        return result;
    }

    // =========================================================
    // UPDATE STATUS
    // =========================================================

    @Override
    @Transactional
    public Map<String, Object> updateStatus(Long id, String status, String observation) {
        String normalized = status == null
                ? ""
                : status.trim().toUpperCase(Locale.ROOT);
        if (!VALID_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException(
                    "Invalid status. Allowed values: " + VALID_STATUSES);
        }

        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Request not found with ID " + id));
        String oldStatus = solicitud.getEstado();
        solicitud.setEstado(normalized);
        solicitud.setObservacion(blankToNull(observation));
        solicitudRepository.save(solicitud);
        addHistory(id, oldStatus, normalized, observation);
        return detail(id);
    }

    // =========================================================
    // UPDATE REQUEST DATA
    // =========================================================

    @Override
    @Transactional
    public Map<String, Object> update(Long id, SolicitudRequest request) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Request not found with ID " + id));
        if (solicitud.getFechaEliminacion() != null) {
            throw new IllegalArgumentException(
                    "Cannot edit a deleted request; restore it first.");
        }

        validateRequest(request);
        solicitud.setSolicito(request.getSolicitante().trim());
        String requestedType = blankToNull(request.getTipoTramite());
        if (requestedType == null) requestedType = blankToNull(request.getSolicitante());
        solicitud.setIdTipoTramite(requestedType == null ? null : tipoTramiteRepository
                .findByNombreIgnoreCase(requestedType)
                .map(pe.edu.vallegrande.app.model.TipoTramite::getIdTipoTramite)
                .orElse(null));
        solicitud.setNombreCompleto(request.getFullName().trim());
        solicitud.setDni(request.getDni().trim());
        solicitud.setCorreo(blankToNull(request.getEmail()));
        solicitud.setCelular(request.getCellphone().trim());
        solicitud.setDomicilio(request.getAddress().trim());
        solicitud.setDistrito(request.getDistrict().trim());
        solicitud.setProvincia(request.getProvince().trim());
        solicitud.setFundamento(request.getBasis().trim());
        solicitud.setDocumentosAdjuntos(blankToNull(request.getAttachedDocuments()));
        if (request.getDeclaredDate() != null && !request.getDeclaredDate().isBlank()) {
            LocalDate date = parseDeclaredDate(request.getDeclaredDate());
            solicitud.setFechaDeclarada(date);
        }
        solicitud.setNotaFirma(blankToNull(request.getSignatureNote()));
        applyAdditionalFormFields(solicitud, request);

        // Update related applicant data.
        if (solicitud.getIdSolicitante() != null) {
            Solicitante person = solicitanteRepository
                    .findById(solicitud.getIdSolicitante())
                    .orElse(null);
            if (person != null) {
                String[] names = splitName(request.getFullName());
                person.setNombres(names[0]);
                person.setApellidoPaterno(names[1]);
                person.setApellidoMaterno(names[2]);
                person.setNumeroDocumento(request.getDni().trim());
                person.setIdTipoDocumento(solicitanteRepository
                        .findTipoDocumentoIdByCodigo(normalizeDocumentType(request.getTipoDocumento()))
                        .orElseThrow(() -> new IllegalStateException(
                                "Document type does not exist in dbo.tipo_documento. Please run the compatibility migration.")));
                person.setTelefono(request.getCellphone().trim());
                person.setCorreo(blankToNull(request.getEmail()));
                person.setDireccion(request.getAddress().trim());
                person.setFechaActualizacion(LocalDateTime.now());
                solicitanteRepository.save(person);
            }
        }

        solicitud.setFechaActualizacion(LocalDateTime.now());
        solicitudRepository.save(solicitud);
        addHistory(id, solicitud.getEstado(), solicitud.getEstado(), "Request data updated.");
        return detail(id);
    }

    // =========================================================
    // LOGICAL DELETION
    // =========================================================

    @Override
    @Transactional
    public void delete(Long id) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Request not found with ID " + id));
        if (solicitud.getFechaEliminacion() == null) {
            solicitud.setFechaEliminacion(LocalDateTime.now());
            solicitudRepository.save(solicitud);
            addHistory(id, solicitud.getEstado(), solicitud.getEstado(), "Request logically deleted.");
        }
    }

    // =========================================================
    // RESTORE REQUEST
    // =========================================================

    @Override
    @Transactional
    public Map<String, Object> restore(Long id) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Request not found with ID " + id));
        if (solicitud.getFechaEliminacion() != null) {
            solicitud.setFechaEliminacion(null);
            solicitud.setFechaRestauracion(LocalDateTime.now());
            solicitud.setFechaActualizacion(LocalDateTime.now());
            solicitudRepository.save(solicitud);
            addHistory(id, solicitud.getEstado(), solicitud.getEstado(), "Request restored.");
        }
        return detail(id);
    }

    // =========================================================
    // LIST DELETED REQUESTS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> deleted() {
        return solicitudRepository
                .findAllByOrderByFechaRegistroDesc()
                .stream()
                .filter(s -> s.getFechaEliminacion() != null)
                .map(this::summary)
                .toList();
    }

    // =========================================================
    // ADD HISTORY ENTRY
    // =========================================================

    private void addHistory(Long id, String oldStatus, String newStatus, String observation) {
        SeguimientoTramite history = new SeguimientoTramite();
        history.setIdSolicitud(id);
        history.setEstadoAnterior(oldStatus);
        history.setEstadoNuevo(newStatus);
        history.setObservacion(blankToNull(observation));
        seguimientoRepository.save(history);
    }

    // =========================================================
    // REQUEST SUMMARY MAP
    // =========================================================

    private Map<String, Object> summary(Solicitud solicitud) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("requestId", solicitud.getIdSolicitud());
        row.put("trackingCode", solicitud.getCodigoSeguimiento());
        row.put("status", solicitud.getEstado());
        row.put("createdAt", solicitud.getFechaRegistro());
        row.put("procedureRequested", solicitud.getSolicito());
        row.put("fullName", solicitud.getNombreCompleto());
        row.put("documentNumber", solicitud.getDni());
        row.put("documentType", solicitud.getTipoDocumento());
        row.put("applicantType", solicitud.getTipoSolicitante());
        row.put("department", solicitud.getDepartamento());
        row.put("deptNumber", solicitud.getNumeroDepto());
        row.put("landlinePhone", solicitud.getTelefonoFijo());
        row.put("folios", solicitud.getFolios());
        row.put("fileName", solicitud.getArchivoNombre());
        row.put("fileType", solicitud.getArchivoTipo());
        row.put("fileSize", solicitud.getArchivoTamano());
        row.put("createdAt",  solicitud.getFechaRegistro());
        row.put("updatedAt",  solicitud.getFechaActualizacion());
        row.put("deletedAt",  solicitud.getFechaEliminacion());
        row.put("restoredAt", solicitud.getFechaRestauracion());
        row.put("deleted", solicitud.getFechaEliminacion() != null);
        return row;
    }

    // =========================================================
    // VALIDATE REQUEST FIELDS
    // =========================================================

    private static void validateRequest(SolicitudRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("The request data is required.");
        }

        require(request.getSolicitante(), "Solicito");
        require(request.getFullName(), "Full name");
        require(request.getDni(), "DNI");
        require(request.getCellphone(), "Mobile phone");
        require(request.getAddress(), "Address");
        require(request.getDistrict(), "District");
        require(request.getProvince(), "Province");
        require(request.getBasis(), "Request basis");

        String documentType = normalizeDocumentType(request.getTipoDocumento());
        String documentNumber = request.getDni().trim();
        boolean validDocument = "CNE".equals(documentType)
                ? documentNumber.matches("[A-Za-z0-9]{9,12}")
                : documentNumber.matches("\\d{8}");
        if (!validDocument) {
            throw new IllegalArgumentException("DNI must contain 8 digits or CNE between 9 and 12 alphanumeric characters.");
        }
        if (request.getFolios() != null && request.getFolios() < 0) {
            throw new IllegalArgumentException("The number of folios cannot be negative.");
        }
        if (request.getArchivoTamano() != null && request.getArchivoTamano() > 4L * 1024 * 1024) {
            throw new IllegalArgumentException("The attached file cannot exceed 4 MB.");
        }
        if (!request.getCellphone().trim().matches("\\d{9}")) {
            throw new IllegalArgumentException("The mobile phone must have 9 digits.");
        }

        String email = blankToNull(request.getEmail());
        if (email != null && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("The email address format is not valid.");
        }
        if (email != null && email.length() > 150) {
            throw new IllegalArgumentException("The email cannot exceed 150 characters.");
        }
    }

    // =========================================================
    // VALIDATE AND PARSE DECLARED DATE
    // =========================================================

    private static LocalDate parseDeclaredDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return LocalDate.now();
        }

        try {
            String value = rawDate.trim();
            if (value.length() < 10) {
                throw new IllegalArgumentException("The declared date must have format YYYY-MM-DD.");
            }

            LocalDate date = LocalDate.parse(value.substring(0, 10));
            if (date.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("The declared date cannot be in the future.");
            }

            return date;
        } catch (DateTimeParseException | IndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("The declared date must have format YYYY-MM-DD.");
        }
    }

    // =========================================================
    // SPLIT LAST NAMES AND FIRST NAMES
    //
    // Expected format: First names PaternalLastName MaternalLastName
    // Example: Viviana Cubas Rufasto -> Viviana | Cubas | Rufasto
    // =========================================================

    private static String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Please enter first names and last names.");
        }

        String[] parts = fullName.trim().split("\\s+");
        if (parts.length < 3) {
            throw new IllegalArgumentException(
                    "Please enter first names, paternal last name, and maternal last name.");
        }

        // The form is entered as: first names, paternal last name, maternal last name.
        // Example: Viviana Cubas Rufasto -> Viviana | Cubas | Rufasto.
        String nombres = String.join(" ", Arrays.copyOfRange(parts, 0, parts.length - 2));
        String apellidoPaterno = parts[parts.length - 2];
        String apellidoMaterno = parts[parts.length - 1];
        return new String[]{nombres, apellidoPaterno, apellidoMaterno};
    }

    // =========================================================
    // GENERATE TRACKING CODE
    // =========================================================

    private static String newTrackingCode() {
        return "FUT-" + LocalDate.now().getYear() + "-"
                + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 10)
                        .toUpperCase(Locale.ROOT);
    }

    // =========================================================
    // APPLY ADDITIONAL FORM FIELDS
    // =========================================================

    private static void applyAdditionalFormFields(Solicitud solicitud, SolicitudRequest request) {
        solicitud.setTipoDocumento(normalizeDocumentType(request.getTipoDocumento()));
        solicitud.setTipoSolicitante(blankToNull(request.getTipoSolicitante()));
        solicitud.setDepartamento(blankToNull(request.getDepartment()));
        solicitud.setNumeroDepto(blankToNull(request.getNumeroDepto()));
        solicitud.setTelefonoFijo(blankToNull(request.getTelefonoFijo()));
        solicitud.setFolios(request.getFolios());
        solicitud.setArchivoNombre(blankToNull(request.getArchivoNombre()));
        solicitud.setArchivoTipo(blankToNull(request.getArchivoTipo()));
        solicitud.setArchivoTamano(request.getArchivoTamano());
    }

    private static String normalizeDocumentType(String raw) {
        return raw != null && raw.trim().equalsIgnoreCase("CNE") ? "CNE" : "DNI";
    }

    private static String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static void require(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("The " + label + " field is required.");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
