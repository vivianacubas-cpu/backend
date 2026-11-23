package pe.edu.vallegrande.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO returned after successful authentication.
 */
public class LoginResponse {

    @JsonProperty("userId")
    private Integer idUsuario;

    @JsonProperty("username")
    private String nombreUsuario;

    @JsonProperty("firstNames")
    private String nombres;

    @JsonProperty("paternalLastName")
    private String apellidoPaterno;

    @JsonProperty("maternalLastName")
    private String apellidoMaterno;

    @JsonProperty("roleId")
    private Integer idRol;

    @JsonProperty("role")
    private String rol;

    public LoginResponse() {}

    public LoginResponse(Integer idUsuario, String nombreUsuario, String nombres,
                         String apellidoPaterno, String apellidoMaterno,
                         Integer idRol, String rol) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.nombres = nombres;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.idRol = idRol;
        this.rol = rol;
    }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidoPaterno() { return apellidoPaterno; }
    public void setApellidoPaterno(String apellidoPaterno) { this.apellidoPaterno = apellidoPaterno; }

    public String getApellidoMaterno() { return apellidoMaterno; }
    public void setApellidoMaterno(String apellidoMaterno) { this.apellidoMaterno = apellidoMaterno; }

    public Integer getIdRol() { return idRol; }
    public void setIdRol(Integer idRol) { this.idRol = idRol; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}
