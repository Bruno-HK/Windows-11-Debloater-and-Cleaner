package debloater.model;

/**
 * Categories for grouping debloat actions in the UI.
 * Order matches the display order in the sidebar.
 */
public enum ActionCategory {
    SAFETY("Safety", 0),
    BLOAT_APPS("Bloat Apps", 1),
    WIDGETS("Widgets", 2),
    COPILOT_AI_APPS("Copilot & AI Apps", 3),
    AI_FEATURES_REGISTRY("AI Features (Registry)", 4),
    ONEDRIVE("OneDrive", 5),
    ADS_SPONSORED("Ads & Sponsored Content", 6),
    PRIVACY_TELEMETRY("Privacy & Telemetry", 7),
    SERVICES("Services", 8),
    SCHEDULED_TASKS("Scheduled Tasks", 9);

    private final String label;
    private final int order;

    ActionCategory(String label, int order) {
        this.label = label;
        this.order = order;
    }

    public String getLabel() {
        return label;
    }

    public int getOrder() {
        return order;
    }
}
