package pe.edu.vallegrande.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

/**
 * Entity representing an institutional area.
 * Maps to the 'area' database table.
 */
@Schema(hidden = true)
@Entity
@Table(name = "area")
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_area")
    @JsonProperty("areaId")
    private Integer idArea;

    @Column(name = "nombre", nullable = false, length = 100, unique = true)
    @JsonProperty("name")
    private String nombre;

    public Area() {}

    public Integer getIdArea() { return idArea; }
    public void setIdArea(Integer idArea) { this.idArea = idArea; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
