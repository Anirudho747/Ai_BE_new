package com.testleaf.controller;

import com.testleaf.llm.LLMTestGenerator;
import com.testleaf.llm.TestCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TestGenerationController {

    private final LLMTestGenerator llmTestGenerator;
    private final TestCodeGenerator testCodeGenerator;

    public TestGenerationController(LLMTestGenerator llmTestGenerator, TestCodeGenerator testCodeGenerator) {
        this.llmTestGenerator = llmTestGenerator;
        this.testCodeGenerator = testCodeGenerator;
    }

    /**
     * Generates Rest-Assured test code from the provided API details and test types.
     *
     * Example usage:
     *  POST /api/generateTests
     *  Body (raw JSON):
     *  {
     *    "apiDetails": "Path: /pet, Method: PUT, Summary: Update an existing pet\nPath: ...",
     *    "testTypes": ["positive", "negative"]
     *  }
     */
    @PostMapping("/generateTests")
    public ResponseEntity<String> generateTests(@RequestBody ApiDetailsRequest request) {
        try {
            // Generate code using the LLM, passing both apiDetails and testTypes
            String llmResponse = llmTestGenerator.generateTestCases(
                    request.getApiDetails(),
                    request.getTestTypes()
            );

            // Extract final Java code
            String finalCode = testCodeGenerator.extractJavaCode(llmResponse);

            // Return the code as plain text
            return ResponseEntity.ok(finalCode);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error generating test code: " + e.getMessage());
        }
    }

    // Updated DTO with field "testTypes" (plural) to match the React payload.
    public static class ApiDetailsRequest {
        private String apiDetails;
        private List<String> testTypes;  // Updated field name

        public String getApiDetails() {
            return apiDetails;
        }
        public void setApiDetails(String apiDetails) {
            this.apiDetails = apiDetails;
        }

        public List<String> getTestTypes() {
            return testTypes;
        }
        public void setTestTypes(List<String> testTypes) {
            this.testTypes = testTypes;
        }
    }
}
