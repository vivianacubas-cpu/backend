package pe.edu.vallegrande.app.service;

import pe.edu.vallegrande.app.model.Usuario;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for administrative user management.
 */
public interface UsuarioService {
    List<Usuario> findAll();
    Optional<Usuario> findById(Integer id);
    List<Usuario> findByEstado(Boolean estado);
    Usuario save(Usuario usuario);
    Optional<Usuario> update(Integer id, Usuario usuario);
    boolean delete(Integer id);
    boolean restore(Integer id);
}
