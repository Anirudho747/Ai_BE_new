package testleaf.llm;

import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Component
public class LLMPOMGenerator {

    @Value("${llm.api.url}")
    private String llmApiUrl;

    @Value("${llm.api.key}")
    private String llmApiKey;

    @Value("${llm.model}")
    private String llmModel;

    public String generatePOMWithFallback(String xmlContent, String platform, String className, String packageName, String baseClassName, String mode) throws Exception {
        try {
            return generateUsingLLM(xmlContent, platform, className, packageName, baseClassName, mode);
        } catch (Exception e) {
            switch (mode) {
                case "ANDROID":
                case "IOS":
                    return generateMobilePOM(xmlContent, platform, className, packageName);
                case "CROSS_PLATFORM":
                    return generateCrossPlatformPOMWithMethods(xmlContent, className, packageName, baseClassName);
                case "DYNAMIC_RUNTIME":
                    return generateDynamicPOMWithMethods(xmlContent, className, packageName, baseClassName);
                default:
                    throw new RuntimeException("Unsupported generation mode.");
            }
        }
    }

    // ======== LLM Integration ========
    private String generateUsingLLM(String xmlContent, String platform, String className, String packageName, String baseClassName, String mode) throws Exception {
        List<String> locators = extractFieldLocators(xmlContent, platform);
        if (locators.isEmpty()) throw new RuntimeException("No elements found for LLM generation.");

        String prompt = buildPrompt(platform, className, packageName, baseClassName, mode, locators);
        String aiResponse = callLLM(prompt);
        return extractJavaCode(aiResponse);
    }

