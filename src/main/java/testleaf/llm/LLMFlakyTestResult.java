package testleaf.llm;

import java.time.LocalDateTime;
import java.util.List;

public class LLMFlakyTestResult {

    private String testName;
    private String status;
    private long duration;
    private String errorMsg;
    private LocalDateTime executedAt;
    private double failRate;
    private String suggestion;
    private List<LLMFlakyTestRunEntry> runs;

    // ✅ Newly added fields
    private int failCount;
    private int totalRuns;

    // Constructors
    public LLMFlakyTestResult() {}

    public LLMFlakyTestResult(String testName, String status, long duration, String errorMsg, LocalDateTime executedAt) {
        this.testName = testName;
        this.status = status;
        this.duration = duration;
        this.errorMsg = errorMsg;
        this.executedAt = executedAt;
    }

    // Getters and Setters
    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getStatus() {
        return status;
    }

    public long getDuration() {
        return duration;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public double getFailRate() {
        return failRate;
    }

    public void setFailRate(double failRate) {
        this.failRate = failRate;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getErrorMessage() {
        return errorMsg;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMsg = errorMessage;
    }

    public void setRuns(List<LLMFlakyTestRunEntry> runs) {
        this.runs = runs;
    }

    public List<LLMFlakyTestRunEntry> getRuns() {
        return runs;
    }

    // ✅ New fields
    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    public int getTotalRuns() {
        return totalRuns;
    }

    public void setTotalRuns(int totalRuns) {
        this.totalRuns = totalRuns;
    }
}
