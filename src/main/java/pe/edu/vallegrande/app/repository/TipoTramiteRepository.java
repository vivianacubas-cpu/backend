package pe.edu.vallegrande.app.repository;

import pe.edu.vallegrande.app.model.TipoTramite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing procedure types (TipoTramite).
 */
public interface TipoTramiteRepository extends JpaRepository<TipoTramite, Integer> {

    /** Retrieves all procedure types with the given status, ordered alphabetically by name. */
    List<TipoTramite> findByEstadoOrderByNombreAsc(Boolean estado);

    /** Returns true if a procedure type with the given name already exists (case-insensitive). */
    boolean existsByNombreIgnoreCase(String nombre);

    /** Finds a procedure type by its exact name (case-insensitive). */
    Optional<TipoTramite> findByNombreIgnoreCase(String nombre);
}
