package pe.edu.vallegrande.app.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.vallegrande.app.dto.GestionTramiteFormRequest;
import pe.edu.vallegrande.app.dto.GestionTramiteResponse;
import pe.edu.vallegrande.app.model.Area;
import pe.edu.vallegrande.app.model.GestionTramiteFut;
import pe.edu.vallegrande.app.model.Solicitante;
import pe.edu.vallegrande.app.repository.GestionTramiteFutRepository;
import pe.edu.vallegrande.app.repository.SolicitanteRepository;
import pe.edu.vallegrande.app.repository.AreaRepository;
import pe.edu.vallegrande.app.service.GestionTramiteFutService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of GestionTramiteFutService.
 * Handles CRUD and lifecycle operations for FUT procedure management records.
 */
@Service
public class GestionTramiteFutServiceImpl implements GestionTramiteFutService {

    private static final Logger log = LoggerFactory.getLogger(GestionTramiteFutServiceImpl.class);

    // ── Internal enums ────────────────────────────────────────────────────────
    private static final Set<String> MANAGEMENT_TYPES = Set.of(
            "RECEPCION", "EVALUACION", "DERIVACION", "APROBACION", "RECHAZO", "SUBSANACION");
    private static final Set<String> PRIORITIES = Set.of(
            "NORMAL", "URGENTE", "MUY_URGENTE");
    private static final Set<String> MANAGEMENT_STATUSES = Set.of(
            "PENDIENTE", "EN_PROCESO", "OBSERVADO", "APROBADO", "RECHAZADO");

    // ── Text-to-code maps (human-readable → internal code) ───────────────────
    private static final Map<String, String> TEXT_TO_TYPE = Map.of(
            "RECEPCIÓN",   "RECEPCION",
            "EVALUACIÓN",  "EVALUACION",
            "DERIVACIÓN",  "DERIVACION",
            "APROBACIÓN",  "APROBACION",
            "RECHAZO",     "RECHAZO",
            "SUBSANACIÓN", "SUBSANACION"
    );
    private static final Map<String, String> TEXT_TO_PRIORITY = Map.of(
            "NORMAL",       "NORMAL",
            "URGENTE",      "URGENTE",
            "MUY URGENTE",  "MUY_URGENTE"
    );
    private static final Map<String, String> TEXT_TO_STATUS = Map.of(
            "PENDIENTE",   "PENDIENTE",
            "EN PROCESO",  "EN_PROCESO",
            "OBSERVADO",   "OBSERVADO",
            "APROBADO",    "APROBADO",
            "RECHAZADO",   "RECHAZADO"
    );

    private final GestionTramiteFutRepository repository;
    private final SolicitanteRepository solicitanteRepository;
    private final AreaRepository areaRepository;

    public GestionTramiteFutServiceImpl(GestionTramiteFutRepository repository,
                                         SolicitanteRepository solicitanteRepository,
                                         AreaRepository areaRepository) {
        this.repository = repository;
        this.solicitanteRepository = solicitanteRepository;
        this.areaRepository = areaRepository;
    }

    // ── REGISTER ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public GestionTramiteResponse save(GestionTramiteFormRequest request) {
        validate(request);

        // Resolve applicant: numeric ID takes priority, then name
        Long idSolicitante = resolveApplicantId(request);
        Solicitante solicitante = solicitanteRepository.findById(idSolicitante)
                .orElseThrow(() -> new NoSuchElementException(
                        "Applicant not found with ID: " + idSolicitante));

        // If the applicant has empty names (legacy data), try to recover from tramite_fut
        if (solicitante.getNombres() == null || solicitante.getNombres().isBlank()) {
            repairApplicantNames(solicitante);
        }

        // Resolve area: numeric ID takes priority, then name
        Integer idArea = resolveAreaId(request);
        if (idArea != null) {
            areaRepository.findById(idArea)
                    .orElseThrow(() -> new NoSuchElementException(
                            "Area not found with ID: " + idArea));
        }

