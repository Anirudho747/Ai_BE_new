package testleaf.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import testleaf.llm.LLMDesignTestGenerator;
import java.util.HashMap;
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
        public String llmApiUrl;
        public String llmApiKey;
        public String llmModel;
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateTests(@RequestBody DesignTestRequest request) {
        try {
            String prompt = testGenerator.buildPromptFromDescription(request.description);

            Map<String, String> result = testGenerator.callLLMToGenerateTestCases(
                    prompt,
                    request.llmApiUrl,
                    request.llmApiKey,
                    request.llmModel
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("bdd", "");
            error.put("tdd", "");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

