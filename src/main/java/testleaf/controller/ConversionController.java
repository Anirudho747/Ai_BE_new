package testleaf.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import testleaf.llm.LLMConverterService;
import testleaf.llm.TestCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/convert")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ConversionController {

    private final LLMConverterService converterService;
    private final TestCodeGenerator testCodeGenerator;

    public ConversionController(LLMConverterService converterService, TestCodeGenerator testCodeGenerator) {
        this.converterService = converterService;
        this.testCodeGenerator = testCodeGenerator;
    }


    // Endpoint to convert Selenium Java code to Playwright TypeScript code
    @PostMapping("/seleniumToPlaywright")
    public ResponseEntity<String> convertSeleniumToPlaywright(@RequestBody Map<String, String> payload) {
        try {
            String seleniumCode = payload.get("seleniumCode");
            String llmApiKey = payload.get("llmApiKey");
            String llmApiUrl = payload.get("llmApiUrl");
            String llmModel = payload.get("llmModel");

            if (seleniumCode == null || seleniumCode.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Missing seleniumCode");
            }

            if (llmApiKey == null || llmApiUrl == null || llmModel == null) {
                return ResponseEntity.badRequest().body("Missing LLM configuration.");
            }

            String result = converterService.convertSeleniumToPlaywright(seleniumCode, llmApiKey, llmApiUrl, llmModel);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error during conversion: " + e.getMessage());
        }
    }

    // Stub endpoint to compile Selenium code (dummy implementation)
    @PostMapping("/compileSelenium")
    public ResponseEntity<String> compileSelenium(@RequestBody CodeRequest request) {
        // Implement dynamic compilation logic if needed.
        // For now, we assume the code compiles.
        return ResponseEntity.ok("Selenium code compiled successfully.");
    }

    // Stub endpoint to run Playwright code (dummy implementation)
    @PostMapping("/runPlaywright")
    public ResponseEntity<String> runPlaywright(@RequestBody CodeRequest request) {
        // Implement execution logic using Node.js if desired.
        // For now, we return a dummy success message.
        return ResponseEntity.ok("Playwright code executed successfully.");
    }

    @PostMapping("/runPlaywrightProxy")
    public ResponseEntity<String> runPlaywrightProxy(@RequestBody CodeRequest request) {
        try {
            // Create HTTP POST request to external Playwright API
            HttpPost post = new HttpPost("https://try.playwright.tech/service/control/run");
            post.setHeader("Content-Type", "application/json");

            // Build payload for the external API
            ObjectMapper mapper = new ObjectMapper();
            String payload = mapper.writeValueAsString(request);
            post.setEntity(new StringEntity(payload));

            // Execute the request
            try (CloseableHttpResponse externalResponse = HttpClients.createDefault().execute(post)) {
                String responseBody = EntityUtils.toString(externalResponse.getEntity());
                return ResponseEntity.ok(responseBody);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error pushing code: " + e.getMessage());
        }
    }


    public static class ConversionRequest {
        private String seleniumCode;

        public String getSeleniumCode() {
            return seleniumCode;
        }
        public void setSeleniumCode(String seleniumCode) {
            this.seleniumCode = seleniumCode;
        }
    }

    public static class CodeRequest {
        private String code;
        private String language;

        // getters and setters

        public String getCode() {
            return code;
        }
        public void setCode(String code) {
            this.code = code;
        }
        public String getLanguage() {
            return language;
        }
        public void setLanguage(String language) {
            this.language = language;
        }
    }

}
