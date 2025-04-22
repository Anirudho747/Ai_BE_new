package com.testleaf.parser;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.yaml.snakeyaml.Yaml;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class SwaggerParser {

    public String parseSwagger(String swaggerFilePath) {
        try {
            // Read file content
            Path path = Paths.get(swaggerFilePath);
            String fileContent = new String(Files.readAllBytes(path));

            // Check if the content is YAML (not JSON)
            if (!fileContent.trim().startsWith("{")) {
                fileContent = convertYamlToJson(fileContent);
            }

            // Parse using swagger-parser
            SwaggerParseResult result = new OpenAPIParser().readContents(fileContent, null, null);
            OpenAPI openAPI = result.getOpenAPI();

            if (openAPI == null) {
                System.err.println("⚠️ Failed to parse Swagger/OpenAPI file: " + result.getMessages());
                return null;
            }

            return extractApiDetails(openAPI);

        } catch (Exception e) {
            System.err.println("❌ Error while parsing Swagger file: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Convert YAML format to JSON format using SnakeYAML & Jackson
     */
    private String convertYamlToJson(String yamlContent) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> yamlMap = yaml.load(yamlContent);
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(yamlMap);
        } catch (Exception e) {
            System.err.println("❌ Failed to convert YAML to JSON: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extract API details from the OpenAPI model.
     */
    private String extractApiDetails(OpenAPI openAPI) {
        StringBuilder sb = new StringBuilder();
        openAPI.getPaths().forEach((path, pathItem) -> {
            pathItem.readOperationsMap().forEach((method, operation) -> {
                sb.append("Path: ").append(path)
                        .append(", Method: ").append(method)
                        .append(", Summary: ").append(operation.getSummary())
                        .append("\n");
            });
        });
        return sb.toString();
    }
}
