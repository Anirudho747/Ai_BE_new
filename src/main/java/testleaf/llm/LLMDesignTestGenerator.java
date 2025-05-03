package testleaf.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class LLMDesignTestGenerator {

    private static final String API_KEY = "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"; // 🔐 Replace with env/config
    private static final String LLM_API_URL = "https://api.groq.com/openai/v1/chat/completions"; // or your preferred endpoint
    private static final String MODEL = "deepseek-r1-distill-llama-70b"; // or gpt-4-turbo / claude etc.

    public String buildPromptFromDescription(String description, String figmaUrl) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are a helpful QA assistant.\n\n");
        sb.append("Generate manual test cases based on the following description.\n");
        sb.append("Include Positive, Negative, and Edge cases.\n\n");

        sb.append("Description:\n");
        sb.append(description).append("\n");

        if (figmaUrl != null && !figmaUrl.trim().isEmpty()) {
            sb.append("\nFigma Design Reference: ").append(figmaUrl).append("\n");
        }

        sb.append("\nReturn test cases in the format:\n");
        sb.append("- Test Case Title\n");
        sb.append("- Prerequisites\n");
        sb.append("- Steps:\n    1. ...\n    2. ...\n  ");
        sb.append("- Expected Result: ...\n\n");

        return sb.toString();
    }

    public String callLLMToGenerateTestCases(String prompt) throws Exception
    {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", MODEL);
        payload.put("temperature", 0.3);
        payload.put("max_tokens", 1500);

        // 🧠 YOU MUST ADD messages
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", "You are an expert QA assistant who writes clean manual test cases.Please always include Positive,Negative,Edge,Security and Performance test cases. Directly output test cases.No thinking steps or internal notes."
        ));
        messages.add(Map.of(
                "role", "user",
                "content", prompt
        ));

        payload.put("messages", messages); // ✅ ADDING this line

        ObjectMapper mapper = new ObjectMapper();
        String requestBody = mapper.writeValueAsString(payload);

        try (var client = org.apache.http.impl.client.HttpClients.createDefault()) {
            var request = new org.apache.http.client.methods.HttpPost(LLM_API_URL);
            request.setHeader("Authorization", "Bearer " + API_KEY);
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new org.apache.http.entity.StringEntity(requestBody));

            var response = client.execute(request);
            var responseText = org.apache.http.util.EntityUtils.toString(response.getEntity());

            var root = mapper.readTree(responseText);

            if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
                String content = root.path("choices").get(0).path("message").path("content").asText().trim();

                // ➡️ Clean up <think>...</think> block if present
                if (content.contains("<think>")) {
                    int startIdx = content.indexOf("</think>");
                    if (startIdx != -1 && startIdx + 7 < content.length()) {
                        content = content.substring(startIdx + 7).trim();
                    }
                }

                return content;
            } else if (root.has("error")) {
                String errorMsg = root.path("error").path("message").asText();
                throw new RuntimeException("LLM Error: " + errorMsg);
            } else {
                throw new RuntimeException("Unexpected response from LLM API: " + responseText);
            }
        }
    }

}
