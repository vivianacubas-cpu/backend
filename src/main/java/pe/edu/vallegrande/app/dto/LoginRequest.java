package pe.edu.vallegrande.app.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user authentication.
 * Accepts both "username" (new English name) and "nombreUsuario" (legacy Spanish alias).
 */
public class LoginRequest {

    @NotBlank(message = "The username is required")
    @JsonAlias({"nombreUsuario"})
    private String nombreUsuario;

    @NotBlank(message = "The password is required")
    private String password;

    public LoginRequest() {}

    public LoginRequest(String nombreUsuario, String password) {
        this.nombreUsuario = nombreUsuario;
        this.password = password;
    }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
