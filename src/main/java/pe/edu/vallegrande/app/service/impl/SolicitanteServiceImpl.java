package pe.edu.vallegrande.app.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.vallegrande.app.dto.SolicitanteRequest;
import pe.edu.vallegrande.app.dto.SolicitanteResponse;
import pe.edu.vallegrande.app.model.Solicitante;
import pe.edu.vallegrande.app.repository.SolicitanteRepository;
import pe.edu.vallegrande.app.repository.SolicitanteResponseProjection;
import pe.edu.vallegrande.app.service.SolicitanteService;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Implementation of the SolicitanteService.
 * Handles CRUD operations and catalog queries for applicants.
 */
@Service
public class SolicitanteServiceImpl implements SolicitanteService {

    private static final Logger log = LoggerFactory.getLogger(SolicitanteServiceImpl.class);
    private final SolicitanteRepository solicitanteRepository;

    public SolicitanteServiceImpl(SolicitanteRepository solicitanteRepository) {
        this.solicitanteRepository = solicitanteRepository;
    }

    // ----------------------------------------------------------------
    // LIST ALL APPLICANTS (active and inactive)
    // ----------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public List<SolicitanteResponse> findAll() {
        log.info("Listing all applicants");
        return solicitanteRepository.findAllResponses().stream()
                .map(this::toResponse)
                .toList();
    }

    // ----------------------------------------------------------------
    // LIST BY STATUS
    // A = Active, I = Inactive, T = All
    // ----------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public List<SolicitanteResponse> findByEstado(String estado) {
        log.info("Listing applicants by status: {}", estado);
        if (estado == null || estado.isBlank() || "T".equalsIgnoreCase(estado.trim())) {
            return findAll();
        }
        String normalizedStatus = estado.trim().toUpperCase();
        if (!normalizedStatus.equals("A") && !normalizedStatus.equals("I")) {
            throw new IllegalArgumentException(
                    "Invalid status '" + estado + "'. Allowed values: A, I, T.");
        }
        return solicitanteRepository.findResponsesByEstado(normalizedStatus).stream()
                .map(this::toResponse)
                .toList();
    }

    // ----------------------------------------------------------------
    // FIND BY ID
    // ----------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public Optional<SolicitanteResponse> findById(Long id) {
        log.info("Looking up applicant with ID: {}", id);
        return solicitanteRepository.findResponseById(id).map(this::toResponse);
    }

    // ----------------------------------------------------------------
    // REGISTER APPLICANT
    // ----------------------------------------------------------------
    @Override
    @Transactional
    public SolicitanteResponse save(Solicitante solicitante) {
        if (solicitante == null) {
            throw new IllegalArgumentException("The applicant data is required.");
        }

        // Swagger/Angular may send 0 — JPA needs null to generate IDENTITY.
        if (solicitante.getIdSolicitante() != null && solicitante.getIdSolicitante() <= 0L) {
            solicitante.setIdSolicitante(null);
        }

        // Required fields validation
        validateRequiredFields(solicitante);

        // Verify the document type exists
        if (solicitante.getIdTipoDocumento() == null) {
            throw new IllegalArgumentException("The idTipoDocumento field is required.");
        }
        // idUbigeo is no longer mandatory for the user — use 1 (fallback ubigeo)
        // when the user enters a free-text location. Just ensure it is not null.
        if (solicitante.getIdUbigeo() == null) {
            solicitante.setIdUbigeo(1); // fallback ubigeo always exists
        }

        // Unique document number
        solicitanteRepository.findByNumeroDocumento(solicitante.getNumeroDocumento().trim())
                .ifPresent(s -> {
                    if (!s.getIdSolicitante().equals(solicitante.getIdSolicitante())) {
                        throw new pe.edu.vallegrande.app.service.exception.DuplicateResourceException(
                                "An applicant with document number '"
                                + solicitante.getNumeroDocumento() + "' already exists.");
                    }
                });

        solicitante.setNumeroDocumento(solicitante.getNumeroDocumento().trim());
        solicitante.setEstado("A");
        solicitante.setFechaRegistro(LocalDateTime.now());
        solicitante.setFechaActualizacion(null);
        solicitante.setFechaEliminacion(null);
        solicitante.setFechaRestauracion(null);

        log.info("Registering new applicant: {}", solicitante.getNumeroDocumento());
        Solicitante saved = solicitanteRepository.save(solicitante);
        return findResponse(saved.getIdSolicitante());
    }

    // ----------------------------------------------------------------
    // UPDATE APPLICANT
    // ----------------------------------------------------------------
    @Override
    @Transactional
    public SolicitanteResponse update(Solicitante solicitante) {
        if (solicitante == null || solicitante.getIdSolicitante() == null) {
            throw new IllegalArgumentException("The applicant ID and data are required.");
        }

        Long id = solicitante.getIdSolicitante();
        log.info("Updating applicant with ID: {}", id);

        Solicitante existing = solicitanteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Applicant not found with ID: " + id));

        if ("I".equalsIgnoreCase(existing.getEstado()) || existing.getFechaEliminacion() != null) {
            // Automatically restore before editing
            existing.setEstado("A");
            existing.setFechaRestauracion(LocalDateTime.now());
            existing.setFechaActualizacion(LocalDateTime.now());
            existing.setFechaEliminacion(null);
            solicitanteRepository.save(existing);
            log.info("Applicant with ID {} automatically restored for editing.", id);
        }

        validateRequiredFields(solicitante);

        // Check for duplicate document number in another record
        if (solicitante.getNumeroDocumento() != null) {
            solicitanteRepository.findByNumeroDocumento(solicitante.getNumeroDocumento().trim())
                    .ifPresent(s -> {
                        if (!s.getIdSolicitante().equals(id)) {
                            throw new pe.edu.vallegrande.app.service.exception.DuplicateResourceException(
                                    "Another applicant with document number '"
                                    + solicitante.getNumeroDocumento() + "' already exists.");
                        }
                    });
            solicitante.setNumeroDocumento(solicitante.getNumeroDocumento().trim());
        }

        // Preserve original audit data
        solicitante.setFechaRegistro(existing.getFechaRegistro());
        solicitante.setFechaActualizacion(LocalDateTime.now());
        solicitante.setFechaEliminacion(existing.getFechaEliminacion());
        solicitante.setFechaRestauracion(existing.getFechaRestauracion());
        solicitante.setEstado("A");

        solicitanteRepository.save(solicitante);
        return findResponse(id);
    }

    // ----------------------------------------------------------------
    // LOGICAL DELETION
    // Status: A -> I, records fecha_eliminacion
    // ----------------------------------------------------------------
    @Override
    @Transactional
    public SolicitanteResponse delete(Long id) {
        log.info("Logical deletion of applicant with ID: {}", id);

        Solicitante solicitante = solicitanteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Applicant not found with ID: " + id));

        if ("I".equalsIgnoreCase(solicitante.getEstado())) {
            log.info("Applicant with ID {} is already inactive.", id);
            return findResponse(id); // idempotent: already deleted
        }

        solicitante.setEstado("I");
        solicitante.setFechaEliminacion(LocalDateTime.now());
        solicitante.setFechaActualizacion(LocalDateTime.now());

        solicitanteRepository.save(solicitante);
        log.info("Applicant with ID {} marked as inactive.", id);
        return findResponse(id);
    }

    // ----------------------------------------------------------------
    // RESTORE APPLICANT
    // Status: I -> A, records fecha_restauracion
    // ----------------------------------------------------------------
    @Override
    @Transactional
    public SolicitanteResponse restore(Long id) {
        log.info("Restoring applicant with ID: {}", id);

        Solicitante solicitante = solicitanteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Applicant not found with ID: " + id));

        if ("A".equalsIgnoreCase(solicitante.getEstado())) {
            log.info("Applicant with ID {} is already active.", id);
            return findResponse(id); // idempotent: already active
        }

        solicitante.setEstado("A");
        solicitante.setFechaRestauracion(LocalDateTime.now());
        solicitante.setFechaActualizacion(LocalDateTime.now());

        solicitanteRepository.save(solicitante);
        log.info("Applicant with ID {} restored.", id);
        return findResponse(id);
    }

    // ----------------------------------------------------------------
    // INTERNAL VALIDATIONS
    // ----------------------------------------------------------------
    private void validateRequiredFields(Solicitante s) {
        if (s.getNumeroDocumento() == null || s.getNumeroDocumento().isBlank())
            throw new IllegalArgumentException("The document number is required.");
        if (s.getNumeroDocumento().trim().length() > 20)
            throw new IllegalArgumentException("The document number cannot exceed 20 characters.");

        // Validation by document type
        String docNumber = s.getNumeroDocumento().trim();
        if (s.getIdTipoDocumento() != null) {
            // idTipoDocumento 1 = DNI → exactly 8 numeric digits
            if (s.getIdTipoDocumento() == 1) {
                if (!docNumber.matches("^\\d{8}$"))
                    throw new IllegalArgumentException("The DNI must contain exactly 8 numeric digits.");
            }
            // idTipoDocumento 2 = CNE → up to 20 characters (already validated above)
        }
        if (s.getNombres() == null || s.getNombres().isBlank())
            throw new IllegalArgumentException("The first names field is required.");
        if (s.getApellidoPaterno() == null || s.getApellidoPaterno().isBlank())
            throw new IllegalArgumentException("The paternal last name is required.");
        if (s.getApellidoMaterno() == null || s.getApellidoMaterno().isBlank())
            throw new IllegalArgumentException("The maternal last name is required.");
        if (s.getTelefono() != null && !s.getTelefono().matches("^\\d{9}$"))
            throw new IllegalArgumentException("The phone number must contain exactly 9 numeric digits.");
        if (s.getCorreo() != null && s.getCorreo().length() > 150)
            throw new IllegalArgumentException("The email cannot exceed 150 characters.");
        if (s.getDireccion() != null && s.getDireccion().length() > 250)
            throw new IllegalArgumentException("The address cannot exceed 250 characters.");
    }

    // ----------------------------------------------------------------
    // FIND BY DOCUMENT NUMBER
    // Returns the applicant regardless of status (active or inactive),
    // so the frontend can differentiate and show the appropriate message.
    // ----------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public Optional<Solicitante> findInternalByNumeroDocumento(String numeroDocumento) {
        if (numeroDocumento == null || numeroDocumento.isBlank()) {
            return Optional.empty();
        }
        return solicitanteRepository.findByNumeroDocumento(numeroDocumento.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SolicitanteResponse> findByNumeroDocumento(String numeroDocumento) {
        if (numeroDocumento == null || numeroDocumento.isBlank()) {
            return Optional.empty();
        }
        return solicitanteRepository.findResponseByNumeroDocumento(numeroDocumento.trim())
                .map(this::toResponse);
    }

    private SolicitanteResponse findResponse(Long id) {
        return solicitanteRepository.findResponseById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NoSuchElementException(
                        "Applicant not found with ID: " + id));
    }

    private SolicitanteResponse toResponse(SolicitanteResponseProjection row) {
        return new SolicitanteResponse(
                row.getIdSolicitante(),
                row.getTipoDocumento(),
                row.getNumeroDocumento(),
                row.getApellidoPaterno(),
                row.getApellidoMaterno(),
                row.getNombres(),
                row.getDistrito(),
                row.getProvincia(),
                row.getDepartamento(),
                row.getDireccion(),
                row.getTelefono(),
                row.getCorreo(),
                row.getEstadoTexto(),     // "Activo" or "Inactivo"
                row.getFechaRegistro(),
                row.getFechaActualizacion(),
                row.getFechaEliminacion(),
                row.getFechaRestauracion()
        );
    }

    // ----------------------------------------------------------------
    // REGISTER FROM PRESENTATION DTO (POST with tipoDocumento and ubicacion)
    // ----------------------------------------------------------------
    @Override
    @Transactional
    public SolicitanteResponse saveFromRequest(SolicitanteRequest req) {
        Solicitante s = new Solicitante();
        resolveAndMap(req, s);
        return save(s);
    }

    // ----------------------------------------------------------------
    // UPDATE FROM PRESENTATION DTO (PUT with tipoDocumento and ubicacion)
    // ----------------------------------------------------------------
    @Override
    @Transactional
    public SolicitanteResponse updateFromRequest(Long id, SolicitanteRequest req) {
        Solicitante s = new Solicitante();
        s.setIdSolicitante(id);
        resolveAndMap(req, s);
        return update(s);
    }

    /**
     * Converts input DTO fields into the entity's internal fields.
     *
     * Format A (native — Angular frontend):
     *   idTipoDocumento (Integer) → used directly.
     *   idUbigeo (Integer) → used directly; departamento/provincia/distrito are stored if provided.
     *
     * Format B (presentation — Swagger / manual testing):
     *   tipoDocumento (text "DNI") → looks up idTipoDocumento in dbo.tipo_documento.
     *   ubicacion ("District, Province, Department") → parses and looks up idUbigeo.
     *   Or departamento/provincia/distrito separately → looks up idUbigeo.
     */
    private void resolveAndMap(SolicitanteRequest req, Solicitante s) {
        // ── Document type ──────────────────────────────────────────────────────
        if (req.getIdTipoDocumento() != null && req.getIdTipoDocumento() > 0) {
            // Native format: ID provided directly
            s.setIdTipoDocumento(req.getIdTipoDocumento());
        } else if (req.getTipoDocumento() != null && !req.getTipoDocumento().isBlank()) {
            // Presentation format: text provided, look up the ID
            Integer idTipoDoc = solicitanteRepository
                    .findTipoDocumentoIdByCodigo(req.getTipoDocumento().trim())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Document type not recognized: '" + req.getTipoDocumento()
                            + "'. See /v1/api/applicant/tipos-documento."));
            s.setIdTipoDocumento(idTipoDoc);
        } else {
            throw new IllegalArgumentException(
                    "You must specify the document type: 'idTipoDocumento' (number) or 'tipoDocumento' (text, e.g. DNI).");
        }

        // ── Location → departamento / provincia / distrito / idUbigeo ─────────
        String deptFinal = "";
        String provFinal = "";
        String distFinal = "";

        if (req.getIdUbigeo() != null && req.getIdUbigeo() > 0) {
            // Native format: ID provided directly
            s.setIdUbigeo(req.getIdUbigeo());
            deptFinal = req.getDepartamento() != null ? req.getDepartamento().trim() : "";
            provFinal = req.getProvincia()    != null ? req.getProvincia().trim()    : "";
            distFinal = req.getDistrito()     != null ? req.getDistrito().trim()     : "";
        } else {
            // Presentation format: departamento/provincia/distrito or ubicacion string
            if (req.getDepartamento() != null && !req.getDepartamento().isBlank()) {
                deptFinal = req.getDepartamento().trim();
                provFinal = req.getProvincia()  != null ? req.getProvincia().trim()  : "";
                distFinal = req.getDistrito()   != null ? req.getDistrito().trim()   : "";
            } else if (req.getUbicacion() != null && !req.getUbicacion().isBlank()) {
                // "District, Province, Department"
                String[] parts = req.getUbicacion().split(",");
                if (parts.length >= 3) {
                    distFinal = parts[0].trim();
                    provFinal = parts[1].trim();
                    deptFinal = parts[2].trim();
                } else if (parts.length == 2) {
                    provFinal = parts[0].trim();
                    deptFinal = parts[1].trim();
                } else {
                    distFinal = req.getUbicacion().trim();
                }
            }
            // Look up idUbigeo; use 1 as fallback if no exact match found
            Integer idUbigeo = 1;
            if (!deptFinal.isEmpty() && !provFinal.isEmpty() && !distFinal.isEmpty()) {
                idUbigeo = solicitanteRepository
                        .findUbigeoId(deptFinal, provFinal, distFinal)
                        .orElse(1);
            }
            s.setIdUbigeo(idUbigeo);
        }

        s.setDepartamento(deptFinal.isEmpty() ? null : deptFinal);
        s.setProvincia(provFinal.isEmpty()    ? null : provFinal);
        s.setDistrito(distFinal.isEmpty()     ? null : distFinal);

        // ── Basic validations ──────────────────────────────────────────────────
        if (req.getNumeroDocumento() == null || req.getNumeroDocumento().isBlank())
            throw new IllegalArgumentException("The document number is required.");

        // Validation by document type
        String docNum = req.getNumeroDocumento().trim();
        String docType = req.getTipoDocumento() != null ? req.getTipoDocumento().trim().toUpperCase() : "";
        if ("DNI".equals(docType) || (req.getIdTipoDocumento() != null && req.getIdTipoDocumento() == 1)) {
            if (!docNum.matches("^\\d{8}$"))
                throw new IllegalArgumentException("The DNI must contain exactly 8 numeric digits.");
        }
        if (req.getNombres() == null || req.getNombres().isBlank())
            throw new IllegalArgumentException("The first names field is required.");
        if (req.getApellidoPaterno() == null || req.getApellidoPaterno().isBlank())
            throw new IllegalArgumentException("The paternal last name is required.");
        if (req.getApellidoMaterno() == null || req.getApellidoMaterno().isBlank())
            throw new IllegalArgumentException("The maternal last name is required.");
        if (req.getTelefono() != null && !req.getTelefono().isBlank()
                && !req.getTelefono().trim().matches("^\\d{9}$"))
            throw new IllegalArgumentException("The phone number must contain exactly 9 numeric digits.");

        // ── Direct fields ──────────────────────────────────────────────────────
        s.setNumeroDocumento(req.getNumeroDocumento().trim());
        s.setNombres(req.getNombres().trim());
        s.setApellidoPaterno(req.getApellidoPaterno().trim());
        s.setApellidoMaterno(req.getApellidoMaterno().trim());
        s.setTelefono(req.getTelefono() != null && !req.getTelefono().isBlank()
                ? req.getTelefono().trim() : null);
        s.setCorreo(req.getCorreo() != null && !req.getCorreo().isBlank()
                ? req.getCorreo().trim() : null);
        s.setDireccion(req.getDireccion() != null && !req.getDireccion().isBlank()
                ? req.getDireccion().trim() : null);
        // status and dates are assigned internally by save()/update()
    }

    // ----------------------------------------------------------------
    // CATALOG: DOCUMENT TYPES
    // [{id, code, name}]
    // ----------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listTiposDocumento() {
        return solicitanteRepository.findAllTiposDocumento().stream()
                .map(row -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", row[0]);
                    m.put("code", row[1]);
                    m.put("name", row[2]);
                    return m;
                })
                .toList();
    }

    // ----------------------------------------------------------------
    // CATALOG: UBIGEOS
    // [{id, department, province, district, status}]
    // ----------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listUbigeos() {
        return solicitanteRepository.findAllUbigeos().stream()
                .map(row -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", row[0]);
                    m.put("department", row[1]);
                    m.put("province", row[2]);
                    m.put("district", row[3]);
                    m.put("status", row[4]);
                    return m;
                })
                .toList();
    }
}
