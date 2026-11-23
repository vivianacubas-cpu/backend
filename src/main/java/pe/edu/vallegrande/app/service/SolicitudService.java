package pe.edu.vallegrande.app.service;

import pe.edu.vallegrande.app.dto.SolicitudRequest;

import java.util.List;
import java.util.Map;

/**
 * Service interface for FUT request management.
 *
 * Defines the operations for registration, querying,
 * status update, deletion, and restoration.
 */
public interface SolicitudService {

    // Register a new FUT request.
    Map<String, Object> create(SolicitudRequest request);

    // Query tracking status by request tracking code.
    Map<String, Object> track(String code);

    // Retrieve the list of requests filtered by status.
    List<Map<String, Object>> list(String status);

    // Retrieve the full detail of a request by its ID.
    Map<String, Object> detail(Long id);

    // Update the status and observation note of a request.
    Map<String, Object> updateStatus(
            Long id,
            String status,
            String observation
    );

    // Update the data of an existing request.
    Map<String, Object> update(
            Long id,
            SolicitudRequest request
    );

    // Logically delete a request.
    void delete(Long id);

    // Restore a logically deleted request.
    Map<String, Object> restore(Long id);

    // Retrieve all logically deleted requests.
    List<Map<String, Object>> deleted();
}
