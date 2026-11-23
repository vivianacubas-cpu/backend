package pe.edu.vallegrande.app.rest;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.app.model.Usuario;
import pe.edu.vallegrande.app.service.UsuarioService;
import java.util.List;

/**
 * REST controller for administrative user management.
 * Hidden from Swagger; functionality is preserved intact.
 */
@Hidden // The controller and its functionality are preserved; only excluded from Swagger.
@RestController
@RequestMapping("/v1/api/usuario")
public class UsuarioRest {

    private final UsuarioService service;

    public UsuarioRest(UsuarioService service) {
        this.service = service;
    }

    // LIST ALL
    @GetMapping
    public ResponseEntity<List<Usuario>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    // FIND BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> findById(@PathVariable Integer id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // LIST BY STATUS (true = active, false = inactive)
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Usuario>> findByEstado(
            @Schema(allowableValues = {"true", "false"})
            @PathVariable Boolean estado) {
        return ResponseEntity.ok(service.findByEstado(estado));
    }

    // CREATE
    @PostMapping("/save")
    public ResponseEntity<Usuario> save(@RequestBody Usuario usuario) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.save(usuario));
    }

    // UPDATE
    @PutMapping("/update/{id}")
    public ResponseEntity<Usuario> update(@PathVariable Integer id,
                                          @RequestBody Usuario usuario) {
        return service.update(id, usuario)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // LOGICAL DELETION
    @PatchMapping("/delete/{id}")
    public ResponseEntity<Usuario> delete(@PathVariable Integer id) {
        if (!service.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // LOGICAL RESTORATION
    @PatchMapping("/restore/{id}")
    public ResponseEntity<Usuario> restore(@PathVariable Integer id) {
        if (!service.restore(id)) {
            return ResponseEntity.notFound().build();
        }
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
