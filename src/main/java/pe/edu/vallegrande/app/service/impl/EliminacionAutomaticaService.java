package pe.edu.vallegrande.app.service.impl;

import pe.edu.vallegrande.app.model.Solicitud;
import pe.edu.vallegrande.app.repository.SolicitudRepository;
import pe.edu.vallegrande.app.repository.SeguimientoTramiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service that automatically deletes old FUT requests.
 * Runs every day at 2:00 AM and permanently removes requests older than 30 days.
 */
@Service
public class EliminacionAutomaticaService {

    private static final Logger log = LoggerFactory.getLogger(EliminacionAutomaticaService.class);
    private final SolicitudRepository solicitudRepository;
    private final SeguimientoTramiteRepository seguimientoRepository;

    public EliminacionAutomaticaService(
            SolicitudRepository solicitudRepository,
            SeguimientoTramiteRepository seguimientoRepository) {
        this.solicitudRepository = solicitudRepository;
        this.seguimientoRepository = seguimientoRepository;
    }

    // Runs every day at 2:00 AM.
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void deleteOldRequests() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        List<Solicitud> oldRequests =
                solicitudRepository.findByFechaRegistroBefore(cutoffDate);

        for (Solicitud solicitud : oldRequests) {
            try {
                // Delete the associated history first.
                seguimientoRepository.deleteByIdSolicitud(
                        solicitud.getIdSolicitud());

                // Permanently delete the request.
                solicitudRepository.delete(solicitud);
                log.info(
                        "Request permanently deleted due to age. ID: {}",
                        solicitud.getIdSolicitud());
            } catch (Exception ex) {
                log.error(
                        "Could not delete request ID: {}",
                        solicitud.getIdSolicitud(),
                        ex);
                throw ex;
            }
        }

        log.info(
                "Automatic cleanup process completed. Requests reviewed: {}",
                oldRequests.size());
    }
}
