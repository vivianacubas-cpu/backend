package pe.edu.vallegrande.app.service;

import pe.edu.vallegrande.app.dto.SolicitanteRequest;
import pe.edu.vallegrande.app.dto.SolicitanteResponse;
import pe.edu.vallegrande.app.model.Solicitante;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for applicant management.
 */
public interface SolicitanteService {

    List<SolicitanteResponse> findAll();
    List<SolicitanteResponse> findByEstado(String estado);
    Optional<SolicitanteResponse> findById(Long id);
    SolicitanteResponse save(Solicitante solicitante);
    SolicitanteResponse update(Solicitante solicitante);
    SolicitanteResponse delete(Long id);
    SolicitanteResponse restore(Long id);

    /** Registers an applicant from the presentation DTO (with tipoDocumento and ubicacion as text). */
    SolicitanteResponse saveFromRequest(SolicitanteRequest request);

    /** Updates an applicant from the presentation DTO. */
    SolicitanteResponse updateFromRequest(Long id, SolicitanteRequest request);

    // ── Support operations for the Applicant Master (frontend) ──────────────

    /** Finds an applicant by document number (active or inactive). */
    Optional<Solicitante> findInternalByNumeroDocumento(String numeroDocumento);

    /** Finds an applicant by document number and returns a response DTO. */
    Optional<SolicitanteResponse> findByNumeroDocumento(String numeroDocumento);

    /** Catalog of document types: [{id, codigo, nombre}]. */
    List<Map<String, Object>> listTiposDocumento();

    /** Catalog of ubigeos: [{id, departamento, provincia, distrito, estado}]. */
    List<Map<String, Object>> listUbigeos();
}
