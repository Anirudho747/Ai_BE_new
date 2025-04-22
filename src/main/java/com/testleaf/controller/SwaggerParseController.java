package com.testleaf.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.testleaf.parser.SwaggerParser;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000") 
public class SwaggerParseController {

    private final SwaggerParser swaggerParser = new SwaggerParser();

    /**
     * Endpoint to accept a Swagger (YAML/JSON) file and parse it.
     */
    @PostMapping(value = "/parseSwagger", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> parseSwaggerFile(@RequestParam("file") MultipartFile file) {
        try {
            // 1. Save the uploaded file temporarily
            String tempFileName = System.getProperty("java.io.tmpdir") 
                                  + File.separator 
                                  + UUID.randomUUID() + "_" + file.getOriginalFilename();
            
            Files.write(Paths.get(tempFileName), file.getBytes());

            // 2. Parse using your existing SwaggerParser
            String apiDetails = swaggerParser.parseSwagger(tempFileName);

            // 3. Check if parse was successful
            if (apiDetails == null || apiDetails.isEmpty()) {
                return ResponseEntity.badRequest()
                                     .body("Failed to parse the Swagger file. Check if it is a valid specification.");
            }
            
            // 4. Return the extracted API details
            return ResponseEntity.ok(apiDetails);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error parsing file: " + e.getMessage());
        }
    }
}
