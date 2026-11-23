package pe.edu.vallegrande.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.vallegrande.app.model.Usuario;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing administrative users of the FUT system.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    /** Retrieves all users with the given status. */
    List<Usuario> findByEstado(Boolean estado);

    /** Returns true if a user with the given username already exists. */
    boolean existsByNombreUsuario(String nombreUsuario);

    /** Returns true if a user with the given email already exists. */
    boolean existsByCorreo(String correo);

    /** Returns true if another user (different ID) has the same username. */
    boolean existsByNombreUsuarioAndIdUsuarioNot(String nombreUsuario, Integer idUsuario);

    /** Returns true if another user (different ID) has the same email. */
    boolean existsByCorreoAndIdUsuarioNot(String correo, Integer idUsuario);

    /** Finds an active user by username and status. */
    Optional<Usuario> findByNombreUsuarioAndEstado(String nombreUsuario, Boolean estado);
}
