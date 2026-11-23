package pe.edu.vallegrande.app.rest;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.app.dto.GestionTramiteFormRequest;
import pe.edu.vallegrande.app.dto.GestionTramiteResponse;
import pe.edu.vallegrande.app.dto.SolicitanteRequest;
import pe.edu.vallegrande.app.dto.SolicitanteResponse;
import pe.edu.vallegrande.app.dto.SolicitudRequest;
import pe.edu.vallegrande.app.model.TipoTramite;
import pe.edu.vallegrande.app.service.GestionTramiteFutService;
import pe.edu.vallegrande.app.service.SolicitanteService;
import pe.edu.vallegrande.app.service.SolicitudService;
import pe.edu.vallegrande.app.service.TipoTramiteService;

import java.util.List;
import java.util.Map;

/**
 * Legacy URL aliases — hidden from Swagger UI.
 * These controllers forward the old Spanish-path URLs to the same service layer,
 * keeping the existing Angular frontend working without any frontend changes.
 *
 * Old path → New path:
 *   /v1/api/solicitante   → /v1/api/applicant
 *   /v1/api/solicitud     → /v1/api/request
 *   /v1/api/tipo-tramite  → /v1/api/procedure-type
 *   /v1/api/gestion-tramite → /v1/api/management
 */

// ── /v1/api/solicitante ───────────────────────────────────────────────────────
@Hidden
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/v1/api/solicitante")
class LegacySolicitanteAlias {

    private final SolicitanteService service;

