package pe.edu.vallegrande.app.repository;

import pe.edu.vallegrande.app.model.RespuestaTramite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repository for managing official responses to FUT procedure requests.
 */
public interface RespuestaTramiteRepository extends JpaRepository<RespuestaTramite, Long> {

    // Retrieve all responses for a given request, ordered from most recent to oldest.
    List<RespuestaTramite> findByIdSolicitudOrderByFechaRespuestaDesc(Long idSolicitud);
}