    private List<String> extractFieldLocators(String xmlContent, String platform) throws Exception {
        List<String> names = new ArrayList<>();
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));
        NodeList nodes = doc.getElementsByTagName("node");

        for (int i = 0; i < Math.min(nodes.getLength(), 25); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            Element e = (Element) node;
            String name = getBestLocatorName(e, platform);
            if (name != null) names.add(name);
        }
        return names;
    }

    private String getBestLocatorName(Element e, String platform) {
        if (platform.equalsIgnoreCase("ANDROID")) {
            if (e.hasAttribute("resource-id")) return e.getAttribute("resource-id");
            if (e.hasAttribute("content-desc")) return e.getAttribute("content-desc");
        } else {
            if (e.hasAttribute("accessibilityLabel")) return e.getAttribute("accessibilityLabel");
            if (e.hasAttribute("name")) return e.getAttribute("name");
        }
        return null;
    }

    private String buildPrompt(String platform, String className, String packageName, String baseClassName, String mode, List<String> locators) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generate a Java Appium POM class for ").append(platform).append(" using ").append(mode).append(" mode.\n")
                .append("Class name: ").append(className).append("\n")
                .append("Package: ").append(packageName).append("\n");

        if (baseClassName != null && !baseClassName.isBlank()) {
            sb.append("Extend base class: ").append(baseClassName).append("\n");
        }

        sb.append("Include:\n")
                .append("- Basic actions like click, sendKeys\n")
                .append("- Assertions like isVisible, isEnabled\n")
                .append("- Waits using WebDriverWait\n")
                .append("- Compound methods like login(username, password)\n")
                .append("- Validations like isLoginButtonVisible() and toast error detection\n");

        sb.append("Fields:\n");
        locators.forEach(l -> sb.append("- ").append(l).append("\n"));

        sb.append("\nOutput only full Java code inside a code block like ```java ...```.");
        return sb.toString();
    }

    private String callLLM(String prompt) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", llmModel);
        payload.put("temperature", 0.3);
        payload.put("max_tokens", 2000);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", "You are an expert Appium QA assistant writing clean Page Object classes."));
        messages.add(Map.of("role", "user", "content", prompt));
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
        JsonNode json = mapper.readTree(response.body());

        if (json.has("choices")) {
            return json.get("choices").get(0).get("message").get("content").asText();
        } else if (json.has("error")) {
            throw new RuntimeException("LLM Error: " + json.get("error").get("message").asText());
        } else {
            throw new RuntimeException("Unexpected LLM response.");
        }
    }

    private String extractJavaCode(String content) {
        if (content.contains("```java")) {
            int start = content.indexOf("```java") + 7;
            int end = content.lastIndexOf("```", start);
            return content.substring(start, end).trim();
        }
        return content;
    }

    public String generateCrossPlatformPOMWithMethods(String xmlContent, String className, String packageName, String baseClassName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));
        doc.getDocumentElement().normalize();

        NodeList nodeList = doc.getElementsByTagName("node");
        List<String> fields = new ArrayList<>();
        List<String> methods = new ArrayList<>();

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;

                String androidLocator = null, iosLocator = null, name = null;

                if (element.hasAttribute("resource-id")) {
                    androidLocator = element.getAttribute("resource-id");
                    name = androidLocator;
                } else if (element.hasAttribute("content-desc")) {
                    androidLocator = element.getAttribute("content-desc");
                    name = androidLocator;
                }

                if (element.hasAttribute("accessibilityLabel")) {
                    iosLocator = element.getAttribute("accessibilityLabel");
                    if (name == null) name = iosLocator;
                } else if (element.hasAttribute("name")) {
                    iosLocator = element.getAttribute("name");
                    if (name == null) name = iosLocator;
                }

                if (name == null) continue;
                String fieldName = toCamelCase(name, false);
                String readableName = toCamelCase(name, true);

                StringBuilder field = new StringBuilder();
                if (androidLocator != null) {
                    field.append("\t@AndroidFindBy(id = \"").append(androidLocator).append("\")\n");
                }
                if (iosLocator != null) {
                    field.append("\t@iOSXCUITFindBy(accessibility = \"").append(iosLocator).append("\")\n");
                }
                field.append("\tprivate WebElement ").append(fieldName).append(";\n");
                fields.add(field.toString());

                // Actions
                methods.add("\tpublic void click" + readableName + "() {\n\t\t" + fieldName + ".click();\n\t}");

                methods.add("\tpublic void enter" + readableName + "(String input) {\n\t\t" + fieldName + ".sendKeys(input);\n\t}");

                // Waits
                methods.add("\tpublic void waitFor" + readableName + "(WebDriver driver) {\n" +
                        "\t\tnew WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.visibilityOf(" + fieldName + "));\n\t}");

                // Validation
                if (fieldName.toLowerCase().contains("login")) {
                    methods.add("\tpublic boolean isLoginButtonVisible() {\n\t\treturn " + fieldName + ".isDisplayed();\n\t}");
                }

                if (fieldName.toLowerCase().contains("toast") || fieldName.toLowerCase().contains("error")) {
                    methods.add("\tpublic boolean isErrorToastVisible() {\n\t\treturn " + fieldName + ".isDisplayed();\n\t}");
                }
            }
        }

        // Add compound login() if both username and password fields detected
        if (fields.toString().contains("username") && fields.toString().contains("password")) {
            methods.add("\tpublic void login(String username, String password) {\n" +
                    "\t\tthis.username.sendKeys(username);\n" +
                    "\t\tthis.password.sendKeys(password);\n" +
                    "\t\tthis.loginBtn.click();\n\t}");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n\n")
                .append("import io.appium.java_client.pagefactory.AndroidFindBy;\n")
                .append("import io.appium.java_client.pagefactory.iOSXCUITFindBy;\n")
                .append("import io.appium.java_client.pagefactory.AppiumFieldDecorator;\n")
                .append("import io.appium.java_client.AppiumDriver;\n")
                .append("import org.openqa.selenium.WebElement;\n")
                .append("import org.openqa.selenium.support.PageFactory;\n")
                .append("import org.openqa.selenium.support.ui.ExpectedConditions;\n")
                .append("import org.openqa.selenium.support.ui.WebDriverWait;\n")
                .append("import java.time.Duration;\n\n");

        sb.append("public class ").append(className);
        if (baseClassName != null && !baseClassName.isBlank()) {
            sb.append(" extends ").append(baseClassName);
        }
        sb.append(" {\n\n");

        sb.append("\tprivate AppiumDriver driver;\n\n")
                .append("\tpublic ").append(className).append("(AppiumDriver driver) {\n")
                .append("\t\tthis.driver = driver;\n")
                .append("\t\tPageFactory.initElements(new AppiumFieldDecorator(driver), this);\n")
                .append("\t}\n\n");

        fields.forEach(f -> sb.append(f).append("\n"));
        methods.forEach(m -> sb.append(m).append("\n\n"));
        sb.append("}\n");

        return sb.toString();
    }

    public String generateMobilePOM(String xmlContent, String platform, String className, String packageName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));
        doc.getDocumentElement().normalize();

        NodeList nodeList = doc.getElementsByTagName("node");
        List<String> fields = new ArrayList<>();

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;

                String locator = null;
                String name = null;

                if (platform.equalsIgnoreCase("ANDROID")) {
                    if (element.hasAttribute("resource-id")) {
                        locator = element.getAttribute("resource-id");
                        name = locator;
                    } else if (element.hasAttribute("content-desc")) {
                        locator = element.getAttribute("content-desc");
                        name = locator;
                    }
                } else if (platform.equalsIgnoreCase("IOS")) {
                    if (element.hasAttribute("accessibilityLabel")) {
                        locator = element.getAttribute("accessibilityLabel");
                        name = locator;
                    } else if (element.hasAttribute("name")) {
                        locator = element.getAttribute("name");
                        name = locator;
                    }
                }

                if (locator == null || name == null) continue;

                String fieldName = toCamelCase(name, false);
                StringBuilder sb = new StringBuilder();

                if (platform.equalsIgnoreCase("ANDROID")) {
                    sb.append("\t@AndroidFindBy(id = \"").append(locator).append("\")\n");
                } else {
                    sb.append("\t@iOSXCUITFindBy(accessibility = \"").append(locator).append("\")\n");
                }

                sb.append("\tprivate WebElement ").append(fieldName).append(";\n");
                fields.add(sb.toString());
            }
        }

        return buildPOMClass(className, packageName, null, fields);
    }

    public String generateDynamicPOMWithMethods(String xmlContent, String className, String packageName, String baseClassName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));
        doc.getDocumentElement().normalize();

        NodeList nodeList = doc.getElementsByTagName("node");
        List<String> methods = new ArrayList<>();

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;

                String androidLocator = null, iosLocator = null, name = null;

                if (element.hasAttribute("resource-id")) {
                    androidLocator = element.getAttribute("resource-id");
                    name = androidLocator;
                } else if (element.hasAttribute("content-desc")) {
                    androidLocator = element.getAttribute("content-desc");
                    name = androidLocator;
                }

                if (element.hasAttribute("accessibilityLabel")) {
                    iosLocator = element.getAttribute("accessibilityLabel");
                    if (name == null) name = iosLocator;
                } else if (element.hasAttribute("name")) {
                    iosLocator = element.getAttribute("name");
                    if (name == null) name = iosLocator;
                }

                if (name == null) continue;

                String fieldName = toCamelCase(name, false);
                String readableName = toCamelCase(name, true);

                methods.add("\tpublic By get" + readableName + "() {\n" +
                        "\t\tif (driver.getPlatformName().equalsIgnoreCase(\"iOS\")) {\n" +
                        (iosLocator != null ? "\t\t\treturn MobileBy.AccessibilityId(\"" + iosLocator + "\");\n"
                                : "\t\t\treturn MobileBy.xpath(\"//XCUIElementTypeAny\");\n") +
                        "\t\t} else {\n" +
                        (androidLocator != null ? "\t\t\treturn MobileBy.id(\"" + androidLocator + "\");\n"
                                : "\t\t\treturn MobileBy.xpath(\"//android.widget.*\");\n") +
                        "\t\t}\n" +
                        "\t}");

                methods.add("\tpublic void tap" + readableName + "() {\n" +
                        "\t\tdriver.findElement(get" + readableName + "()).click();\n\t}");

                methods.add("\tpublic void enter" + readableName + "(String input) {\n" +
                        "\t\tdriver.findElement(get" + readableName + "()).sendKeys(input);\n\t}");

                methods.add("\tpublic boolean is" + readableName + "Visible() {\n" +
                        "\t\treturn driver.findElement(get" + readableName + "()).isDisplayed();\n\t}");

                if (fieldName.toLowerCase().contains("toast") || fieldName.toLowerCase().contains("error")) {
                    methods.add("\tpublic boolean isErrorToastVisible() {\n" +
                            "\t\treturn driver.findElement(get" + readableName + "()).isDisplayed();\n\t}");
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n\n")
                .append("import io.appium.java_client.AppiumDriver;\n")
                .append("import io.appium.java_client.MobileBy;\n")
                .append("import org.openqa.selenium.By;\n\n");

        sb.append("public class ").append(className);
        if (baseClassName != null && !baseClassName.isBlank()) {
            sb.append(" extends ").append(baseClassName);
        }
        sb.append(" {\n\n")
                .append("\tprivate AppiumDriver driver;\n\n")
                .append("\tpublic ").append(className).append("(AppiumDriver driver) {\n")
                .append("\t\tthis.driver = driver;\n\t}\n\n");

        methods.forEach(m -> sb.append(m).append("\n\n"));
        sb.append("}\n");

        return sb.toString();
    }

    private String buildPOMClass(String className, String packageName, String baseClassName, List<String> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n\n");
        sb.append("import io.appium.java_client.pagefactory.AndroidFindBy;\n");
        sb.append("import io.appium.java_client.pagefactory.iOSXCUITFindBy;\n");
        sb.append("import io.appium.java_client.pagefactory.AppiumFieldDecorator;\n");
        sb.append("import org.openqa.selenium.support.PageFactory;\n");
        sb.append("import io.appium.java_client.AppiumDriver;\n");
        sb.append("import org.openqa.selenium.WebElement;\n\n");

        sb.append("public class ").append(className);
        if (baseClassName != null && !baseClassName.isBlank()) {
            sb.append(" extends ").append(baseClassName);
        }
        sb.append(" {\n\n");

        sb.append("\tpublic ").append(className).append("(AppiumDriver driver) {\n");
        sb.append("\t\tPageFactory.initElements(new AppiumFieldDecorator(driver), this);\n");
        sb.append("\t}\n\n");

        for (String field : fields) {
            sb.append(field).append("\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    private String buildDynamicPOMClass(String className, String packageName, String baseClassName, List<String> methods) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n\n");
        sb.append("import io.appium.java_client.AppiumDriver;\n");
        sb.append("import io.appium.java_client.MobileBy;\n");
        sb.append("import org.openqa.selenium.By;\n\n");

        sb.append("public class ").append(className);
        if (baseClassName != null && !baseClassName.isBlank()) {
            sb.append(" extends ").append(baseClassName);
        }
        sb.append(" {\n\n");

        sb.append("\tprivate AppiumDriver driver;\n\n");
        sb.append("\tpublic ").append(className).append("(AppiumDriver driver) {\n");
        sb.append("\t\tthis.driver = driver;\n");
        sb.append("\t}\n\n");

        for (String method : methods) {
            sb.append(method).append("\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    private String toCamelCase(String str, boolean capitalizeFirst) {
        str = str.replaceAll("[^a-zA-Z0-9]", " ");
        String[] parts = str.split(" ");
        StringBuilder camel = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;
            if (i == 0 && !capitalizeFirst) {
                camel.append(part.substring(0, 1).toLowerCase()).append(part.substring(1));
            } else {
                camel.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
            }
        }
        return camel.toString();
    }
}
