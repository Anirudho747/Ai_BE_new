package testleaf.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class LLMConverterService {

    /**
     * Converts Selenium Java code to Playwright TypeScript code.
     */
    public String convertSeleniumToPlaywright(String seleniumCode, String llmApiKey, String llmApiUrl, String llmModel) {
        if (seleniumCode == null || seleniumCode.isEmpty()) {
            return "No valid Selenium code provided.";
        }

        // Updated prompt that instructs the LLM to output only the final Playwright TypeScript code.
        String prompt = "Instructions:\n"
        		+ "\n"
        		+ "- Convert Selenium Java test automation code to Playwright TypeScript while preserving the logic and functionality.\n"
        		+ "- Ensure that the converted code follows Playwright's best practices, including:\n"
        		+ "    -- Proper async/await usage for handling asynchronous operations.\n"
        		+ "    -- Selectors conversion (e.g., By.id() → page.locator() equivalent).\n"
        		+ "    -- Handling of waits (Implicit/Explicit waits should be replaced with Playwright’s auto-waiting).\n"
        		+ "    -- Assertions should be mapped to Playwright’s test assertions if applicable.\n"
        		+ "    -- Maintain proper TypeScript typings (Page, Browser, etc.) and use ES6+ features where appropriate.\n"
        		+ "    -- Optimize code structure, removing unnecessary waits or redundant calls.\n"
        		+ "    -- Ensure that logging/debugging mechanisms (if present in Selenium) are mapped correctly to Playwright equivalents.\n"
        		+ "    -- The output must be idiomatic Playwright TypeScript, not just a direct Java-to-TypeScript translation.\n"
        		+ "    -- DO NOT add any additional steps other than given input code.\n"
        		+ "    -- Always use page.locator() method followed by actions and DO not call method without locating \n"
        		+ "	   -- when using name locator, it should look like await page.locator('[name=\"value\"]')."
        		+ "    -- Make sure to waitUntil: 'domcontentloaded' \n"
        		+ "    -- DO NOT Use expect and just use console.log \n"
        		+ "    -- selectOption is the dropdown selection function in playwright typescript"
        		+ "    -- fill is the text box type function in playwright typescript"
        		+ "	   --[MANDATORY] ONLY use getByRole('link', { name: 'text' }) for linktext"
        		+ "    --[IMPORTANT] Follow Playwright Official Documentation to ensure all functions are correctly\n"
        		+ "    --[MANDATORY] Follow the exact output format as in the example"
        		+ "    --[CRITICAL] Use Playwright latest version when converting the code\n"
        		+ "\n"
        		+ "Context:\n"
        		+ "\n"
        		+ "I am building an AI-based prompt to convert Selenium Java code to Playwright TypeScript automatically.\n"
        		+ "The converted code must be production-ready, as accuracy is crucial for my career growth.\n"
        		+ "\n"
        		+ "Example:\n"
        		+ "\n"
        		+ "Selenium Java (Input)\n"
        		+ "\n"
        		+ "java\n"
        		+ "import org.openqa.selenium.WebDriver;\n"
        		+ "import org.openqa.selenium.chrome.ChromeDriver;\n"
        		+ "\n"
        		+ "public class PrintTitle {\n"
        		+ "  public static void main(String[] args) {\n"
        		+ "    WebDriver driver = new ChromeDriver();\n"
        		+ "    driver.get(\"http://playwright.dev\");\n"
        		+ "    System.out.println(driver.getTitle());\n"
        		+ "    driver.quit();\n"
        		+ "  }\n"
        		+ "}\n"
        		+ "\n"
        		+ "\n"
        		+ "Playwright TypeScript (Expected Output)\n"
        		+ "\n"
        		+ "typescript\n"
        		+ "import { test, expect } from '@playwright/test';\n"
        		+ "\n"
        		+ "test('has title', async ({ page }) => {\n"
        		+ "  await page.goto('https://playwright.dev/');\n"
        		+ "\n"
        		+ "  // Expect a title \"to contain\" a substring.\n"
        		+ "console.log(await page.title());\n"
        		+ "});\n"
        		+ "\n"
        		+ "\n"
        		+ "Persona:\n"
        		+ "\n"
        		+ "You are a Senior Test Automation Architect specializing in Selenium and Playwright migration. \n"
        		+ "Your responsibility is to ensure that the converted Playwright TypeScript code is accurate, maintainable, and follows industry best practices.\n"
        		+ "\n"
        		+ "Output Format:\n"
        		+ "\n"
        		+ "-   The output should be fully working Playwright TypeScript code.\n"
        		+ "-   It should be structured as an executable script or within a test framework if required.\n"
        		+ "-   The code should be formatted properly and follow Playwright’s official documentation. \n"
        		+ "-   DO NOT Provide anything other than Playwright Code Such as explanations, Key Points.\n"
        		+ "-   Make Sure the comments are staying as it is in the code.\n"
        		+ "\n"
        		+ "Use the above framework to generate the playwright typescript code for the following java code:"
                + seleniumCode;

        try {
			Map<String, Object> payload = new HashMap<>();
			payload.put("model", llmModel);
			payload.put("temperature", 0.3);
			payload.put("max_tokens", 2000);

			List<Map<String, String>> messages = new ArrayList<>();
			messages.add(Map.of("role", "user", "content", prompt));
			payload.put("messages", messages);

			ObjectMapper mapper = new ObjectMapper();
			String requestBody = mapper.writeValueAsString(payload);

			String jsonResponse = callLLMApi(requestBody, llmApiUrl, llmApiKey);
			JsonNode root = mapper.readTree(jsonResponse);

			if (root.has("choices") && root.get("choices").size() > 0) {
				return extractCode(root.get("choices").get(0).get("message").get("content").asText());
			} else if (root.has("error")) {
				throw new RuntimeException("LLM Error: " + root.get("error").get("message").asText());
			} else {
				throw new RuntimeException("Unexpected LLM response");
			}
    } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
}

	private String callLLMApi(String requestBody, String llmApiUrl, String llmApiKey) {
		try (var httpClient = HttpClients.createDefault()) {
			var request = new HttpPost(llmApiUrl);
			request.setHeader("Content-Type", "application/json");
			request.setHeader("Authorization", "Bearer " + llmApiKey);
			request.setEntity(new StringEntity(requestBody));

			try (CloseableHttpResponse response = httpClient.execute(request)) {
				return EntityUtils.toString(response.getEntity());
			}
		} catch (Exception e) {
			e.printStackTrace();
			return "{\"error\":\"" + e.getMessage() + "\"}";
		}
	}

	private String extractCode(String content) {
		if (content.contains("```")) {
			int start = content.indexOf("```");
			int langStart = content.indexOf("\n", start);
			int end = content.indexOf("```", langStart + 1);
			if (start != -1 && end != -1) {
				return content.substring(langStart + 1, end).trim();
			}
		}
		return content.trim(); // fallback
	}
}
