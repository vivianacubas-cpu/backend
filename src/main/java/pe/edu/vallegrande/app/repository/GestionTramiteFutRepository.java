package pe.edu.vallegrande.app.repository;

import pe.edu.vallegrande.app.model.GestionTramiteFut;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repository for managing FUT procedure management records.
 */
public interface GestionTramiteFutRepository
        extends JpaRepository<GestionTramiteFut, Integer> {

    /** All active management records for a given applicant. */
    List<GestionTramiteFut> findByIdSolicitanteAndEstadoOrderByFechaRegistroDesc(
            Long idSolicitante, String estado);

    /** All management records for a given applicant (including deleted). */
    List<GestionTramiteFut> findByIdSolicitanteOrderByFechaRegistroDesc(
            Long idSolicitante);

    /** List by audit status (A=active, I=deleted). */
    List<GestionTramiteFut> findByEstadoOrderByFechaRegistroDesc(String estado);

    /** List by management status (PENDIENTE, EN_PROCESO, etc.). */
    List<GestionTramiteFut> findByEstadoGestionAndEstadoOrderByFechaRegistroDesc(
            String estadoGestion, String estado);
}
