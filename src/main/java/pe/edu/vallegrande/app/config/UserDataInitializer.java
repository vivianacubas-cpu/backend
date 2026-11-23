package pe.edu.vallegrande.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.app.model.Usuario;
import pe.edu.vallegrande.app.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds default administrative users on startup.
 * Only inserts a user if it does not already exist (idempotent).
 */
@Component
public class UserDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserDataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserDataInitializer(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
        List<UserSeed> seeds = List.of(
                new UserSeed("directora.demo", "Directora Demo", "Demo", "Apellido", "directora.demo@ie20191.edu.pe", "DirectoraDemo2026!", 1, 1, 1),
                new UserSeed("secretaria.demo", "Secretaria Demo", "Demo", "Apellido", "secretaria.demo@ie20191.edu.pe", "SecretariaDemo2026!", 2, 1, 1),
                new UserSeed("admin.demo", "Administrador Demo", "Demo", "Apellido", "admin.demo@ie20191.edu.pe", "AdminDemo2026!", 3, 1, 1)
        );

        for (UserSeed seed : seeds) {
            if (usuarioRepository.findByNombreUsuarioAndEstado(seed.username, true).isPresent()) {
                continue;
            }

            Usuario usuario = new Usuario();
            usuario.setIdRol(seed.roleId);
            usuario.setIdArea(seed.areaId);
            usuario.setIdUbigeo(seed.ubigeoId);
            usuario.setNombreUsuario(seed.username);
            usuario.setNombres(seed.names);
            usuario.setApellidoPaterno(seed.lastName);
            usuario.setApellidoMaterno(seed.motherLastName);
            usuario.setCorreo(seed.email);
            usuario.setPasswordHash(passwordEncoder.encode(seed.password));
            usuario.setFechaRegistro(LocalDateTime.now());
            usuario.setEstado(true);
            usuario.setFechaCreacion(LocalDateTime.now());
            usuario.setFechaActualizacion(null);
            usuario.setFechaEliminacion(null);
            usuario.setFechaRestauracion(null);
            usuarioRepository.save(usuario);
            log.info("Default user inserted: {}", seed.username);
        }
        } catch (Exception e) {
            log.warn("UserDataInitializer: could not seed users — no database connection: {}", e.getMessage());
        }
    }

    private record UserSeed(
            String username,
            String names,
            String lastName,
            String motherLastName,
            String email,
            String password,
            Integer roleId,
            Integer areaId,
            Integer ubigeoId
    ) {}
}
