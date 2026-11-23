package pe.edu.vallegrande.app.rest;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.app.dto.LoginRequest;
import pe.edu.vallegrande.app.dto.LoginResponse;
import pe.edu.vallegrande.app.model.Usuario;
import pe.edu.vallegrande.app.repository.UsuarioRepository;

import java.util.Map;

/**
 * Authentication for administrative users.
 * Kept functional but hidden from Swagger (internal system use).
 */
@Hidden
@RestController
@RequestMapping("/v1/api/auth")
public class AuthRest {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final Map<Integer, String> ROLE_MAP = Map.of(
            1, "DIRECTORA",
            2, "SECRETARIA",
            3, "ADMIN"
    );

    public AuthRest(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Usuario usuario = usuarioRepository
                .findByNombreUsuarioAndEstado(request.getNombreUsuario().trim().toLowerCase(), true)
                .orElse(null);

        if (usuario == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "User not found or inactive."));
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Incorrect password."));
        }

        String role = ROLE_MAP.getOrDefault(usuario.getIdRol(), "ROL_" + usuario.getIdRol());

        LoginResponse response = new LoginResponse(
                usuario.getIdUsuario(),
                usuario.getNombreUsuario(),
                usuario.getNombres(),
                usuario.getApellidoPaterno(),
                usuario.getApellidoMaterno(),
                usuario.getIdRol(),
                role
        );

        return ResponseEntity.ok(response);
    }
}
