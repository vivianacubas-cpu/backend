package pe.edu.vallegrande.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds the departamento, provincia, and distrito columns to dbo.solicitante
 * if they do not already exist. Runs at Spring Boot startup with ddl-auto: none.
 *
 * NOTE: These columns are no longer populated automatically from the ubigeo catalog.
 * The location is entered freely by the user in the form.
 * The id_ubigeo field is kept with value 1 (generic fallback ubigeo)
 * only to satisfy the NOT NULL constraint on the column.
 */
@Component
@Order(2)
public class SolicitanteUbigeoInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SolicitanteUbigeoInitializer.class);
    private final JdbcTemplate jdbc;

    public SolicitanteUbigeoInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        addColumn("departamento", "NVARCHAR(100)");
        addColumn("provincia",    "NVARCHAR(100)");
        addColumn("distrito",     "NVARCHAR(100)");
        // Data is no longer migrated from the ubigeo catalog.
        // The user enters the location freely in the form.
    }

    private void addColumn(String column, String type) {
        try {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = 'solicitante' AND COLUMN_NAME = ?",
                Integer.class, column);
            if (count == null || count == 0) {
                jdbc.execute("ALTER TABLE dbo.solicitante ADD " + column + " " + type + " NULL");
                log.info("Column {} added to dbo.solicitante.", column);
            } else {
                log.debug("Column {} already exists in dbo.solicitante.", column);
            }
        } catch (Exception e) {
            log.warn("Could not add column {} to solicitante: {}", column, e.getMessage());
        }
    }
}
