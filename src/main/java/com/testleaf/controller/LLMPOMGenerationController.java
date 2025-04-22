package com.testleaf.controller;

import com.testleaf.llm.LLMPOMGenerator;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/pom")
public class LLMPOMGenerationController {

    private final LLMPOMGenerator pomGenerator;

    @Autowired
    public LLMPOMGenerationController(LLMPOMGenerator pomGenerator) {
        this.pomGenerator = pomGenerator;
    }

    public static class POMRequest {
        public String xmlContent;
        public String platform; // ANDROID, IOS, CROSS_PLATFORM, DYNAMIC_RUNTIME
        public String className;
        public String packageName;
        public String baseClassName;
    }

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generatePOM(@RequestBody POMRequest request) {
        try {
            String pomClass;

            switch (request.platform.toUpperCase()) {
                case "CROSS_PLATFORM":
                    pomClass = pomGenerator.generateCrossPlatformPOM(
                            request.xmlContent,
                            request.className,
                            request.packageName,
                            request.baseClassName
                    );
                    break;
                case "DYNAMIC_RUNTIME":
                    pomClass = pomGenerator.generateDynamicPOM(
                            request.xmlContent,
                            request.className,
                            request.packageName,
                            request.baseClassName
                    );
                    break;
                default:
                    pomClass = pomGenerator.generateMobilePOM(
                            request.xmlContent,
                            request.platform,
                            request.className,
                            request.packageName
                    );
            }

            byte[] pomBytes = pomClass.getBytes(StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + request.className + ".java");
            headers.add(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8");

            return new ResponseEntity<>(pomBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error generating POM: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }
}

