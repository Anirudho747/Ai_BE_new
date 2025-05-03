package testleaf.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import testleaf.llm.LLMTestCaseGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TestCaseGenerationController {

    private final LLMTestCaseGenerator llmTestCaseGenerator;

    public TestCaseGenerationController(LLMTestCaseGenerator llmTestCaseGenerator) {
        this.llmTestCaseGenerator = llmTestCaseGenerator;
    }

    /**
     * Generates manual test cases from the provided details.
     * <p>
     * Example usage:
     * POST /api/generateTestCases
     * Body (raw JSON):
     * {
     * "testType": {
     *   "positive": true,
     *   "negative": false,
     *   "edge": false
     * },
     * "userStoryDescription": "As a user, I want to log in to Salesforce so that I can access my account and manage leads.",
     * "applicationUrl": "https://login.salesforce.com",
     * "acceptanceCriteria": "1. User should be able to log in with valid credentials. 2. Invalid credentials should show an error message. 3. Login page should be accessible.",
     * "epicDescription": ""
     * }
     */
    @PostMapping("/generateTestCases")
    public ResponseEntity<String> generateTestCases(@RequestBody TestCaseDetailsRequest request) {
        try {
            // Convert TestType to a comma-separated string
            String testTypeString = convertTestTypeToString(request.getTestType());

            // Generate manual test cases using the LLM
            String llmResponse = llmTestCaseGenerator.llmGenerateTestCases(
                    testTypeString,
                    request.getUserStoryDescription(),
                    request.getApplicationUrl(),
                    request.getAcceptanceCriteria(),
                    request.getEpicDescription(),
                    request.getAdditionalInstructions()
            );

            // Return the manual test cases as plain text
            return ResponseEntity.ok(extractTestCases(llmResponse));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error generating test cases: " + e.getMessage());
        }
    }

    public String extractTestCases(String llmResponse) {
        String content = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(llmResponse);
            JsonNode choicesNode = rootNode.path("choices");
            if (choicesNode.isArray() && choicesNode.size() > 0) {
                JsonNode messageNode = choicesNode.get(0).path("message");
                content = messageNode.path("content").asText().trim();
                if (content.contains("```json")) {
                    int start = content.indexOf("```json");
                    int end = content.lastIndexOf("```");
                    if (start != -1 && end != -1 && end > start) {
                        content = content.substring(start + "```json".length(), end).trim();
                    }
                } else if (content.contains("package")) {
                    int index = content.indexOf("package");
                    content = content.substring(index).trim();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return content;
    }

    // DTO to capture the test case details from the UI
    public static class TestCaseDetailsRequest {
        private TestType testType; // Changed to TestType object
        private String userStoryDescription;
        private String applicationUrl;
        private String acceptanceCriteria;
        private String epicDescription;
        private String additionalInstructions;

        public TestType getTestType() {
            return testType;
        }

        public void setTestType(TestType testType) {
            this.testType = testType;
        }

        public String getUserStoryDescription() {
            return userStoryDescription;
        }

        public void setUserStoryDescription(String userStoryDescription) {
            this.userStoryDescription = userStoryDescription;
        }

        public String getApplicationUrl() {
            return applicationUrl;
        }

        public void setApplicationUrl(String applicationUrl) {
            this.applicationUrl = applicationUrl;
        }

        public String getAcceptanceCriteria() {
            return acceptanceCriteria;
        }

        public void setAcceptanceCriteria(String acceptanceCriteria) {
            this.acceptanceCriteria = acceptanceCriteria;
        }

        public String getEpicDescription() {
            return epicDescription;
        }

        public void setEpicDescription(String epicDescription) {
            this.epicDescription = epicDescription;
        }

        public String getAdditionalInstructions() {
            return additionalInstructions;
        }

        public void setAdditionalInstructions(String additionalInstructions) {
            this.additionalInstructions = additionalInstructions;
        }
    }

    // New DTO for testType
    public static class TestType {
        private boolean positive;
        private boolean negative;
        private boolean edge;

        public boolean isPositive() {
            return positive;
        }

        public void setPositive(boolean positive) {
            this.positive = positive;
        }

        public boolean isNegative() {
            return negative;
        }

        public void setNegative(boolean negative) {
            this.negative = negative;
        }

        public boolean isEdge() {
            return edge;
        }

        public void setEdge(boolean edge) {
            this.edge = edge;
        }
    }

    // Method to convert TestType object to a comma-separated string
    private String convertTestTypeToString(TestType testType) {
        if (testType == null || (!testType.isPositive() && !testType.isNegative() && !testType.isEdge())) {
            return "Positive"; // Default to "Positive" if all are false or if testType is null
        }
        StringBuilder testTypeString = new StringBuilder();
        if (testType.isPositive()) {
            testTypeString.append("Positive");
        }
        if (testType.isNegative()) {
            if (testTypeString.length() > 0) testTypeString.append(", ");
            testTypeString.append("Negative");
        }
        if (testType.isEdge()) {
            if (testTypeString.length() > 0) testTypeString.append(", ");
            testTypeString.append("Edge");
        }
        return testTypeString.toString();
    }
}