    LegacySolicitanteAlias(SolicitanteService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<SolicitanteResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitanteResponse> findById(@PathVariable Long id) {
        return service.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<SolicitanteResponse>> findByEstado(@PathVariable String estado) {
        return ResponseEntity.ok(service.findByEstado(estado));
    }

    @GetMapping("/documento/{doc}")
    public ResponseEntity<SolicitanteResponse> findByDoc(@PathVariable String doc) {
        return service.findByNumeroDocumento(doc).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tipos-documento")
    public ResponseEntity<List<Map<String, Object>>> listDocTypes() {
        return ResponseEntity.ok(service.listTiposDocumento());
    }

    @GetMapping("/ubigeos")
    public ResponseEntity<List<Map<String, Object>>> listUbigeos() {
        return ResponseEntity.ok(service.listUbigeos());
    }

    @PostMapping("/save")
    public ResponseEntity<SolicitanteResponse> save(@Valid @RequestBody SolicitanteRequest req) {
        return ResponseEntity.ok(service.saveFromRequest(req));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<SolicitanteResponse> update(@PathVariable Long id,
                                                       @RequestBody SolicitanteRequest req) {
        return ResponseEntity.ok(service.updateFromRequest(id, req));
    }

    @PatchMapping("/restore/{id}")
    public ResponseEntity<SolicitanteResponse> restore(@PathVariable Long id) {
        return ResponseEntity.ok(service.restore(id));
    }

    @PatchMapping("/delete/{id}")
    public ResponseEntity<SolicitanteResponse> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.delete(id));
    }
}

// ── /v1/api/solicitud ─────────────────────────────────────────────────────────
@Hidden
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/v1/api/solicitud")
class LegacySolicitudAlias {

    private final SolicitudService service;

    LegacySolicitudAlias(SolicitudService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody SolicitudRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PostMapping({"/", "/save"})
    public ResponseEntity<Map<String, Object>> createAlias(@RequestBody SolicitudRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) String estado) {
        return service.list(estado);
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @GetMapping("/seguimiento/{codigo}")
    public Map<String, Object> track(@PathVariable String codigo) {
        return service.track(codigo);
    }

    @GetMapping("/eliminadas")
    public List<Map<String, Object>> deleted() {
        return service.deleted();
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @Valid @RequestBody SolicitudRequest req) {
        return service.update(id, req);
    }

    @PatchMapping("/{id}/estado")
    public Map<String, Object> updateStatus(@PathVariable Long id,
                                             @RequestBody Map<String, String> body) {
        return service.updateStatus(id, body.get("estado"), body.get("observacion"));
    }

    @PatchMapping("/{id}/restaurar")
    public Map<String, Object> restore(@PathVariable Long id) {
        return service.restore(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}

// ── /v1/api/tipo-tramite ──────────────────────────────────────────────────────
@Hidden
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/v1/api/tipo-tramite")
class LegacyTipoTramiteAlias {

    private final TipoTramiteService service;

    LegacyTipoTramiteAlias(TipoTramiteService service) { this.service = service; }

    @GetMapping
    public List<TipoTramite> findAll() { return service.findAll(); }

    @GetMapping("/estado/{estado}")
    public List<TipoTramite> findByEstado(@PathVariable Boolean estado) {
        return service.findByEstado(estado);
    }

    @GetMapping("/{id}")
    public TipoTramite findById(@PathVariable Integer id) { return service.findById(id); }

    @PostMapping
    public ResponseEntity<TipoTramite> save(@RequestBody TipoTramite t) {
        return ResponseEntity.ok(service.save(t));
    }

    @PostMapping({"/", "/save"})
    public ResponseEntity<TipoTramite> saveAlias(@RequestBody TipoTramite t) {
        return ResponseEntity.ok(service.save(t));
    }

    @PutMapping("/{id}")
    public TipoTramite update(@PathVariable Integer id, @RequestBody TipoTramite t) {
        return service.update(id, t);
    }

    @PutMapping("/update/{id}")
    public TipoTramite updateAlias(@PathVariable Integer id, @RequestBody TipoTramite t) {
        return service.update(id, t);
    }

    @PatchMapping("/{id}/estado")
    public TipoTramite changeState(@PathVariable Integer id,
                                    @RequestBody Map<String, Boolean> body) {
        return service.changeState(id, body.get("estado"));
    }
}

// ── /v1/api/gestion-tramite ───────────────────────────────────────────────────
@Hidden
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/v1/api/gestion-tramite")
class LegacyGestionTramiteAlias {

    private final GestionTramiteFutService service;
    private final pe.edu.vallegrande.app.repository.AreaRepository areaRepository;

    LegacyGestionTramiteAlias(GestionTramiteFutService service,
                               pe.edu.vallegrande.app.repository.AreaRepository areaRepository) {
        this.service = service;
        this.areaRepository = areaRepository;
    }

    @PostMapping
    public ResponseEntity<GestionTramiteResponse> save(@Valid @RequestBody GestionTramiteFormRequest req) {
        return ResponseEntity.ok(service.save(req));
    }

    @GetMapping
    public ResponseEntity<List<GestionTramiteResponse>> findAll(
            @RequestParam(required = false) String estadoGestion) {
        return ResponseEntity.ok(service.findActivas(estadoGestion));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GestionTramiteResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/eliminados")
    public ResponseEntity<List<GestionTramiteResponse>> findDeleted() {
        return ResponseEntity.ok(service.findEliminadas());
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<GestionTramiteResponse> update(@PathVariable Integer id,
                                                          @RequestBody GestionTramiteFormRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @PatchMapping("/restore/{id}")
    public ResponseEntity<GestionTramiteResponse> restore(@PathVariable Integer id) {
        return ResponseEntity.ok(service.restore(id));
    }

    @PatchMapping("/delete/{id}")
    public ResponseEntity<GestionTramiteResponse> delete(@PathVariable Integer id) {
        return ResponseEntity.ok(service.delete(id));
    }

    /** Catálogo de áreas para el formulario del frontend.
     *  Devuelve { areaId, name } igual que el endpoint primario /v1/api/management/catalogo/areas. */
    @GetMapping("/catalogo/areas")
    public ResponseEntity<List<Map<String, Object>>> catalogoAreas() {
        List<Map<String, Object>> result = areaRepository.findAll().stream()
                .map(a -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("areaId", a.getIdArea());
                    m.put("name",   a.getNombre());
                    return m;
                })
                .toList();
        return ResponseEntity.ok(result);
    }
}