        GestionTramiteFut g = new GestionTramiteFut();
        applyRequest(g, request, idSolicitante, idArea);
        g.setEstado("A");
        g.setFechaRegistro(LocalDateTime.now());

        log.info("Registering management record — applicant ID: {}", idSolicitante);
        return toResponse(repository.save(g));
    }

    // ── QUERIES ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<GestionTramiteResponse> findActivas(String estadoGestion) {
        if (estadoGestion != null && !estadoGestion.isBlank()) {
            String code = normalizeManagementStatus(estadoGestion.trim());
            return toResponseList(repository.findByEstadoGestionAndEstadoOrderByFechaRegistroDesc(
                    code, "A"));
        }
        return toResponseList(repository.findByEstadoOrderByFechaRegistroDesc("A"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GestionTramiteResponse> findEliminadas() {
        return toResponseList(repository.findByEstadoOrderByFechaRegistroDesc("I"));
    }

    @Override
    @Transactional(readOnly = true)
    public GestionTramiteResponse findById(Integer id) {
        return toResponse(repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Management record not found with ID: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GestionTramiteResponse> findBySolicitante(Long idSolicitante) {
        solicitanteRepository.findById(idSolicitante)
                .orElseThrow(() -> new NoSuchElementException(
                        "Applicant not found with ID: " + idSolicitante));
        return toResponseList(repository.findByIdSolicitanteAndEstadoOrderByFechaRegistroDesc(
                idSolicitante, "A"));
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public GestionTramiteResponse update(Integer id, GestionTramiteFormRequest request) {
        GestionTramiteFut g = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Management record not found with ID: " + id));
        if ("I".equalsIgnoreCase(g.getEstado())) {
            // Automatically restore before editing
            g.setEstado("A");
            g.setFechaRestauracion(LocalDateTime.now());
            g.setFechaActualizacion(LocalDateTime.now());
            g.setFechaEliminacion(null);
            repository.save(g);
            log.info("Management record with ID {} automatically restored for editing.", id);
        }
        validate(request);

        Long idSolicitante = resolveApplicantId(request);
        solicitanteRepository.findById(idSolicitante)
                .orElseThrow(() -> new NoSuchElementException(
                        "Applicant not found with ID: " + idSolicitante));

        Integer idArea = resolveAreaId(request);
        if (idArea != null) {
            areaRepository.findById(idArea)
                    .orElseThrow(() -> new NoSuchElementException(
                            "Area not found with ID: " + idArea));
        }
        applyRequest(g, request, idSolicitante, idArea);
        g.setFechaActualizacion(LocalDateTime.now());
        log.info("Updating management record ID: {}", id);
        return toResponse(repository.save(g));
    }

    // ── LOGICAL DELETION ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public GestionTramiteResponse delete(Integer id) {
        GestionTramiteFut g = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Management record not found with ID: " + id));
        if ("I".equalsIgnoreCase(g.getEstado())) return toResponse(g);
        g.setEstado("I");
        g.setFechaEliminacion(LocalDateTime.now());
        g.setFechaActualizacion(LocalDateTime.now());
        log.info("Logically deleting management record ID: {}", id);
        return toResponse(repository.save(g));
    }

    // ── RESTORE ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public GestionTramiteResponse restore(Integer id) {
        GestionTramiteFut g = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Management record not found with ID: " + id));
        if ("A".equalsIgnoreCase(g.getEstado())) return toResponse(g);
        g.setEstado("A");
        g.setFechaRestauracion(LocalDateTime.now());
        g.setFechaActualizacion(LocalDateTime.now());
        log.info("Restoring management record ID: {}", id);
        return toResponse(repository.save(g));
    }

    // ── ID RESOLUTION ─────────────────────────────────────────────────────────

    /**
     * Resolves idSolicitante from the DTO.
     * Priority: 1) numeric idSolicitante, 2) name in solicitante field.
     */
    private Long resolveApplicantId(GestionTramiteFormRequest req) {
        // Native format (frontend): ID provided directly
        if (req.getIdSolicitante() != null && req.getIdSolicitante() > 0) {
            return req.getIdSolicitante();
        }
        // Human-readable format (Swagger/manual testing): full name provided
        if (req.getSolicitante() != null && !req.getSolicitante().isBlank()) {
            String nombre = req.getSolicitante().trim();
            List<Solicitante> candidates = solicitanteRepository.findAll();
            for (Solicitante s : candidates) {
                String fullName = Stream.of(s.getNombres(), s.getApellidoPaterno(), s.getApellidoMaterno())
                        .filter(v -> v != null && !v.isBlank())
                        .collect(Collectors.joining(" "));
                if (fullName.equalsIgnoreCase(nombre)) {
                    return s.getIdSolicitante();
                }
            }
            // Try partial match by document number
            for (Solicitante s : candidates) {
                if (nombre.equals(s.getNumeroDocumento())) {
                    return s.getIdSolicitante();
                }
            }
            throw new IllegalArgumentException(
                    "No applicant found with the name or document: '" + nombre + "'. " +
                    "Check /v1/api/applicant to verify exact names.");
        }
        throw new IllegalArgumentException(
                "You must specify the applicant: 'solicitante' (full name) or 'idSolicitante' (number).");
    }

    /**
     * Resolves idAreaResponsable from the DTO.
     * Priority: 1) numeric idAreaResponsable, 2) name in areaResponsable field.
     */
    private Integer resolveAreaId(GestionTramiteFormRequest req) {
        // Native format (frontend): ID provided directly
        if (req.getIdAreaResponsable() != null && req.getIdAreaResponsable() > 0) {
            return req.getIdAreaResponsable();
        }
        // Human-readable format (Swagger/manual testing): area name provided
        if (req.getAreaResponsable() != null && !req.getAreaResponsable().isBlank()) {
            String name = req.getAreaResponsable().trim().toUpperCase();
            return areaRepository.findAll().stream()
                    .filter(a -> a.getNombre() != null &&
                            a.getNombre().trim().toUpperCase().equals(name))
                    .map(Area::getIdArea)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No area found with the name: '" + req.getAreaResponsable() + "'. " +
                            "See /v1/api/management/catalogo/areas for available areas."));
        }
        throw new IllegalArgumentException(
                "You must specify the responsible area: 'areaResponsable' (name) or 'idAreaResponsable' (number). " +
                "See /v1/api/management/catalogo/areas for the area list.");
    }

    // ── LEGACY DATA REPAIR ────────────────────────────────────────────────────

    /**
     * If the applicant has empty names (legacy data), attempts to recover them
     * from tramite_fut.nombre_completo and persists them so they appear correctly
     * in all future responses.
     * Split: "FirstNames PaternalLastName MaternalLastName" (same as splitName in SolicitudServiceImpl).
     */
    private void repairApplicantNames(Solicitante s) {
        try {
            solicitanteRepository.findNombreCompletoFromTramite(s.getIdSolicitante())
                    .filter(n -> n != null && !n.isBlank())
                    .ifPresent(fullName -> {
                        String[] parts = fullName.trim().split("\\s+");
                        if (parts.length < 2) return;
                        String maternal = parts[parts.length - 1];
                        String paternal = parts[parts.length - 2];
                        String names = parts.length > 2
                                ? String.join(" ", java.util.Arrays.copyOfRange(parts, 0, parts.length - 2))
                                : parts[0];
                        s.setNombres(names);
                        if (s.getApellidoPaterno() == null || s.getApellidoPaterno().isBlank())
                            s.setApellidoPaterno(paternal);
                        if (s.getApellidoMaterno() == null || s.getApellidoMaterno().isBlank())
                            s.setApellidoMaterno(maternal);
                        solicitanteRepository.save(s);
                        log.info("Names recovered for applicant ID {}: {}", s.getIdSolicitante(), fullName);
                    });
        } catch (Exception e) {
            log.warn("Could not repair names for applicant ID {}: {}", s.getIdSolicitante(), e.getMessage());
        }
    }

    // ── ENTITY → RESPONSE DTO CONVERSION ────────────────────────────────────

    private List<GestionTramiteResponse> toResponseList(List<GestionTramiteFut> records) {
        if (records.isEmpty()) return List.of();

        List<Long> applicantIds = records.stream()
                .map(GestionTramiteFut::getIdSolicitante)
                .distinct().toList();
        Map<Long, Solicitante> applicants = solicitanteRepository
                .findAllById(applicantIds)
                .stream()
                .collect(Collectors.toMap(Solicitante::getIdSolicitante, Function.identity()));

        Map<Integer, Area> areas = areaRepository.findAllById(
                        records.stream().map(GestionTramiteFut::getIdAreaResponsable)
                                .filter(id -> id != null).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Area::getIdArea, Function.identity()));

        // Row correlation counter for the "numero" field
        int[] counter = {1};
        return records.stream()
                .map(g -> buildResponse(g,
                        applicants.get(g.getIdSolicitante()),
                        areas.get(g.getIdAreaResponsable()),
                        counter[0]++))
                .toList();
    }

    private GestionTramiteResponse toResponse(GestionTramiteFut record) {
        Solicitante solicitante = solicitanteRepository.findById(record.getIdSolicitante())
                .orElse(null);
        Area area = record.getIdAreaResponsable() == null ? null
                : areaRepository.findById(record.getIdAreaResponsable()).orElse(null);
        return buildResponse(record, solicitante, area, 1);
    }

    private GestionTramiteResponse buildResponse(GestionTramiteFut g,
                                                  Solicitante solicitante,
                                                  Area area,
                                                  int numero) {
        if (solicitante == null) {
            throw new NoSuchElementException(
                    "Applicant associated with management record ID " + g.getIdGestion() + " not found.");
        }
        if (g.getIdAreaResponsable() != null && area == null) {
            throw new NoSuchElementException(
                    "Area associated with management record ID " + g.getIdGestion() + " not found.");
        }

        // Full name of responsible person: use compound field if partial fields are empty
        String responsibleName = Stream.of(
                g.getNombresResponsable(),
                g.getApellidoPaternoResponsable(),
                g.getApellidoMaternoResponsable())
            .filter(v -> v != null && !v.isBlank())
            .collect(Collectors.joining(" "));
        // Fallback to nombre_responsable if partial fields are empty (legacy data)
        if (responsibleName.isBlank()) {
            responsibleName = g.getNombreResponsable() != null ? g.getNombreResponsable() : null;
        }
        String finalResponsible = responsibleName == null || responsibleName.isBlank()
                ? null : responsibleName;

        return new GestionTramiteResponse(
                g.getIdGestion(),
                g.getIdSolicitante(),
                solicitante.getNumeroDocumento(),
                solicitante.getApellidoPaterno(),
                solicitante.getApellidoMaterno(),
                solicitante.getNombres(),
                finalResponsible,
                g.getNombresResponsable(),
                g.getApellidoPaternoResponsable(),
                g.getApellidoMaternoResponsable(),
                g.getIdAreaResponsable(),
                area != null ? area.getNombre() : null,
                g.getTipoGestionTexto(),
                g.getPrioridadTexto(),
                g.getEstadoGestion(),
                "A".equalsIgnoreCase(g.getEstado()) ? "ACTIVO" : "INACTIVO",
                g.getFechaRecepcion(),
                g.getFechaLimite(),
                g.getFechaResolucion(),
                g.getDecision(),
                g.getFundamentoDecision(),
                g.getObservaciones(),
                g.getFechaRegistro(),
                g.getFechaActualizacion(),
                g.getFechaEliminacion(),
                g.getFechaRestauracion()
        );
    }

    // ── ENUM NORMALIZATION ────────────────────────────────────────────────────

    /**
     * Accepts both internal code (APROBACION) and human-readable text (Aprobación).
     * Always returns the internal code in uppercase.
     */
    private static String normalizeManagementType(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String upper = raw.trim().toUpperCase();
        if (MANAGEMENT_TYPES.contains(upper)) return upper;
        String noAccents = removeAccents(upper);
        if (TEXT_TO_TYPE.containsKey(noAccents)) return TEXT_TO_TYPE.get(noAccents);
        throw new IllegalArgumentException(
                "Invalid tipoGestion: '" + raw + "'. Allowed values: " +
                "Recepción, Evaluación, Derivación, Aprobación, Rechazo, Subsanación " +
                "(or codes: RECEPCION, EVALUACION, DERIVACION, APROBACION, RECHAZO, SUBSANACION).");
    }

    private static String normalizePriority(String raw) {
        if (raw == null || raw.isBlank()) return "NORMAL";
        String upper = raw.trim().toUpperCase();
        if (PRIORITIES.contains(upper)) return upper;
        String noAccents = removeAccents(upper);
        if (TEXT_TO_PRIORITY.containsKey(noAccents)) return TEXT_TO_PRIORITY.get(noAccents);
        throw new IllegalArgumentException(
                "Invalid priority: '" + raw + "'. Allowed values: " +
                "Normal, Urgente, Muy urgente (or: NORMAL, URGENTE, MUY_URGENTE).");
    }

    private static String normalizeManagementStatus(String raw) {
        if (raw == null || raw.isBlank()) return "PENDIENTE";
        String upper = raw.trim().toUpperCase();
        if (MANAGEMENT_STATUSES.contains(upper)) return upper;
        String noAccents = removeAccents(upper);
        if (TEXT_TO_STATUS.containsKey(noAccents)) return TEXT_TO_STATUS.get(noAccents);
        throw new IllegalArgumentException(
                "Invalid estadoGestion: '" + raw + "'. Allowed values: " +
                "Pendiente, En proceso, Observado, Aprobado, Rechazado " +
                "(or: PENDIENTE, EN_PROCESO, OBSERVADO, APROBADO, RECHAZADO).");
    }

    /** Removes accents for tolerant comparison (Á→A, É→E, etc.) */
    private static String removeAccents(String s) {
        return s.replace("Á", "A").replace("É", "E").replace("Í", "I")
                .replace("Ó", "O").replace("Ú", "U").replace("Ñ", "N");
    }

    // ── VALIDATIONS ──────────────────────────────────────────────────────────

    private void validate(GestionTramiteFormRequest req) {
        if (req == null)
            throw new IllegalArgumentException("The management data is required.");
        // Applicant: at least one identifier must be provided
        boolean hasApplicant = (req.getIdSolicitante() != null && req.getIdSolicitante() > 0)
                || (req.getSolicitante() != null && !req.getSolicitante().isBlank());
        if (!hasApplicant)
            throw new IllegalArgumentException(
                    "You must specify the applicant: 'solicitante' (full name) or 'idSolicitante' (number).");
        // Area: at least one identifier must be provided
        boolean hasArea = (req.getIdAreaResponsable() != null && req.getIdAreaResponsable() > 0)
                || (req.getAreaResponsable() != null && !req.getAreaResponsable().isBlank());
        if (!hasArea)
            throw new IllegalArgumentException(
                    "You must specify the responsible area: 'areaResponsable' (name) or 'idAreaResponsable' (number).");
        if (req.getNombresResponsable() == null || req.getNombresResponsable().isBlank())
            throw new IllegalArgumentException("The responsible person's first names are required.");
        if (req.getApellidoPaternoResponsable() == null || req.getApellidoPaternoResponsable().isBlank())
            throw new IllegalArgumentException("The paternal last name is required.");
        if (req.getApellidoMaternoResponsable() == null || req.getApellidoMaternoResponsable().isBlank())
            throw new IllegalArgumentException("The maternal last name is required.");
        if (req.getFechaRecepcion() == null || req.getFechaRecepcion().isBlank())
            throw new IllegalArgumentException("The reception date is required.");
        if (req.getFechaLimite() == null || req.getFechaLimite().isBlank())
            throw new IllegalArgumentException("The deadline date is required.");
        if (req.getTipoGestion() == null || req.getTipoGestion().isBlank())
            throw new IllegalArgumentException("The tipoGestion field is required.");

        // fechaResolucion, decision and fundamentoDecision are optional when
        // status is Pending or In Progress (no resolution yet)
        String statusNorm = req.getEstadoGestion() != null
                ? removeAccents(req.getEstadoGestion().trim().toUpperCase()) : "";
        boolean pendingOrInProgress = statusNorm.isEmpty()
                || statusNorm.equals("PENDIENTE") || statusNorm.equals("EN PROCESO")
                || statusNorm.equals("EN_PROCESO");

        if (!pendingOrInProgress) {
            if (req.getFechaResolucion() == null || req.getFechaResolucion().isBlank())
                throw new IllegalArgumentException("The resolution date is required.");
            if (req.getDecision() == null || req.getDecision().isBlank())
                throw new IllegalArgumentException("The decision is required.");
            if (req.getFundamentoDecision() == null || req.getFundamentoDecision().isBlank())
                throw new IllegalArgumentException("The decision basis is required.");
        }
        if (req.getObservaciones() == null || req.getObservaciones().isBlank())
            throw new IllegalArgumentException("Observations are required.");
        validateResponsibleName(req.getNombresResponsable(), "First names");
        validateResponsibleName(req.getApellidoPaternoResponsable(), "Paternal last name");
        validateResponsibleName(req.getApellidoMaternoResponsable(), "Maternal last name");
        // Validate enum values by attempting normalization (throws if not recognized)
        normalizeManagementType(req.getTipoGestion());
        normalizePriority(req.getPrioridad());
        normalizeManagementStatus(req.getEstadoGestion());
    }

    private void applyRequest(GestionTramiteFut g, GestionTramiteFormRequest req,
                               Long idSolicitante, Integer idArea) {
        g.setIdSolicitante(idSolicitante);
        String names = blank(req.getNombresResponsable());
        String paternal = blank(req.getApellidoPaternoResponsable());
        String maternal = blank(req.getApellidoMaternoResponsable());
        g.setNombresResponsable(names);
        g.setApellidoPaternoResponsable(paternal);
        g.setApellidoMaternoResponsable(maternal);
        String fullName = Stream.of(names, paternal, maternal)
                .filter(v -> v != null && !v.isBlank())
                .collect(Collectors.joining(" "));
        g.setNombreResponsable(fullName.isEmpty() ? null
                : fullName.length() <= 150 ? fullName : null);
        g.setIdAreaResponsable(idArea);
        g.setFechaRecepcion(parseDate(req.getFechaRecepcion(), LocalDate.now()));
        g.setTipoGestion(normalizeManagementType(req.getTipoGestion()));
        g.setPrioridad(normalizePriority(req.getPrioridad()));
        g.setFechaLimite(parseDate(req.getFechaLimite(), null));
        g.setEstadoGestion(normalizeManagementStatus(req.getEstadoGestion()));
        g.setDecision(blank(req.getDecision()));
        g.setFundamentoDecision(blank(req.getFundamentoDecision()));
        g.setFechaResolucion(parseDate(req.getFechaResolucion(), null));
        g.setObservaciones(blank(req.getObservaciones()));
    }

    private static void validateResponsibleName(String value, String field) {
        if (value != null && !value.isBlank()
                && !value.trim().matches("^[A-Za-zÁÉÍÓÚáéíóúÑñ]+( [A-Za-zÁÉÍÓÚáéíóúÑñ]+)*$")) {
            throw new IllegalArgumentException(
                    field + " can only contain letters, accented vowels, Ñ, and spaces.");
        }
    }

    private static LocalDate parseDate(String raw, LocalDate fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try { return LocalDate.parse(raw.trim().substring(0, 10)); }
        catch (Exception e) { return fallback; }
    }

    private static String blank(String v) {
        if (v == null) return null;
        String trimmed = v.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
