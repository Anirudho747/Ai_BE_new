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
public class LLMTestCaseGenerator {

    @Value("${llm.api.url}")
    private String llmApiUrl;

    @Value("${llm.api.key}")
    private String apiKey;

    @Value("${llm.model}")
    private String modelName;

    /**
     * Generates test cases given user story details and test types.
     */

    public String llmGenerateTestCases(String testType, String userStoryDescription, String applicationUrl,
                                       String acceptanceCriteria, String epicDescription, String additionalInstructions) {

        if (userStoryDescription == null || userStoryDescription.isEmpty()) {
            return "No valid user story description to generate test cases.";
        }

        String testTypePrompt;
        if (testType == null || testType.isEmpty()) {
            return "Only Positive tests"; // Default to positive
        }

        String[] types = testType.split(",\\s*"); // Split by comma and optional space
        int count = types.length;

        if (count == 1) {
            testTypePrompt = "Only " + types[0] + " tests";
        } else if (count == 2) {
            testTypePrompt = "Combination of " + types[0] + " and " + types[1] + " tests";
        } else if (count == 3) {
            testTypePrompt = "Combination of Positive, Negative, and Edge tests";
        }
        else
            testTypePrompt = "Only Positive tests";

        // Build the user prompt using the user story details
        String userPrompt = "Generate test cases for the following user story description:\n"
                + userStoryDescription + "\n"
                + "Application URL: " + applicationUrl + "\n"
                + "Acceptance Criteria: " + acceptanceCriteria +"\n"
                + "Test Type :"+ testTypePrompt;

        if (epicDescription != null && !epicDescription.isEmpty()) {
            userPrompt = userPrompt + "\nEpic Description: " + epicDescription;
        }

        if (additionalInstructions != null && !additionalInstructions.trim().isEmpty()) {
            userPrompt = userPrompt + "\n[Important] Additional Instructions: " + additionalInstructions;
        }

        try {
            // Prepare the chat messages
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");

            String systemContent = "Instruction:\n"
                    + "You are a highly skilled test analyst specializing in manual test case generation. Your task is to create structured manual test cases based on the given inputs, such as:\n\n"
                    + "- Application URL\n"
                    + "- User Story Description\n"
                    + "- Acceptance Criteria\n"
                    + "- Test Types (Positive, Negative, Edge, or All)\n\n"
                    + "Each test case should be well-structured and easy to follow for manual testers.\n\n"
                    + "Guidelines:\n"
                    + "- Write in a step-by-step format using plain English.\n"
                    + "- Do not include any code or automation-related steps.\n"
                    + "- Ensure each test case covers a different scenario (positive, negative, and edge cases).\n"
                    + "- Each test case must have:\n"
                    + "  - A unique Test Case ID (e.g., TC001, TC002)\n"
                    + "  - Clear test case description\n"
                    + "  - Preconditions (if applicable)\n"
                    + "  - Well-defined test steps\n"
                    + "  - Expected and actual results\n"
                    + "  - Status (set as \"Pending\" by default)\n"
                    + "- Ensure completeness by covering all conditions mentioned in the user story and acceptance criteria.\n\n"
                    + "Context:\n"
                    + "You are assisting testers who will execute these test cases manually. Your test cases should be detailed yet concise, ensuring clarity and accuracy.\n\n"
                    + "Persona:\n"
                    + "- You are a meticulous and detail-oriented senior test lead.\n"
                    + "- Your responses should be precise, structured, and well-formatted.\n\n"
                    + "Output Format:\n"
                    + "Your response must be in structured JSON format as shown below:\n\n"
                    + "{\n"
                    + "  \"testCases\": [\n"
                    + "    {\n"
                    + "      \"TC_No\": \"TC001\",\n"
                    + "      \"Test_Case_Description\": \"Verify login with valid credentials\",\n"
                    + "      \"Pre-requisites\": \"User must have valid credentials\",\n"
                    + "      \"Test_Steps\": \"1. Open login page\\n2. Enter valid username\\n3. Enter valid password\\n4. Click on Login\",\n"
                    + "      \"Actual_Result\": \"User is successfully logged in\",\n"
                    + "      \"Expected_Result\": \"User should be logged in successfully\",\n"
                    + "      \"Status\": \"Pending\"\n"
                    + "    },\n"
                    + "    {\n"
                    + "      \"TC_No\": \"TC002\",\n"
                    + "      \"Test_Case_Description\": \"Verify login with invalid credentials\",\n"
                    + "      \"Pre-requisites\": \"User must have an invalid set of credentials\",\n"
                    + "      \"Test_Steps\": \"1. Open login page\\n2. Enter invalid username\\n3. Enter invalid password\\n4. Click on Login\",\n"
                    + "      \"Actual_Result\": \"Error message displayed\",\n"
                    + "      \"Expected_Result\": \"User should receive an error message\",\n"
                    + "      \"Status\": \"Pending\"\n"
                    + "    }\n"
                    + "  ]\n"
                    + "}\n\n"
                    + "Tone:\n"
                    + "- Professional, clear, and structured\n"
                    + "- Concise yet detailed\n"
                    + "- User-friendly for manual testers";




            systemMessage.put("content", systemContent);
            messages.add(systemMessage);

            // Prepare the user message with user story details
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
            payload.put("max_tokens", 20000);

            ObjectMapper mapper = new ObjectMapper();
            String requestBody = mapper.writeValueAsString(payload);

            // Call the LLM endpoint
            return callLLMApi(requestBody);

        } catch (Exception e) {
            e.printStackTrace();
            return "Error building JSON payload: " + e.getMessage();
        }
    }

    // For backward compatibility: defaults to positive tests if testType is not provided.
    public String generateTestCases(String userStoryDescription, String applicationUrl,
                                    String acceptanceCriteria, String epicDescription, String additionalInstruction) {
        return llmGenerateTestCases("positive", userStoryDescription, applicationUrl, acceptanceCriteria, epicDescription, additionalInstruction);
    }

    private String callLLMApi(String requestBody) {
        try (var httpClient = HttpClients.createDefault()) {
            var request = new HttpPost(llmApiUrl);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Authorization", "Bearer " + apiKey);
            request.setEntity(new StringEntity(requestBody));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return EntityUtils.toString(response.getEntity());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error calling LLM API: " + e.getMessage();
        }
    }
}