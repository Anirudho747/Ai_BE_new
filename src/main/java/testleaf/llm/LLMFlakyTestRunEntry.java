package testleaf.llm;

import java.time.LocalDateTime;

public class LLMFlakyTestRunEntry {

    private String testName;
    private String status; // e.g., PASSED, FAILED
    private long duration;
    private String errorMsg;
    private LocalDateTime executedAt;

    public LLMFlakyTestRunEntry() {
    }

    public LLMFlakyTestRunEntry(String testName, String status, long duration, String errorMsg, LocalDateTime executedAt) {
        this.testName = testName;
        this.status = status;
        this.duration = duration;
        this.errorMsg = errorMsg;
        this.executedAt = executedAt;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }
}
