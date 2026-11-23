package pe.edu.vallegrande.app.repository;

import pe.edu.vallegrande.app.model.Area;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for managing institutional areas.
 */
public interface AreaRepository extends JpaRepository<Area, Integer> {

    /** Finds an area by its exact name (case-insensitive). */
    Optional<Area> findByNombreIgnoreCase(String nombre);
}
