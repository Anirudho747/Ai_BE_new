package testleaf.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LLMDesignTestGenerator {

    // Step 1: Build the LLM Prompt
    public String buildPromptFromDescription(String description) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a professional QA engineer assistant.\n")
                .append("Given the following UI screen description, generate both:\n")
                .append("1. BDD-style manual test cases (in Gherkin format).\n")
                .append("2. TDD-style Java test cases (JUnit, based on assertions).\n\n")
                .append("Separate them with proper headings:\n")
                .append("### BDD\n")
                .append("... BDD Tests ...\n")
                .append("### TDD\n")
                .append("... TDD Tests ...\n\n")
                .append("Ensure both are clean, readable, and production-ready.\n\n")
                .append("Screen Description:\n")
                .append(description.trim())
                .append("\n");

        return sb.toString();
    }

    // Step 2: Call the LLM API
    public Map<String, String> callLLMToGenerateTestCases(String prompt, String llmApiUrl, String llmApiKey, String llmModel) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", llmModel);
        payload.put("temperature", 0.3);
        payload.put("max_tokens", 2000);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", "You are an expert QA assistant generating BDD and TDD style test cases."));
        messages.add(Map.of("role", "user", "content", prompt));
        payload.put("messages", messages);

        String requestBody = mapper.writeValueAsString(payload);

        try (var httpClient = HttpClients.createDefault()) {
            var request = new HttpPost(llmApiUrl);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Authorization", "Bearer " + llmApiKey);
            request.setEntity(new StringEntity(requestBody));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String json = EntityUtils.toString(response.getEntity());
                JsonNode root = mapper.readTree(json);

                if (root.has("choices")) {
                    String content = root.path("choices").get(0).path("message").path("content").asText();

                    // Clean up LLM formatting
                    if (content.contains("<think>")) {
                        int startIdx = content.indexOf("</think>");
                        content = content.substring(startIdx + 7).trim();
                    }

                    // Split into BDD and TDD based on heading markers
                    String bddPart = "", tddPart = "";

                    int bddStart = content.indexOf("### BDD");
                    int tddStart = content.indexOf("### TDD");

                    if (bddStart != -1 && tddStart != -1) {
                        bddPart = content.substring(bddStart, tddStart).replace("### BDD", "").trim();
                        tddPart = content.substring(tddStart).replace("### TDD", "").trim();
                    } else {
                        bddPart = content;
                    }

                    Map<String, String> result = new HashMap<>();
                    result.put("bdd", bddPart);
                    result.put("tdd", tddPart);
                    return result;

                } else if (root.has("error")) {
                    throw new RuntimeException("LLM Error: " + root.path("error").path("message").asText());
                } else {
                    throw new RuntimeException("Unexpected response from LLM API: " + json);
                }
            }
        }
    }

}

