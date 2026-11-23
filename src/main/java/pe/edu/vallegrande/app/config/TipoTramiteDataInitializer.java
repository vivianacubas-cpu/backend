package pe.edu.vallegrande.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Fills in the requisitos and vigente_desde fields in dbo.tipo_tramite.
 * Only updates rows where those fields are NULL (idempotent).
 * Runs at Spring Boot startup, after the schema initializer (@Order 4).
 */
@Component
@Order(4)
public class TipoTramiteDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TipoTramiteDataInitializer.class);

    private final JdbcTemplate jdbc;

    public TipoTramiteDataInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
        // Verify the table exists
        Integer tableExists = jdbc.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='tipo_tramite'",
            Integer.class);
        if (tableExists == null || tableExists == 0) return;

        // Verify the vigente_desde column exists
        Integer colExists = jdbc.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='tipo_tramite' AND COLUMN_NAME='vigente_desde'",
            Integer.class);

        int updated = 0;

        // Update requirements where they are NULL (by procedure type name)
        updated += updateRequirements("SOLICITUD DE VACANTES",
            "DNI or CNE of the applicant; Birth certificate of the student; " +
            "Grade report or report card from the previous year (if applicable)");

        updated += updateRequirements("TRASLADO DE MATRÍCULA",
            "DNI or CNE of the applicant; Transfer certificate from the student's previous school; " +
            "Grade report for the completed year; Birth certificate of the student");

        updated += updateRequirements("CONSTANCIA DE ESTUDIOS O DE MATRÍCULA",
            "DNI or CNE of the applicant; Written request specifying the year and grade; " +
            "Must be enrolled in the current school year");

        updated += updateRequirements("CERTIFICADO DE ESTUDIOS",
            "DNI or CNE of the applicant; Written request specifying the years of study to certify; " +
            "No outstanding debts with the institution");

        updated += updateRequirements("RECTIFICACIÓN DE DATOS",
            "DNI or CNE of the applicant; Official document proving the correct data " +
            "(birth certificate, updated ID, or other); Written request detailing the error to be corrected");

        updated += updateRequirements("JUSTIFICACIÓN DE INASISTENCIAS PROLONGADAS",
            "DNI or CNE of the applicant; Medical certificate or other document proving the reason for absence; " +
            "Written request signed by the guardian or applicant");

        updated += updateRequirements("LICENCIAS Y PERMISOS",
            "DNI or CNE of the applicant; Written request detailing the reason and requested period; " +
            "Supporting documentation according to the type of license or permit");

        updated += updateRequirements("CONSTANCIA DE PAGO O DE TRABAJO",
            "DNI or CNE of the applicant; Written request specifying the type of certificate required; " +
            "Must be active in the institution's system");

        updated += updateRequirements("ENTREGA DE LEGAJOS Y DOCUMENTOS",
            "DNI or CNE of the applicant; Written request specifying the documents required; " +
            "Proof of relationship with the record holder");

        updated += updateRequirements("RECLAMOS O ABSOLUCIÓN DE CONSULTAS ADMINISTRATIVAS",
            "DNI or CNE of the applicant; Written request describing the complaint or inquiry; " +
            "Supporting documentation if applicable");

        updated += updateRequirements("OTROS",
            "DNI or CNE of the applicant; Written request specifying the required procedure; " +
            "Additional documentation according to the procedure type");

        if (updated > 0) {
            log.info("TipoTramiteDataInitializer: {} tipo_tramite record(s) updated with requirements.", updated);
        } else {
            log.debug("TipoTramiteDataInitializer: all tipo_tramite requirements were already complete.");
        }

        // Update vigente_desde where it is NULL (use registration date or base date of the school year)
        if (colExists != null && colExists > 0) {
            int vigente = jdbc.update(
                "UPDATE dbo.tipo_tramite " +
                "SET vigente_desde = CAST(COALESCE(fecha_registro, SYSDATETIME()) AS DATE) " +
                "WHERE vigente_desde IS NULL AND estado = 1");
            if (vigente > 0) {
                log.info("TipoTramiteDataInitializer: {} record(s) updated with vigente_desde.", vigente);
            }
        }
        } catch (Exception e) {
            log.warn("TipoTramiteDataInitializer: could not run — no database connection: {}", e.getMessage());
        }
    }

    /**
     * Updates the requisitos field of a procedure type identified by name,
     * only if it is currently NULL or empty.
     *
     * @return number of rows updated (0 or 1)
     */
    private int updateRequirements(String name, String requirements) {
        try {
            return jdbc.update(
                "UPDATE dbo.tipo_tramite " +
                "SET requisitos = ? " +
                "WHERE UPPER(nombre) = UPPER(?) " +
                "  AND (requisitos IS NULL OR LTRIM(RTRIM(requisitos)) = '')",
                requirements, name);
        } catch (Exception e) {
            log.warn("Could not update requirements for '{}': {}", name, e.getMessage());
            return 0;
        }
    }
}
