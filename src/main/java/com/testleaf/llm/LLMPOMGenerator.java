package com.testleaf.llm;

import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class LLMPOMGenerator {

    public String generateCrossPlatformPOM(String xmlContent, String className, String packageName, String baseClassName) throws Exception {
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

                String androidLocator = null;
                String iosLocator = null;
                String name = null;

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
                StringBuilder sb = new StringBuilder();
                if (androidLocator != null) {
                    sb.append("\t@AndroidFindBy(id = \"").append(androidLocator).append("\")\n");
                }
                if (iosLocator != null) {
                    sb.append("\t@iOSXCUITFindBy(accessibility = \"").append(iosLocator).append("\")\n");
                }
                sb.append("\tprivate WebElement ").append(fieldName).append(";\n");
                fields.add(sb.toString());
            }
        }

        return buildPOMClass(className, packageName, baseClassName, fields);
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

    public String generateDynamicPOM(String xmlContent, String className, String packageName, String baseClassName) throws Exception {
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

                String androidLocator = null;
                String iosLocator = null;
                String name = null;

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
                StringBuilder method = new StringBuilder();
                method.append("\tpublic By get").append(toCamelCase(name, true)).append("() {\n");
                method.append("\t\tif (driver.getPlatformName().equalsIgnoreCase(\"iOS\")) {\n");
                if (iosLocator != null) {
                    method.append("\t\t\treturn MobileBy.AccessibilityId(\"").append(iosLocator).append("\");\n");
                } else {
                    method.append("\t\t\treturn MobileBy.xpath(\"//XCUIElementTypeAny\");\n");
                }
                method.append("\t\t} else {\n");
                if (androidLocator != null) {
                    method.append("\t\t\treturn MobileBy.id(\"").append(androidLocator).append("\");\n");
                } else {
                    method.append("\t\t\treturn MobileBy.xpath(\"//android.widget.*\");\n");
                }
                method.append("\t\t}\n");
                method.append("\t}\n");

                methods.add(method.toString());
            }
        }

        return buildDynamicPOMClass(className, packageName, baseClassName, methods);
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
