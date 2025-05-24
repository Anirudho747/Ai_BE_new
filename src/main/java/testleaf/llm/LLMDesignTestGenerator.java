package testleaf.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class LLMDesignTestGenerator {

    @Value("${llm.api.url}")
    private String llmApiUrl;

    @Value("${llm.api.key}")
    private String apiKey;

    @Value("${llm.model}")
    private String modelName;

    public String buildPromptFromDescription(String description, String mode) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are a helpful QA assistant.\n\n");
        sb.append("Use the following description to generate test cases.\n\n");
        sb.append("Description:\n").append(description).append("\n\n");

        if ("BDD".equalsIgnoreCase(mode)) {
            sb.append("Generate test cases in BDD Gherkin format.\n");
            sb.append("Include tags like @positive, @negative, @edge.\n");
            sb.append("Each scenario should be well-labeled and clear.\n\n");
        } else if ("TDD".equalsIgnoreCase(mode)) {
            sb.append("Generate Java-style JUnit test methods for a test class.\n");
            sb.append("Each method should follow the format:\n");
            sb.append("@Test\n");
            sb.append("public void testXyz() {\n");
            sb.append("    // Setup and assertions\n");
            sb.append("}\n\n");
            sb.append("Include validations like assertTrue, assertEquals etc.\n");
        } else {
            sb.append("Return manual test cases in classic format.\n");
            sb.append("Each test case should have: Title, Prerequisites, Steps, Expected Result.\n");
        }

        return sb.toString();
    }

    public String callLLMToGenerateTestCases(String prompt) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", modelName);
        payload.put("temperature", 0.3);
        payload.put("max_tokens", 1500);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", "You are an expert QA assistant. Output only the test cases. Do not include internal thoughts or extra commentary."
        ));
        messages.add(Map.of("role", "user", "content", prompt));
        payload.put("messages", messages);

        ObjectMapper mapper = new ObjectMapper();
        String requestBody = mapper.writeValueAsString(payload);

        try (var client = org.apache.http.impl.client.HttpClients.createDefault()) {
            var request = new org.apache.http.client.methods.HttpPost(llmApiUrl);
            request.setHeader("Authorization", "Bearer " + apiKey);
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new org.apache.http.entity.StringEntity(requestBody));

            var response = client.execute(request);
            var responseText = org.apache.http.util.EntityUtils.toString(response.getEntity());

            JsonNode root = mapper.readTree(responseText);

            if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
                String content = root.path("choices").get(0).path("message").path("content").asText().trim();

                // Clean <think> block if present
                if (content.contains("<think>")) {
                    int startIdx = content.indexOf("</think>");
                    if (startIdx != -1 && startIdx + 7 < content.length()) {
                        content = content.substring(startIdx + 7).trim();
                    }
                }

                return content;
            } else if (root.has("error")) {
                throw new RuntimeException("LLM Error: " + root.path("error").path("message").asText());
            } else {
                throw new RuntimeException("Unexpected LLM response: " + responseText);
            }
        }
    }

}
