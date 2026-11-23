package pe.edu.vallegrande.app.rest;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.app.dto.RespuestaTramiteRequest;
import pe.edu.vallegrande.app.model.RespuestaTramite;
import pe.edu.vallegrande.app.repository.RespuestaTramiteRepository;
import pe.edu.vallegrande.app.repository.SolicitudRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Management of official responses to procedure requests.
 * Kept functional but hidden from Swagger (internal admin panel module).
 * Responses are queried from the request detail in the admin panel.
 */
@Hidden
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/v1/api/respuesta")
public class RespuestaTramiteRest {

    private final RespuestaTramiteRepository respuestaRepository;
    private final SolicitudRepository solicitudRepository;

    public RespuestaTramiteRest(RespuestaTramiteRepository respuestaRepository,
                                SolicitudRepository solicitudRepository) {
        this.respuestaRepository = respuestaRepository;
        this.solicitudRepository = solicitudRepository;
    }

    @PostMapping
    public ResponseEntity<RespuestaTramite> save(@RequestBody RespuestaTramiteRequest request) {
        if (request.getIdSolicitud() == null) {
            throw new IllegalArgumentException("The idSolicitud field is required.");
        }
        if (request.getRespuesta() == null || request.getRespuesta().isBlank()) {
            throw new IllegalArgumentException("The respuesta field is required.");
        }
        if (request.getRespuesta().length() > 2000) {
            throw new IllegalArgumentException("The response cannot exceed 2000 characters.");
        }

        solicitudRepository.findById(request.getIdSolicitud())
                .orElseThrow(() -> new NoSuchElementException(
                        "Request not found with ID: " + request.getIdSolicitud()));

        RespuestaTramite respuesta = new RespuestaTramite();
        respuesta.setIdSolicitud(request.getIdSolicitud());
        respuesta.setIdUsuario(request.getIdUsuario());
        respuesta.setRespuesta(request.getRespuesta().trim());
        respuesta.setFechaRespuesta(LocalDateTime.now());

        return ResponseEntity.ok(respuestaRepository.save(respuesta));
    }

    @GetMapping("/solicitud/{idSolicitud}")
    public ResponseEntity<List<RespuestaTramite>> findBySolicitud(@PathVariable Long idSolicitud) {
        solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new NoSuchElementException(
                        "Request not found with ID: " + idSolicitud));
        return ResponseEntity.ok(
                respuestaRepository.findByIdSolicitudOrderByFechaRespuestaDesc(idSolicitud));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaTramite> findById(@PathVariable Long id) {
        return respuestaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<RespuestaTramite> update(@PathVariable Long id,
                                                   @RequestBody RespuestaTramiteRequest request) {
        if (request.getRespuesta() == null || request.getRespuesta().isBlank()) {
            throw new IllegalArgumentException("The respuesta field is required.");
        }
        RespuestaTramite existing = respuestaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Response not found with ID: " + id));
        existing.setRespuesta(request.getRespuesta().trim());
        if (request.getIdUsuario() != null) {
            existing.setIdUsuario(request.getIdUsuario());
        }
        return ResponseEntity.ok(respuestaRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        RespuestaTramite existing = respuestaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Response not found with ID: " + id));
        respuestaRepository.delete(existing);
        return ResponseEntity.ok().build();
    }
}
