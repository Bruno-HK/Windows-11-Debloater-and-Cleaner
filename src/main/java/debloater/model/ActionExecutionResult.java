package debloater.model;

/**
 * Represents the structured result of executing a single debloat action.
 * Parsed from JSON lines emitted by the generated PowerShell script.
 * Each selected action emits exactly one result.
 */
public class ActionExecutionResult {

    private String id;
    private String title;
    private String category;
    private String riskLevel;
    private String status;      // Success, Failed, Skipped, Partially completed, Already applied
    private String message;     // Human-readable summary
    private String details;     // Technical details
    private String error;       // Error message if failed, null otherwise
    private boolean restartRecommended;
    private String timestamp;

    public ActionExecutionResult() {
        // Default constructor for Gson deserialization
    }

    public ActionExecutionResult(String id, String title, String category,
                                 String riskLevel, String status, String message,
                                 String details, String error,
                                 boolean restartRecommended, String timestamp) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.riskLevel = riskLevel;
        this.status = status;
        this.message = message;
        this.details = details;
        this.error = error;
        this.restartRecommended = restartRecommended;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public boolean isRestartRecommended() { return restartRecommended; }
    public void setRestartRecommended(boolean restartRecommended) { this.restartRecommended = restartRecommended; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
