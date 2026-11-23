package pe.edu.vallegrande.app.repository;
import pe.edu.vallegrande.app.model.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
public interface SolicitudRepository
        extends JpaRepository<Solicitud, Long> {
    Optional<Solicitud> findByCodigoSeguimientoIgnoreCase(
            String codigoSeguimiento);
    Optional<Solicitud> findByIdempotencyKey(String idempotencyKey);
    List<Solicitud> findByEstadoIgnoreCaseOrderByFechaRegistroDesc(
            String estado);
    List<Solicitud> findAllByOrderByFechaRegistroDesc();
    List<Solicitud> findByFechaRegistroBefore(
            LocalDateTime fechaLimite);
}
