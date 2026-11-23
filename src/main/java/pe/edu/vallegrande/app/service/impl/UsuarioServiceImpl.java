package pe.edu.vallegrande.app.service.impl;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.vallegrande.app.model.Usuario;
import pe.edu.vallegrande.app.repository.UsuarioRepository;
import pe.edu.vallegrande.app.service.UsuarioService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of the UsuarioService.
 * Handles CRUD operations for administrative users including password hashing.
 */
@Service
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository repository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UsuarioServiceImpl(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> findById(Integer id) {
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> findByEstado(Boolean estado) {
        return repository.findByEstado(estado);
    }

    @Override
    public Usuario save(Usuario usuario) {
        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The password is required");
        }
        if (repository.existsByNombreUsuario(usuario.getNombreUsuario())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The username already exists");
        }
        if (repository.existsByCorreo(usuario.getCorreo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The email is already registered");
        }

        LocalDateTime now = LocalDateTime.now();
        usuario.setIdUsuario(null);
        usuario.setPasswordHash(passwordEncoder.encode(usuario.getPassword()));
        usuario.setPassword(null);
        usuario.setFechaRegistro(now);
        usuario.setEstado(true);
        usuario.setFechaCreacion(now);
        usuario.setFechaActualizacion(null);
        usuario.setFechaEliminacion(null);
        usuario.setFechaRestauracion(null);
        return repository.save(usuario);
    }

    @Override
    public Optional<Usuario> update(Integer id, Usuario data) {
        return repository.findById(id).map(usuario -> {
            if (repository.existsByNombreUsuarioAndIdUsuarioNot(data.getNombreUsuario(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "The username already exists");
            }
            if (repository.existsByCorreoAndIdUsuarioNot(data.getCorreo(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "The email is already registered");
            }

            usuario.setIdRol(data.getIdRol());
            usuario.setIdArea(data.getIdArea());
            usuario.setIdUbigeo(data.getIdUbigeo());
            usuario.setNombreUsuario(data.getNombreUsuario());
            usuario.setNombres(data.getNombres());
            usuario.setApellidoPaterno(data.getApellidoPaterno());
            usuario.setApellidoMaterno(data.getApellidoMaterno());
            usuario.setCorreo(data.getCorreo());
            // Only change the password if a new one is provided
            if (data.getPassword() != null && !data.getPassword().isBlank()) {
                usuario.setPasswordHash(passwordEncoder.encode(data.getPassword()));
            }
            usuario.setFechaActualizacion(LocalDateTime.now());
            return repository.save(usuario);
        });
    }

    @Override
    public boolean delete(Integer id) {
        return repository.findById(id).map(usuario -> {
            usuario.setEstado(false);
            usuario.setFechaEliminacion(LocalDateTime.now());
            repository.save(usuario);
            return true;
        }).orElse(false);
    }

    @Override
    public boolean restore(Integer id) {
        return repository.findById(id).map(usuario -> {
            usuario.setEstado(true);
            // Preserve the original deletion date so it remains visible
            // even after the user has been restored.
            usuario.setFechaRestauracion(LocalDateTime.now());
            repository.save(usuario);
            return true;
        }).orElse(false);
    }
}
