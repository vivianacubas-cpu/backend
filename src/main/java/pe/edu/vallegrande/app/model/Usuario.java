package pe.edu.vallegrande.app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing an administrative user of the FUT system.
 * Maps to the 'usuario' database table.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    @JsonProperty("userId")
    private Integer idUsuario;

    @Column(name = "id_rol", nullable = false)
    @JsonProperty("roleId")
    private Integer idRol;

    @Column(name = "id_area", nullable = false)
    @JsonProperty("areaId")
    private Integer idArea;

    @Column(name = "id_ubigeo", nullable = false)
    @JsonProperty("ubigeoId")
    private Integer idUbigeo;

    @Column(name = "nombre_usuario", nullable = false, length = 50, unique = true)
    @JsonProperty("username")
    private String nombreUsuario;

    @Column(name = "nombres", nullable = false, length = 80)
    @JsonProperty("firstNames")
    private String nombres;

    @Column(name = "apellido_paterno", nullable = false, length = 60)
    @JsonProperty("paternalLastName")
    private String apellidoPaterno;

    @Column(name = "apellido_materno", nullable = false, length = 60)
    @JsonProperty("maternalLastName")
    private String apellidoMaterno;

    @Column(name = "correo", nullable = false, length = 120, unique = true)
    @JsonProperty("email")
    private String correo;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Transient
    @JsonProperty(value = "password", access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(name = "fecha_registro", nullable = false)
    @JsonProperty("createdAt")
    private LocalDateTime fechaRegistro;

    @Column(name = "estado", nullable = false)
    @JsonProperty("status")
    private Boolean estado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    @JsonProperty("registeredAt")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    @JsonProperty("updatedAt")
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_eliminacion")
    @JsonProperty("deletedAt")
    private LocalDateTime fechaEliminacion;

    @Column(name = "fecha_restauracion")
    @JsonProperty("restoredAt")
    private LocalDateTime fechaRestauracion;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (estado == null) estado = true;
        if (fechaRegistro == null) fechaRegistro = now;
        if (fechaCreacion == null) fechaCreacion = now;
    }

    @PreUpdate
    public void preUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    public Usuario() {}

    public Usuario(Integer idUsuario, Integer idRol, Integer idArea, Integer idUbigeo,
                   String nombreUsuario, String nombres, String apellidoPaterno, String apellidoMaterno,
                   String correo, String passwordHash, String password,
                   LocalDateTime fechaRegistro, Boolean estado,
                   LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion,
                   LocalDateTime fechaEliminacion, LocalDateTime fechaRestauracion) {
        this.idUsuario = idUsuario;
        this.idRol = idRol;
        this.idArea = idArea;
        this.idUbigeo = idUbigeo;
        this.nombreUsuario = nombreUsuario;
        this.nombres = nombres;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.correo = correo;
        this.passwordHash = passwordHash;
        this.password = password;
        this.fechaRegistro = fechaRegistro;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
        this.fechaEliminacion = fechaEliminacion;
        this.fechaRestauracion = fechaRestauracion;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Integer idUsuario;
        private Integer idRol;
        private Integer idArea;
        private Integer idUbigeo;
        private String nombreUsuario;
        private String nombres;
        private String apellidoPaterno;
        private String apellidoMaterno;
        private String correo;
        private String passwordHash;
        private String password;
        private LocalDateTime fechaRegistro;
        private Boolean estado;
        private LocalDateTime fechaCreacion;
        private LocalDateTime fechaActualizacion;
        private LocalDateTime fechaEliminacion;
        private LocalDateTime fechaRestauracion;

        public Builder idUsuario(Integer v) { this.idUsuario = v; return this; }
        public Builder idRol(Integer v) { this.idRol = v; return this; }
        public Builder idArea(Integer v) { this.idArea = v; return this; }
        public Builder idUbigeo(Integer v) { this.idUbigeo = v; return this; }
        public Builder nombreUsuario(String v) { this.nombreUsuario = v; return this; }
        public Builder nombres(String v) { this.nombres = v; return this; }
        public Builder apellidoPaterno(String v) { this.apellidoPaterno = v; return this; }
        public Builder apellidoMaterno(String v) { this.apellidoMaterno = v; return this; }
        public Builder correo(String v) { this.correo = v; return this; }
        public Builder passwordHash(String v) { this.passwordHash = v; return this; }
        public Builder password(String v) { this.password = v; return this; }
        public Builder fechaRegistro(LocalDateTime v) { this.fechaRegistro = v; return this; }
        public Builder estado(Boolean v) { this.estado = v; return this; }
        public Builder fechaCreacion(LocalDateTime v) { this.fechaCreacion = v; return this; }
        public Builder fechaActualizacion(LocalDateTime v) { this.fechaActualizacion = v; return this; }
        public Builder fechaEliminacion(LocalDateTime v) { this.fechaEliminacion = v; return this; }
        public Builder fechaRestauracion(LocalDateTime v) { this.fechaRestauracion = v; return this; }

        public Usuario build() {
            return new Usuario(idUsuario, idRol, idArea, idUbigeo, nombreUsuario, nombres,
                    apellidoPaterno, apellidoMaterno, correo, passwordHash, password,
                    fechaRegistro, estado, fechaCreacion, fechaActualizacion,
                    fechaEliminacion, fechaRestauracion);
        }
    }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public Integer getIdRol() { return idRol; }
    public void setIdRol(Integer idRol) { this.idRol = idRol; }
    public Integer getIdArea() { return idArea; }
    public void setIdArea(Integer idArea) { this.idArea = idArea; }
    public Integer getIdUbigeo() { return idUbigeo; }
    public void setIdUbigeo(Integer idUbigeo) { this.idUbigeo = idUbigeo; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidoPaterno() { return apellidoPaterno; }
    public void setApellidoPaterno(String apellidoPaterno) { this.apellidoPaterno = apellidoPaterno; }
    public String getApellidoMaterno() { return apellidoMaterno; }
    public void setApellidoMaterno(String apellidoMaterno) { this.apellidoMaterno = apellidoMaterno; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public Boolean getEstado() { return estado; }
    public void setEstado(Boolean estado) { this.estado = estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
    public LocalDateTime getFechaEliminacion() { return fechaEliminacion; }
    public void setFechaEliminacion(LocalDateTime fechaEliminacion) { this.fechaEliminacion = fechaEliminacion; }
    public LocalDateTime getFechaRestauracion() { return fechaRestauracion; }
    public void setFechaRestauracion(LocalDateTime fechaRestauracion) { this.fechaRestauracion = fechaRestauracion; }
}
