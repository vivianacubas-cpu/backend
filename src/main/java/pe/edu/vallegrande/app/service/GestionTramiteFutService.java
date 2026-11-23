package pe.edu.vallegrande.app.service;

import pe.edu.vallegrande.app.dto.GestionTramiteFormRequest;
import pe.edu.vallegrande.app.dto.GestionTramiteResponse;

import java.util.List;

/**
 * Business contract for internal FUT procedure management.
 */
public interface GestionTramiteFutService {

    /** Register a new management record. */
    GestionTramiteResponse save(GestionTramiteFormRequest request);

    /** List active management records. Optionally filtered by estadoGestion. */
    List<GestionTramiteResponse> findActivas(String estadoGestion);

    /** List logically deleted management records. */
    List<GestionTramiteResponse> findEliminadas();

    /** Find by ID. Throws NoSuchElementException if not found. */
    GestionTramiteResponse findById(Integer id);

    /** List active management records for a given applicant. */
    List<GestionTramiteResponse> findBySolicitante(Long idSolicitante);

    /** Update an existing management record. */
    GestionTramiteResponse update(Integer id, GestionTramiteFormRequest request);

    /** Logical deletion: status A → I. Idempotent. */
    GestionTramiteResponse delete(Integer id);

    /** Restore: status I → A. Idempotent. */
    GestionTramiteResponse restore(Integer id);
}
