package pe.edu.vallegrande.app.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.vallegrande.app.model.TipoTramite;
import pe.edu.vallegrande.app.repository.TipoTramiteRepository;
import pe.edu.vallegrande.app.service.TipoTramiteService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of the TipoTramiteService.
 * Handles CRUD operations and status changes for procedure types.
 */
@Service
public class TipoTramiteServiceImpl implements TipoTramiteService {

    private static final Logger log = LoggerFactory.getLogger(TipoTramiteServiceImpl.class);
    private final TipoTramiteRepository repository;

    public TipoTramiteServiceImpl(TipoTramiteRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TipoTramite> findAll() {
        log.info("Listing all procedure types");
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TipoTramite> findByEstado(Boolean estado) {
        return repository.findByEstadoOrderByNombreAsc(estado);
    }

    @Override
    @Transactional(readOnly = true)
    public TipoTramite findById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(
                        "No procedure type found with ID " + id));
    }

    @Override
    @Transactional
    public TipoTramite save(TipoTramite tipoTramite) {
        validate(tipoTramite);
        if (repository.existsByNombreIgnoreCase(tipoTramite.getNombre().trim())) {
            throw new pe.edu.vallegrande.app.service.exception.DuplicateResourceException(
                    "A procedure type with that name already exists.");
        }
        tipoTramite.setIdTipoTramite(null);
        if (tipoTramite.getCodigo() != null) tipoTramite.setCodigo(tipoTramite.getCodigo().trim().toUpperCase());
        if (tipoTramite.getEstado() == null) tipoTramite.setEstado(Boolean.TRUE);
        if (tipoTramite.getRequiereDocumentos() == null) tipoTramite.setRequiereDocumentos(Boolean.FALSE);
        tipoTramite.setFechaActualizacion(null);
        tipoTramite.setFechaEliminacion(null);
        return repository.save(tipoTramite);
    }

    @Override
    @Transactional
    public TipoTramite update(Integer id, TipoTramite entrada) {
        validate(entrada);
        TipoTramite actual = findById(id);
        if (!actual.getNombre().equalsIgnoreCase(entrada.getNombre().trim())
                && repository.existsByNombreIgnoreCase(entrada.getNombre().trim())) {
            throw new pe.edu.vallegrande.app.service.exception.DuplicateResourceException(
                    "Another procedure type with that name already exists.");
        }
        actual.setNombre(entrada.getNombre().trim());
        actual.setCodigo(entrada.getCodigo() != null ? entrada.getCodigo().trim().toUpperCase() : null);
        actual.setCategoria(entrada.getCategoria());
        actual.setDescripcion(entrada.getDescripcion());
        actual.setRequisitos(entrada.getRequisitos());
        actual.setPlazoEstimadoDias(entrada.getPlazoEstimadoDias());
        actual.setRequiereDocumentos(
            entrada.getRequiereDocumentos() != null ? entrada.getRequiereDocumentos() : Boolean.FALSE);
        actual.setVigentDesde(entrada.getVigentDesde());
        if (entrada.getEstado() != null) actual.setEstado(entrada.getEstado());
        actual.setFechaActualizacion(LocalDateTime.now());
        return repository.save(actual);
    }

    @Override
    @Transactional
    public TipoTramite changeState(Integer id, Boolean estado) {
        if (estado == null) throw new IllegalArgumentException("The status field is required.");
        TipoTramite actual = findById(id);
        actual.setEstado(estado);
        actual.setFechaActualizacion(LocalDateTime.now());
        if (Boolean.FALSE.equals(estado)) {
            // Deactivate: record deletion date, clear restoration date
            actual.setFechaEliminacion(LocalDateTime.now());
            actual.setFechaRestauracion(null);
        } else {
            // Activate/restore: record restoration date, clear deletion date
            actual.setFechaEliminacion(null);
            actual.setFechaRestauracion(LocalDateTime.now());
        }
        return repository.save(actual);
    }

    private void validate(TipoTramite t) {
        if (t == null || t.getNombre() == null || t.getNombre().isBlank())
            throw new IllegalArgumentException("The procedure type name is required.");
        if (t.getNombre().trim().length() > 100)
            throw new IllegalArgumentException("The name cannot exceed 100 characters.");
        if (t.getCategoria() != null && t.getCategoria().length() > 60)
            throw new IllegalArgumentException("The category cannot exceed 60 characters.");
        if (t.getDescripcion() != null && t.getDescripcion().length() > 250)
            throw new IllegalArgumentException("The description cannot exceed 250 characters.");
        if (t.getRequisitos() != null && t.getRequisitos().length() > 500)
            throw new IllegalArgumentException("The requirements cannot exceed 500 characters.");
        if (t.getPlazoEstimadoDias() != null && t.getPlazoEstimadoDias() < 0)
            throw new IllegalArgumentException("The estimated deadline cannot be negative.");
        t.setNombre(t.getNombre().trim());
    }
}
