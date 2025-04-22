package com.testleaf.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class LLMTestGenerator {

    @Value("${llm.api.url}")
    private String llmApiUrl;

    @Value("${llm.api.key}")
    private String apiKey;

    @Value("${llm.model}")
    private String modelName;

    /**
     * Generates test cases given API details and a list of test types.
     */
    public String generateTestCases(String apiDetails, List<String> testTypes) {
        if (apiDetails == null || apiDetails.isEmpty()) {
            return "No valid API details to generate test cases.";
        }
        
        // Build the test type instruction line according to the following logic:
        // 1. Default (if no type provided): Only Positive tests.
        // 2. If one type is provided, use "Include Only {Type} tests".
        // 3. If more than one type is provided, join them with " and ".
        
        System.out.println(testTypes);
        String testTypeLine;
        if (testTypes == null || testTypes.isEmpty()) {
            testTypeLine = "- Include Only Positive tests\n";
        } else if (testTypes.size() == 1) {
            String type = testTypes.get(0).toLowerCase();
            if ("negative".equals(type)) {
                testTypeLine = "- Include Only Negative tests\n";
            } else if ("edge".equals(type)) {
                testTypeLine = "- Include Only Edge tests\n";
            } else {
                testTypeLine = "- Include Only Positive tests\n";
            }
        } else {
            // For multiple selections, create a list of capitalized types.
            List<String> types = new ArrayList<>();
            for (String type : testTypes) {
                if ("negative".equalsIgnoreCase(type)) {
                    types.add("Negative");
                } else if ("edge".equalsIgnoreCase(type)) {
                    types.add("Edge");
                } else if ("positive".equalsIgnoreCase(type)) {
                    types.add("Positive");
                }
            }
            if (types.size() == 1) {
                testTypeLine = "- Include Only " + types.get(0) + " tests\n";
            } else {
                String joined = String.join(" and ", types);
                testTypeLine = "- Include " + joined + " tests\n";
            }
        }
        
        System.out.println(testTypeLine);

        
        // Build the user prompt using the API details.
        String userPrompt = "Generate REST API test cases using Rest Assured (Java, TestNG) for the following API specification:\n"
                            + apiDetails;

        try {
            // Prepare the chat messages
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");

            String systemContent =
                    "You are a helpful assistant that generates Java code for API tests. "
                  + "Your response must contain only Java code enclosed in a single code block using triple backticks (```java ... ```). "
                  + "- Do not include any additional text explanations. "
                  + "- Be a complete and executable Java class. "
                  + "- Use only standard and correct imports (e.g., org.testng.Assert, io.restassured.RestAssured). "
                  + testTypeLine
                  + "- Add package as automation.tests "
                  + "- Write comments on the code "
                  + "- Print output of the API response "
                  + "- Generate Java code that is compatible with Java 8 version ONLY "
                  + "- Use BeforeMethod of Testng with hardcoded baseURI "
                  + "- DO NOT add any assertion other than status code "
                  + "- DO NOT use Arrays.stream method in the generated code "
                  + "- The generated code should use only the Java methods belonging to RestAssured and TestNG compatible versions "
                  + "- If you parse JSON (e.g., new ObjectMapper().readTree(...)), handle or declare any exceptions (throws Exception or try/catch).";
            
            systemMessage.put("content", systemContent);
            messages.add(systemMessage);

            // Prepare the user message with API details
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);
            messages.add(userMessage);

            // Build the request payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", modelName);
            payload.put("messages", messages);
            payload.put("temperature", 0.1);
            payload.put("top_p", 0.2);
            payload.put("max_tokens", 10000);

            ObjectMapper mapper = new ObjectMapper();
            String requestBody = mapper.writeValueAsString(payload);

            // Call the LLM endpoint
            return callLLMApi(requestBody);

        } catch (Exception e) {
            e.printStackTrace();
            return "Error building JSON payload: " + e.getMessage();
        }
    }

    // For backward compatibility: defaults to positive tests if testTypes is not provided.
    public String generateTestCases(String apiDetails) {
        return generateTestCases(apiDetails, new ArrayList<>());
    }

    private String callLLMApi(String requestBody) {
        try (var httpClient = HttpClients.createDefault()) {
            var request = new HttpPost(llmApiUrl);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Authorization", "Bearer " + apiKey);
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
