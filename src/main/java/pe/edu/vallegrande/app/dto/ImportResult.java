package pe.edu.vallegrande.app.dto;

import java.util.List;

/**
 * DTO that summarises the result of a bulk import operation (Excel or CSV).
 * Returns the total rows read, how many were saved and how many were rejected,
 * plus a per-row detail of every rejection so the caller can fix the file.
 */
public class ImportResult {

    private int totalProcessed;
    private int totalImported;
    private int totalRejected;
    private List<RowError> errors;

    public ImportResult(int totalProcessed, int totalImported, int totalRejected, List<RowError> errors) {
        this.totalProcessed = totalProcessed;
        this.totalImported  = totalImported;
        this.totalRejected  = totalRejected;
        this.errors         = errors;
    }

    public int getTotalProcessed() { return totalProcessed; }
    public int getTotalImported()  { return totalImported; }
    public int getTotalRejected()  { return totalRejected; }
    public List<RowError> getErrors() { return errors; }

    // ----------------------------------------------------------------
    // Inner class: detail of a single rejected row
    // ----------------------------------------------------------------
    public static class RowError {

        private int row;          // 1-based row number inside the file
        private String reason;    // human-readable rejection reason

        public RowError(int row, String reason) {
            this.row    = row;
            this.reason = reason;
        }

        public int getRow()      { return row; }
        public String getReason() { return reason; }
    }
}
