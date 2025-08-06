package testleaf.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LLMFlakyAnalyzer {

    public List<LLMFlakyTestRunEntry> parseCsv(InputStream in) throws IOException {
        List<LLMFlakyTestRunEntry> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {

            for (CSVRecord csvRecord : csvParser) {
                LLMFlakyTestRunEntry entry = new LLMFlakyTestRunEntry();
                entry.setTestName(csvRecord.get("testName"));
                entry.setStatus(csvRecord.get("status"));
                entry.setDuration(Long.parseLong(csvRecord.get("duration")));
                entry.setErrorMsg(csvRecord.get("errorMessage"));
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                entry.setExecutedAt(LocalDateTime.parse(csvRecord.get("timestamp"), formatter));
                entries.add(entry);
            }
        }
        return entries;
    }

    public List<LLMFlakyTestRunEntry> parseJson(InputStream in) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return Arrays.asList(mapper.readValue(in, LLMFlakyTestRunEntry[].class));
    }

    private String buildPrompt(List<LLMFlakyTestRunEntry> runs) {
        if (runs == null || runs.isEmpty()) return "";

        String testName = runs.get(0).getTestName();
        long failCount = runs.stream().filter(r -> "FAILED".equalsIgnoreCase(r.getStatus())).count();
        double failRate = (double) failCount / runs.size();

        StringBuilder sb = new StringBuilder();
        sb.append("Test Name: ").append(testName).append("\n");
        sb.append("Fail Rate: ").append(String.format("%.2f", failRate * 100)).append("%\n");
        sb.append("Recent Runs:\n");

        for (LLMFlakyTestRunEntry run : runs) {
            sb.append("- [")
                    .append(run.getExecutedAt()).append("] ")
                    .append(run.getStatus()).append(" | ")
                    .append(run.getDuration()).append("ms | ")
                    .append(run.getErrorMsg()).append("\n");
        }

        sb.append("\nSuggestion:\n");
        return sb.toString();
    }

    private String callLLM(String prompt, String apiUrl, String apiKey, String model) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0.3);
        payload.put("max_tokens", 1500);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", "You are a test flakiness analyzer."));
        messages.add(Map.of("role", "user", "content", prompt));
        payload.put("messages", messages);

        String json = mapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode jsonNode = mapper.readTree(response.body());
        return jsonNode.get("choices").get(0).get("message").get("content").asText();
    }

    private Map<String, String> parseSuggestions(String rawResponse) {
        Map<String, String> suggestions = new HashMap<>();
        String[] lines = rawResponse.split("\n");
        String currentTest = null;

        for (String line : lines) {
            if (line.startsWith("Test Name: ")) {
                currentTest = line.replace("Test Name: ", "").trim();
                suggestions.put(currentTest, "");
            } else if (currentTest != null) {
                suggestions.put(currentTest, suggestions.get(currentTest) + line + "\n");
            }
        }

        return suggestions;
    }

    public LLMFlakyTestResponse analyze(InputStream input, String format, int threshold,
                                        String llmApiUrl, String llmApiKey, String llmModel) throws Exception {
        List<LLMFlakyTestRunEntry> allEntries = format.equalsIgnoreCase("csv") ?
                parseCsv(input) :
                parseJson(input);

        Map<String, List<LLMFlakyTestRunEntry>> grouped = allEntries.stream()
                .collect(Collectors.groupingBy(LLMFlakyTestRunEntry::getTestName));

        List<LLMFlakyTestResult> flakyTests = new ArrayList<>();

        for (Map.Entry<String, List<LLMFlakyTestRunEntry>> entry : grouped.entrySet()) {
            String testName = entry.getKey();
            List<LLMFlakyTestRunEntry> runs = entry.getValue();

            long failCount = runs.stream().filter(r -> "FAILED".equalsIgnoreCase(r.getStatus())).count();
            double failRate = (double) failCount / runs.size();

            if (failRate >= threshold / 100.0) {
                LLMFlakyTestResult result = new LLMFlakyTestResult();
                result.setTestName(testName);
                result.setFailRate(failRate);
                result.setFailCount((int) failCount);
                result.setTotalRuns(runs.size());
                result.setRuns(runs);

                Optional<LLMFlakyTestRunEntry> latestFail = runs.stream()
                        .filter(r -> "FAILED".equalsIgnoreCase(r.getStatus()))
                        .max(Comparator.comparing(LLMFlakyTestRunEntry::getExecutedAt));

                result.setErrorMessage(latestFail.map(LLMFlakyTestRunEntry::getErrorMsg).orElse("N/A"));
                flakyTests.add(result);
            }
        }

        flakyTests.sort((a, b) -> Double.compare(b.getFailRate(), a.getFailRate()));
        List<LLMFlakyTestResult> topFlaky = flakyTests.stream().limit(10).collect(Collectors.toList());

        // Build prompt
        StringBuilder combinedPrompt = new StringBuilder();
        combinedPrompt.append("Instructions:\n")
                .append("- You are a QA flakiness assistant.\n")
                .append("- For each test, suggest retry logic, waits, or root causes.\n\n");

        for (LLMFlakyTestResult result : topFlaky) {
            String prompt = buildPrompt(result.getRuns());
            combinedPrompt.append(prompt).append("\n");
        }

        String finalPrompt = combinedPrompt.toString();
        String suggestionResponse = callLLM(finalPrompt, llmApiUrl, llmApiKey, llmModel);
        Map<String, String> suggestions = parseSuggestions(suggestionResponse);

        for (LLMFlakyTestResult result : topFlaky) {
            result.setSuggestion(suggestions.getOrDefault(result.getTestName(), "No suggestion."));
        }

        return new LLMFlakyTestResponse(topFlaky, suggestions);
    }
}
