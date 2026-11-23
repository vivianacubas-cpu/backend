package pe.edu.vallegrande.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Ensures the gestion_tramite_fut table has the nombre_responsable column.
 * Runs at Spring Boot startup, before any business operations.
 * Uses JdbcTemplate to execute DDL directly without relying on Hibernate.
 */
@Component
@Order(1)
public class GestionTramiteSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GestionTramiteSchemaInitializer.class);

    private final JdbcTemplate jdbc;

    public GestionTramiteSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 1. Add nombre_responsable if it does not exist
        try {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = 'gestion_tramite_fut' " +
                "AND COLUMN_NAME = 'nombre_responsable'",
                Integer.class);

            if (count == null || count == 0) {
                jdbc.execute(
                    "ALTER TABLE dbo.gestion_tramite_fut " +
                    "ADD nombre_responsable NVARCHAR(150) NULL");
                log.info("Column nombre_responsable added to gestion_tramite_fut.");
            } else {
                log.debug("Column nombre_responsable already exists in gestion_tramite_fut.");
            }
        } catch (Exception e) {
            log.warn("Could not verify/add nombre_responsable: {}", e.getMessage());
        }

        // 2. Remove id_usuario_responsable if it still exists (legacy column)
        try {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = 'gestion_tramite_fut' " +
                "AND COLUMN_NAME = 'id_usuario_responsable'",
                Integer.class);

            if (count != null && count > 0) {
                // Remove FK if it exists
                try {
                    String fkName = jdbc.queryForObject(
                        "SELECT fk.name FROM sys.foreign_keys fk " +
                        "INNER JOIN sys.foreign_key_columns fkc " +
                        "    ON fk.object_id = fkc.constraint_object_id " +
                        "WHERE fk.parent_object_id = OBJECT_ID('dbo.gestion_tramite_fut') " +
                        "AND COL_NAME(fkc.parent_object_id, fkc.parent_column_id) = 'id_usuario_responsable'",
                        String.class);
                    if (fkName != null) {
                        jdbc.execute("ALTER TABLE dbo.gestion_tramite_fut DROP CONSTRAINT " + fkName);
                        log.info("FK {} removed.", fkName);
                    }
                } catch (Exception ignored) {}

                jdbc.execute("ALTER TABLE dbo.gestion_tramite_fut DROP COLUMN id_usuario_responsable");
                log.info("Column id_usuario_responsable removed from gestion_tramite_fut.");
            }
        } catch (Exception e) {
            log.warn("Could not remove id_usuario_responsable: {}", e.getMessage());
        }

        migrateApplicantRelation();

        // 3. Backfill partial responsible fields from nombre_responsable (legacy data)
        backfillResponsibleFromCompound();

        // 4. Backfill solicitante.nombres from tramite_fut (legacy data)
        backfillApplicantNames();
    }

    private void migrateApplicantRelation() {
        try {
        Integer tableCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES " +
            "WHERE TABLE_SCHEMA = 'dbo' AND TABLE_NAME = 'gestion_tramite_fut'",
            Integer.class);
        if (tableCount == null || tableCount == 0) return;

        ensureColumn("nombres_responsable", "NVARCHAR(100) NULL");
        ensureColumn("apellido_paterno_responsable", "NVARCHAR(100) NULL");
        ensureColumn("apellido_materno_responsable", "NVARCHAR(100) NULL");

        if (!columnExists("id_solicitante")) {
            jdbc.execute(
                "ALTER TABLE dbo.gestion_tramite_fut " +
                "ADD id_solicitante BIGINT NULL");
            log.info("Column id_solicitante added to gestion_tramite_fut.");
        } else {
            // Ensure it is BIGINT (may have been created as INT before)
            try {
                // Remove FK if it exists before changing the type
                List<String> fks = jdbc.queryForList(
                    "SELECT fk.name FROM sys.foreign_keys fk " +
                    "INNER JOIN sys.foreign_key_columns fkc " +
                    "ON fk.object_id = fkc.constraint_object_id " +
                    "WHERE fkc.parent_object_id = OBJECT_ID('dbo.gestion_tramite_fut') " +
                    "AND COL_NAME(fkc.parent_object_id, fkc.parent_column_id) = 'id_solicitante'",
                    String.class);
                for (String fk : fks) {
                    jdbc.execute("ALTER TABLE dbo.gestion_tramite_fut DROP CONSTRAINT [" + fk + "]");
                    log.info("FK {} removed to change id_solicitante type.", fk);
                }
                jdbc.execute(
                    "ALTER TABLE dbo.gestion_tramite_fut " +
                    "ALTER COLUMN id_solicitante BIGINT NULL");
                log.info("Column id_solicitante converted to BIGINT.");
            } catch (Exception e) {
                log.warn("Could not convert id_solicitante to BIGINT: {}", e.getMessage());
            }
        }

        if (columnExists("id_solicitud")
                && tableExists("tramite_fut")
                && columnExists("id_solicitud", "tramite_fut")
                && columnExists("id_solicitante", "tramite_fut")) {
            int migrated = jdbc.update(
                "UPDATE gestion SET id_solicitante = tramite.id_solicitante " +
                "FROM dbo.gestion_tramite_fut gestion " +
                "INNER JOIN dbo.tramite_fut tramite " +
                "ON tramite.id_solicitud = gestion.id_solicitud " +
                "WHERE gestion.id_solicitante IS NULL " +
                "AND tramite.id_solicitante IS NOT NULL");
            if (migrated > 0) {
                log.info("{} management records linked to the existing applicant master.", migrated);
            }
        }

        if (columnExists("id_solicitud")) {
            List<String> legacyForeignKeys = jdbc.queryForList(
                "SELECT fk.name FROM sys.foreign_keys fk " +
                "INNER JOIN sys.foreign_key_columns fkc " +
                "ON fk.object_id = fkc.constraint_object_id " +
                "WHERE fk.parent_object_id = OBJECT_ID('dbo.gestion_tramite_fut') " +
                "AND COL_NAME(fkc.parent_object_id, fkc.parent_column_id) = 'id_solicitud'",
                String.class);
            for (String foreignKey : legacyForeignKeys) {
                jdbc.execute("ALTER TABLE dbo.gestion_tramite_fut DROP CONSTRAINT ["
                    + foreignKey.replace("]", "]]") + "]");
            }
            jdbc.execute(
                "ALTER TABLE dbo.gestion_tramite_fut " +
                "ALTER COLUMN id_solicitud BIGINT NULL");
        }

        Integer relationCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM sys.foreign_key_columns fkc " +
            "INNER JOIN sys.foreign_keys fk ON fk.object_id = fkc.constraint_object_id " +
            "WHERE fkc.parent_object_id = OBJECT_ID('dbo.gestion_tramite_fut') " +
            "AND COL_NAME(fkc.parent_object_id, fkc.parent_column_id) = 'id_solicitante' " +
            "AND fkc.referenced_object_id = OBJECT_ID('dbo.solicitante')",
            Integer.class);
        if (relationCount == null || relationCount == 0) {
            jdbc.execute(
                "ALTER TABLE dbo.gestion_tramite_fut WITH CHECK " +
                "ADD CONSTRAINT FK_gestion_tramite_fut_solicitante " +
                "FOREIGN KEY (id_solicitante) " +
                "REFERENCES dbo.solicitante(id_solicitante)");
            log.info("Relationship gestion_tramite_fut → solicitante verified.");
        }
        } catch (Exception e) {
            log.warn("Could not migrate applicant relation in gestion_tramite_fut: {}", e.getMessage());
        }
    }

    /**
     * For rows where the partial responsible fields are NULL but
     * nombre_responsable (legacy compound field) has a value, attempts to split
     * the compound into: names | paternal last name | maternal last name using
     * the heuristic "last part = maternal, second-to-last = paternal, rest = names".
     * This covers the most common case for Peruvian names.
     */
    private void backfillResponsibleFromCompound() {
        try {
            if (!columnExists("nombre_responsable") || !columnExists("nombres_responsable")) return;

            List<java.util.Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id_gestion, nombre_responsable " +
                "FROM dbo.gestion_tramite_fut " +
                "WHERE nombre_responsable IS NOT NULL " +
                "  AND LTRIM(RTRIM(nombre_responsable)) <> '' " +
                "  AND (nombres_responsable IS NULL OR LTRIM(RTRIM(nombres_responsable)) = '') " +
                "  AND (apellido_paterno_responsable IS NULL OR LTRIM(RTRIM(apellido_paterno_responsable)) = '') " +
                "  AND (apellido_materno_responsable IS NULL OR LTRIM(RTRIM(apellido_materno_responsable)) = '')");

            if (rows.isEmpty()) {
                log.debug("No gestion_tramite_fut rows require responsible backfill.");
                return;
            }

            int updated = 0;
            for (java.util.Map<String, Object> row : rows) {
                Integer idGestion = (Integer) row.get("id_gestion");
                String name = ((String) row.get("nombre_responsable")).trim();
                String[] parts = name.split("\\s+");
                String names, paternal, maternal;
                if (parts.length >= 3) {
                    maternal  = parts[parts.length - 1];
                    paternal  = parts[parts.length - 2];
                    names     = String.join(" ", java.util.Arrays.copyOfRange(parts, 0, parts.length - 2));
                } else if (parts.length == 2) {
                    names     = parts[0];
                    paternal  = parts[1];
                    maternal  = parts[1]; // fallback: duplicate last name
                } else {
                    names     = name;
                    paternal  = name;
                    maternal  = name;
                }
                jdbc.update(
                    "UPDATE dbo.gestion_tramite_fut " +
                    "SET nombres_responsable = ?, apellido_paterno_responsable = ?, apellido_materno_responsable = ? " +
                    "WHERE id_gestion = ?",
                    names, paternal, maternal, idGestion);
                updated++;
            }
            if (updated > 0) {
                log.info("Responsible backfill: {} gestion_tramite_fut row(s) updated from nombre_responsable.", updated);
            }
        } catch (Exception e) {
            log.warn("Could not backfill responsible partial fields: {}", e.getMessage());
        }
    }

    /**
     * For rows in dbo.solicitante where nombres is NULL, attempts to recover
     * the value from dbo.tramite_fut.nombre_completo via id_solicitante.
     * Applies the same split logic as SolicitudServiceImpl.splitName:
     * last token = maternal last name, second-to-last = paternal last name, rest = names.
     */
    private void backfillApplicantNames() {
        try {
            if (!tableExists("tramite_fut")) {
                log.debug("Table tramite_fut does not exist; skipping applicant names backfill.");
                return;
            }
            if (!columnExists("nombre_completo", "tramite_fut")) {
                log.debug("tramite_fut does not have nombre_completo column; skipping backfill.");
                return;
            }

            // Fetch applicants with NULL nombres whose tramite has nombre_completo
            List<java.util.Map<String, Object>> rows = jdbc.queryForList(
                "SELECT s.id_solicitante, t.nombre_completo " +
                "FROM dbo.solicitante s " +
                "INNER JOIN dbo.tramite_fut t ON t.id_solicitante = s.id_solicitante " +
                "WHERE (s.nombres IS NULL OR LTRIM(RTRIM(s.nombres)) = '') " +
                "  AND t.nombre_completo IS NOT NULL " +
                "  AND LTRIM(RTRIM(t.nombre_completo)) <> '' " +
                "ORDER BY t.fecha_registro DESC");

            if (rows.isEmpty()) {
                log.debug("No solicitante rows require applicant names backfill.");
                return;
            }

            // Deduplicate: keep only first (most recent) per id_solicitante
            java.util.Map<Long, String> byId = new java.util.LinkedHashMap<>();
            for (java.util.Map<String, Object> row : rows) {
                Long idSol = ((Number) row.get("id_solicitante")).longValue();
                byId.putIfAbsent(idSol, (String) row.get("nombre_completo"));
            }

            int updated = 0;
            for (java.util.Map.Entry<Long, String> entry : byId.entrySet()) {
                Long idSolicitante = entry.getKey();
                String fullName = entry.getValue().trim();
                String[] parts = fullName.split("\\s+");
                if (parts.length < 2) continue; // not enough tokens to split
                String maternal = parts[parts.length - 1];
                String paternal = parts[parts.length - 2];
                String names    = parts.length > 2
                        ? String.join(" ", java.util.Arrays.copyOfRange(parts, 0, parts.length - 2))
                        : parts[0];
                jdbc.update(
                    "UPDATE dbo.solicitante " +
                    "SET nombres = ?, apellido_paterno = ?, apellido_materno = ? " +
                    "WHERE id_solicitante = ? " +
                    "  AND (nombres IS NULL OR LTRIM(RTRIM(nombres)) = '')",
                    names, paternal, maternal, idSolicitante);
                updated++;
            }
            if (updated > 0) {
                log.info("Applicant names backfill: {} solicitante row(s) updated from tramite_fut.nombre_completo.", updated);
            }
        } catch (Exception e) {
            log.warn("Could not backfill solicitante.nombres from tramite_fut: {}", e.getMessage());
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES " +
            "WHERE TABLE_SCHEMA = 'dbo' AND TABLE_NAME = ?",
            Integer.class,
            tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(String columnName) {
        return columnExists(columnName, "gestion_tramite_fut");
    }

    private boolean columnExists(String columnName, String tableName) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
            "WHERE TABLE_SCHEMA = 'dbo' AND TABLE_NAME = ? AND COLUMN_NAME = ?",
            Integer.class,
            tableName,
            columnName);
        return count != null && count > 0;
    }

    private void ensureColumn(String columnName, String sqlType) {
        if (!columnExists(columnName)) {
            jdbc.execute("ALTER TABLE dbo.gestion_tramite_fut ADD "
                + columnName + " " + sqlType);
            log.info("Column {} added to gestion_tramite_fut.", columnName);
        }
    }
}
