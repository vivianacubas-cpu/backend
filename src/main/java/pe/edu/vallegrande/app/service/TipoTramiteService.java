package pe.edu.vallegrande.app.service;

import pe.edu.vallegrande.app.model.TipoTramite;
import java.util.List;

/**
 * Service interface for procedure type (TipoTramite) management.
 */
public interface TipoTramiteService {
    List<TipoTramite> findAll();
    List<TipoTramite> findByEstado(Boolean estado);
    TipoTramite findById(Integer id);
    TipoTramite save(TipoTramite tipoTramite);
    TipoTramite update(Integer id, TipoTramite tipoTramite);
    TipoTramite changeState(Integer id, Boolean estado);
}
