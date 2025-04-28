package com.testleaf.controller;

import com.testleaf.llm.LLMDesignTestGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/design")
@CrossOrigin(origins = "*")
public class DesignTestController {

    private final LLMDesignTestGenerator testGenerator;

    @Autowired
    public DesignTestController(LLMDesignTestGenerator testGenerator) {
        this.testGenerator = testGenerator;
    }

    // DTO for incoming request
    public static class DesignTestRequest {
        public String description;
        public String figmaUrl;
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateTests(@RequestBody DesignTestRequest request) {
        try {
            String prompt = testGenerator.buildPromptFromDescription(request.description, request.figmaUrl);
            String testCases = testGenerator.callLLMToGenerateTestCases(prompt);
            return ResponseEntity.ok(testCases);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating test cases: " + e.getMessage());
        }
    }
}
