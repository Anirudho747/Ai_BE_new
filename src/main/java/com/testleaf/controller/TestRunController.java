package com.testleaf.controller;

import com.testleaf.runner.TestExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TestRunController {

    private final TestExecutor testExecutor;

    public TestRunController(TestExecutor testExecutor) {
        this.testExecutor = testExecutor;
    }

    @PostMapping("/runTests")
    public ResponseEntity<?> runTests(@RequestBody TestRunRequest request) {
        try {
            // If you already know the class name, pass it directly
            // Otherwise, parse it from the "public class XYZ" in the code
            String className = extractClassName(request.getJavaCode());
            if (className == null) {
                return ResponseEntity.badRequest()
                        .body("Could not find class name in the code.");
            }

            // Call the executor service
            var result = testExecutor.runTests(request.getJavaCode(), className);

            // Return the results as JSON
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error running tests: " + e.getMessage());
        }
    }

    private String extractClassName(String javaCode) {
        // A simple pattern to find: public class SomeClassName
        String regex = "public\\s+class\\s+(\\w+)";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(javaCode);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static class TestRunRequest {
        private String javaCode;
        public String getJavaCode() { return javaCode; }
        public void setJavaCode(String javaCode) { this.javaCode = javaCode; }
    }
}
