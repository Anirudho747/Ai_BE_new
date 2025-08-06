package testleaf.llm;

import java.util.List;
import java.util.Map;

public class LLMFlakyTestResponse {

    private List<LLMFlakyTestResult> flakyTests;  // renamed from topFlakyTests
    private Map<String, String> llmSuggestions;

    public LLMFlakyTestResponse() {}

    public LLMFlakyTestResponse(List<LLMFlakyTestResult> flakyTests) {
        this.flakyTests = flakyTests;
    }

    public LLMFlakyTestResponse(List<LLMFlakyTestResult> flakyTests, Map<String, String> llmSuggestions) {
        this.flakyTests = flakyTests;
        this.llmSuggestions = llmSuggestions;
    }

    public List<LLMFlakyTestResult> getFlakyTests() {
        return flakyTests;
    }

    public void setFlakyTests(List<LLMFlakyTestResult> flakyTests) {
        this.flakyTests = flakyTests;
    }

    public Map<String, String> getLlmSuggestions() {
        return llmSuggestions;
    }

    public void setLlmSuggestions(Map<String, String> llmSuggestions) {
        this.llmSuggestions = llmSuggestions;
    }
}

