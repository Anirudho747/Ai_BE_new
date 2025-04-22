package com.testleaf.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class TestCodeGenerator {

	
	public String extractTypescriptCode(String llmResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(llmResponse);
            JsonNode choicesNode = rootNode.path("choices");
            if (choicesNode.isArray() && choicesNode.size() > 0) {
                JsonNode messageNode = choicesNode.get(0).path("message");
                String content = messageNode.path("content").asText().trim();
                if (content.contains("```typescript")) {
                    int start = content.indexOf("```typescript");
                    int end = content.lastIndexOf("```");
                    if (start != -1 && end != -1 && end > start) {
                        content = content.substring(start + "```typescript".length(), end).trim();
                    }
                }
                return content;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return convertToJavaCode(llmResponse);
    }
    /**
     * Extract the Java code from the LLM's response JSON.
     */
    public String extractJavaCode(String llmResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(llmResponse);
            JsonNode choicesNode = rootNode.path("choices");
            if (choicesNode.isArray() && choicesNode.size() > 0) {
                JsonNode messageNode = choicesNode.get(0).path("message");
                String content = messageNode.path("content").asText().trim();
                if (content.contains("```java")) {
                    int start = content.indexOf("```java");
                    int end = content.lastIndexOf("```");
                    if (start != -1 && end != -1 && end > start) {
                        content = content.substring(start + "```java".length(), end).trim();
                    }
                } else if (content.contains("package")) {
                    int index = content.indexOf("package");
                    content = content.substring(index).trim();
                }
                return addMissingImports(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return convertToJavaCode(llmResponse);
    }

    public String convertToJavaCode(String extractedCode) {
        return addMissingImports(extractedCode);
    }

    /**
     * Ensures all required Rest Assured and Hamcrest imports are included.
     */
    private String addMissingImports(String javaCode) {
        String requiredImports = """
            import io.restassured.RestAssured;
            import io.restassured.http.ContentType;
            import io.restassured.response.Response;
            import org.testng.annotations.BeforeMethod;
            import org.testng.annotations.Test;
            import static io.restassured.RestAssured.given;
            import static org.testng.Assert.assertEquals;
            import static org.hamcrest.Matchers.equalTo;
            import static org.hamcrest.Matchers.notNullValue;
            """;

        if (!javaCode.contains("import io.restassured.RestAssured;")) {
            javaCode = requiredImports + "\n" + javaCode;
        }
        return javaCode;
    }
}
