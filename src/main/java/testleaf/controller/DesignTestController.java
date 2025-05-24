package testleaf.controller;

import testleaf.llm.LLMDesignTestGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.Map;

@RestController
@RequestMapping("/api/design")
@CrossOrigin(origins = "*")
public class DesignTestController {

    private final LLMDesignTestGenerator testGenerator;

    @Autowired
    public DesignTestController(LLMDesignTestGenerator testGenerator) {
        this.testGenerator = testGenerator;
    }

    public static class DesignTestRequest {
        public String description;
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateTests(@RequestBody DesignTestRequest request) {
        try {
            String bddPrompt = testGenerator.buildPromptFromDescription(request.description, "BDD");
            String tddPrompt = testGenerator.buildPromptFromDescription(request.description, "TDD");

            String bddOutput = testGenerator.callLLMToGenerateTestCases(bddPrompt);
            String tddOutput = testGenerator.callLLMToGenerateTestCases(tddPrompt);

            return ResponseEntity.ok(
                    Map.of("bdd", bddOutput, "tdd", tddOutput)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error generating test cases: " + e.getMessage()));
        }
    }
}
