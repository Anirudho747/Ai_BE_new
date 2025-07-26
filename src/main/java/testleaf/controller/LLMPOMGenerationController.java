package testleaf.controller;

import testleaf.llm.LLMPOMGenerator;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/pom")
public class LLMPOMGenerationController {

    @Autowired
    private LLMPOMGenerator pomGenerator;

    @PostMapping("/generate")
    public ResponseEntity<String> generatePOM(@RequestBody Map<String, String> payload) {
        try {
            String xml = payload.get("xmlContent");
            String platform = payload.get("platform");
            String className = payload.get("className");
            String packageName = payload.get("packageName");
            String baseClassName = payload.getOrDefault("baseClassName", "");
            String mode = platform.toUpperCase();

            // Inject user-supplied LLM credentials (if present)
            String llmApiUrl = payload.getOrDefault("llmApiUrl", "");
            String llmApiKey = payload.getOrDefault("llmApiKey", "");
            String llmModel = payload.getOrDefault("llmModel", "");

            String result = pomGenerator.generatePOMWithFallback(
                    xml, platform, className, packageName, baseClassName, mode,
                    llmApiUrl, llmApiKey, llmModel
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error generating POM: " + e.getMessage());
        }
    }
}


