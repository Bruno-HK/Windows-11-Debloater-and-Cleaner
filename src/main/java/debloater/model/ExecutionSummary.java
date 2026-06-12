package debloater.model;

/**
 * Aggregate summary of all action execution results.
 * Shown in the final report after execution completes.
 */
public class ExecutionSummary {

    private int total;
    private int successCount;
    private int failedCount;
    private int skippedCount;
    private int partialCount;
    private int alreadyAppliedCount;
    private boolean restartRecommended;

    public ExecutionSummary() {}

    public ExecutionSummary(int total, int successCount, int failedCount,
                            int skippedCount, int partialCount,
                            int alreadyAppliedCount, boolean restartRecommended) {
        this.total = total;
        this.successCount = successCount;
        this.failedCount = failedCount;
        this.skippedCount = skippedCount;
        this.partialCount = partialCount;
        this.alreadyAppliedCount = alreadyAppliedCount;
        this.restartRecommended = restartRecommended;
    }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }

    public int getSkippedCount() { return skippedCount; }
    public void setSkippedCount(int skippedCount) { this.skippedCount = skippedCount; }

    public int getPartialCount() { return partialCount; }
    public void setPartialCount(int partialCount) { this.partialCount = partialCount; }

    public int getAlreadyAppliedCount() { return alreadyAppliedCount; }
    public void setAlreadyAppliedCount(int alreadyAppliedCount) { this.alreadyAppliedCount = alreadyAppliedCount; }

    public boolean isRestartRecommended() { return restartRecommended; }
    public void setRestartRecommended(boolean restartRecommended) { this.restartRecommended = restartRecommended; }
}
