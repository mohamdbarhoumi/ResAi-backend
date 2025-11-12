package org.example.resai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String openAiApiKey;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    /**
     * Generate professional summary based on user's description
     */
    public String generateSummary(String userInput) {
        String systemPrompt = """
            You are a professional resume writer. Generate a concise, ATS-friendly professional summary 
            (2-3 sentences, ~50-80 words) based on the user's description. 
            
            Focus on:
            - Key skills and technologies
            - Career level (student, junior, senior, etc.)
            - Career goals or target role
            - Quantifiable achievements when possible
            
            Use strong action words and make it impactful. Do not use first person (I, my, me).
            Return ONLY the summary text, no additional formatting or explanation.
            """;

        return callOpenAI(systemPrompt, userInput);
    }

    /**
     * Generate experience bullets based on user's description
     */
    public String generateExperienceBullets(String userInput, Map<String, String> context) {
        String role = context != null ? context.getOrDefault("role", "") : "";
        String company = context != null ? context.getOrDefault("company", "") : "";

        String systemPrompt = String.format("""
            You are a professional resume writer. Generate 3-5 ATS-friendly bullet points for a work experience 
            based on the user's description.
            
            Context:
            - Role: %s
            - Company: %s
            
            Guidelines:
            - Start each bullet with a strong action verb (past tense)
            - Include quantifiable achievements when possible (percentages, numbers, metrics)
            - Focus on impact and results, not just responsibilities
            - Keep bullets concise (1-2 lines each)
            - Make them ATS-friendly (use industry keywords)
            
            Format: Return ONLY the bullets, one per line, starting with "• "
            Example:
            • Developed RESTful APIs using Spring Boot, reducing response time by 40%%
            • Collaborated with cross-functional team of 5 to deliver features on schedule
            • Implemented automated testing, increasing code coverage from 60%% to 85%%
            """, role, company);

        return callOpenAI(systemPrompt, userInput);
    }

    /**
     * Generate project bullets based on user's description
     */
    public String generateProjectBullets(String userInput, Map<String, String> context) {
        String projectTitle = context != null ? context.getOrDefault("projectTitle", "") : "";

        String systemPrompt = String.format("""
            You are a professional resume writer. Generate 3-4 ATS-friendly bullet points for a project 
            based on the user's description.
            
            Project: %s
            
            Guidelines:
            - Start each bullet with a strong action verb (past tense: Built, Developed, Implemented, etc.)
            - Highlight key features and technologies used
            - Include technical details (frameworks, languages, tools)
            - Mention any measurable outcomes (users, performance, etc.)
            - Keep bullets concise (1-2 lines each)
            
            Format: Return ONLY the bullets, one per line, starting with "• "
            Example:
            • Built full-stack e-commerce platform using Next.js, PostgreSQL, and Stripe API
            • Implemented user authentication with JWT and OAuth2, supporting 500+ active users
            • Designed responsive UI with Tailwind CSS, achieving 95+ Lighthouse score
            """, projectTitle);

        return callOpenAI(systemPrompt, userInput);
    }

    /**
     * Call OpenAI API with system prompt and user input
     */
    private String callOpenAI(String systemPrompt, String userInput) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-mini"); // Using gpt-4o-mini for cost efficiency
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userInput)
            ));
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 500);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    OPENAI_API_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // Parse the response
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();

            return content.trim();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to call OpenAI API: " + e.getMessage());
        }
    }
}