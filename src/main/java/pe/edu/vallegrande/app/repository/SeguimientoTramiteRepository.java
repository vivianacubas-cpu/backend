package pe.edu.vallegrande.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.vallegrande.app.model.SeguimientoTramite;

import java.util.List;

/**
 * Repository for managing the status change history (tracking) of FUT procedure requests.
 */
public interface SeguimientoTramiteRepository
        extends JpaRepository<SeguimientoTramite, Long> {

    // Retrieve the history of a request ordered by change date (most recent first).
    List<SeguimientoTramite> findByIdSolicitudOrderByFechaCambioDesc(
            Long idSolicitud
    );

    // Retrieve the history of a request filtered by status.
    List<SeguimientoTramite> findByIdSolicitudAndEstadoOrderByFechaCambioDesc(
            Long idSolicitud,
            String estado
    );

    // Retrieve all records filtered by status.
    List<SeguimientoTramite> findByEstadoOrderByFechaCambioDesc(
            String estado
    );

    // Delete all history records associated with a given request.
    void deleteByIdSolicitud(Long idSolicitud);
}
