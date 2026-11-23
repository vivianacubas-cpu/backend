package pe.edu.vallegrande.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures that the tipo_tramite and tramite_fut tables have all four
 * audit columns present.
 *
 * tipo_tramite:
 *   - fecha_restauracion (was missing — added if not present)
 *
 * tramite_fut:
 *   - fecha_actualizacion (was missing — added if not present)
 *   - fecha_restauracion  (was missing — added if not present)
 *
 * Safe to re-run: only acts if the column does not already exist.
 * Runs before business initializers (@Order(3)).
 */
@Component
@Order(3)
public class AuditoriaSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaSchemaInitializer.class);
    private final JdbcTemplate jdbc;

    public AuditoriaSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        // tipo_tramite: add fecha_restauracion
        addColumn("tipo_tramite", "fecha_restauracion", "DATETIME2 NULL");

        // tramite_fut: add fecha_actualizacion and fecha_restauracion
        addColumn("tramite_fut", "fecha_actualizacion", "DATETIME2 NULL");
        addColumn("tramite_fut", "fecha_restauracion",  "DATETIME2 NULL");
    }

    private void addColumn(String table, String column, String type) {
        try {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, table, column);
            if (count == null || count == 0) {
                jdbc.execute("ALTER TABLE dbo." + table + " ADD " + column + " " + type);
                log.info("Column {}.{} added.", table, column);
            } else {
                log.debug("Column {}.{} already exists.", table, column);
            }
        } catch (Exception e) {
            log.warn("Could not add {}.{}: {}", table, column, e.getMessage());
        }
    }
}
