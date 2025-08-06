package testleaf.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import testleaf.llm.LLMFlakyAnalyzer;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import testleaf.llm.LLMFlakyTestResponse;
import java.util.logging.Logger;
import java.io.InputStream;

@RestController
@RequestMapping("/flaky")
@CrossOrigin(origins = "*")
public class LLMFlakyAnalyzerController {

    private static final Logger LOGGER = Logger.getLogger(LLMFlakyAnalyzerController.class.getName());

    @Autowired
    private LLMFlakyAnalyzer analyzer;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeFlakyTests(
            @RequestParam("file") MultipartFile file,
            @RequestParam("format") String format,
            @RequestParam("threshold") int threshold,
            @RequestParam("llmApiUrl") String llmApiUrl,
            @RequestParam("llmApiKey") String llmApiKey,
            @RequestParam("llmModel") String llmModel
    ) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Uploaded file is empty.");
            }

            LOGGER.info("Received file: " + file.getOriginalFilename());
            InputStream input = file.getInputStream();

            LLMFlakyTestResponse result = analyzer.analyze(input, format, threshold, llmApiUrl, llmApiKey, llmModel);
            LOGGER.info("Analysis completed. Returning result.");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            LOGGER.severe("Error during flaky test analysis: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to analyze test file: " + e.getMessage());
        }
    }

    @GetMapping("/sample/csv")
    public ResponseEntity<InputStreamResource> downloadSampleCsv() {
        try {
            ClassPathResource resource = new ClassPathResource("static/templates/sample_flaky.csv");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sample_flaky.csv\"")
                    .body(new InputStreamResource(resource.getInputStream()));

        } catch (Exception e) {
            LOGGER.severe("Failed to load sample CSV: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/sample/json")
    public ResponseEntity<InputStreamResource> downloadSampleJson() {
        try {
            ClassPathResource resource = new ClassPathResource("static/templates/sample_flaky.json");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sample_flaky.json\"")
                    .body(new InputStreamResource(resource.getInputStream()));

        } catch (Exception e) {
            LOGGER.severe("Failed to load sample JSON: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
