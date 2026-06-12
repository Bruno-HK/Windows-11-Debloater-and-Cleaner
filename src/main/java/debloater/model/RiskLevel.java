package debloater.model;

/**
 * Risk level for each debloat action.
 * Used to warn users and control default selection state.
 */
public enum RiskLevel {
    SAFE("Safe"),
    MODERATE("Moderate"),
    AGGRESSIVE("Aggressive");

    private final String label;

    RiskLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
