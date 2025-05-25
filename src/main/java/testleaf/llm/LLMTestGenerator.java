package testleaf.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
public class LLMTestGenerator {

    /**
     * Generates test cases given API details and a list of test types.
     */

    public String generateTestCases(String apiDetails, List<String> testTypes, String llmApiUrl, String llmApiKey, String llmModel) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", llmModel);
        payload.put("temperature", 0.3);
        payload.put("max_tokens", 1800);

        String typesStr = String.join(", ", testTypes);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", "You are an expert RestAssured QA generating optimal test cases using Java and JUnit. Output only Java code in a code block."
        ));
        messages.add(Map.of(
                "role", "user",
                "content", "Generate Java test cases using RestAssured for the following API description:\n\n" +
                        apiDetails + "\n\nInclude the following test types: " + typesStr +
                        "\nUse proper assertions and keep code clean.\nOnly output Java code in a ```java ...``` block."
        ));

        payload.put("messages", messages);

        String requestBody = mapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(llmApiUrl))
                .header("Authorization", "Bearer " + llmApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode root = mapper.readTree(response.body());

        if (root.has("choices")) {
            return root.get("choices").get(0).get("message").get("content").asText();
        } else if (root.has("error")) {
            throw new RuntimeException("LLM Error: " + root.path("error").path("message").asText());
        } else {
            throw new RuntimeException("Unexpected response from LLM: " + response.body());
        }
    }


    // For backward compatibility: defaults to positive tests if testTypes is not provided.
    public String generateTestCases(String apiDetails, String llmApiUrl, String llmApiKey, String llmModel) throws Exception {
        return generateTestCases(apiDetails, new ArrayList<>(), llmApiUrl, llmApiKey, llmModel);
    }

    private String callLLMApi(String requestBody, String llmApiUrl, String llmApiKey) {
        try (var httpClient = HttpClients.createDefault()) {
            var request = new HttpPost(llmApiUrl);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Authorization", "Bearer " + llmApiKey);
            request.setEntity(new StringEntity(requestBody));
            System.out.println(requestBody);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return EntityUtils.toString(response.getEntity());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error calling LLM API: " + e.getMessage();
        }
    }
}
