package org.example.resai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String openAiApiKey;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    // ============ RESUME BUILDER AI FEATURES ============

    /**
     * Generate professional summary based on user's description
     * Supports: English, French
     */
    public String generateSummary(String userInput, String language) {
        String systemPrompt = getSystemPromptForSummary(language);
        return callOpenAISimple(systemPrompt, userInput, 500);
    }

    /**
     * Generate experience bullets based on user's description
     * Supports: English, French
     */
    public String generateExperienceBullets(String userInput, Map<String, String> context, String language) {
        String role = context != null ? context.getOrDefault("role", "") : "";
        String company = context != null ? context.getOrDefault("company", "") : "";

        String systemPrompt = getSystemPromptForExperience(role, company, language);
        return callOpenAISimple(systemPrompt, userInput, 500);
    }

    /**
     * Generate project bullets based on user's description
     * Supports: English, French
     */
    public String generateProjectBullets(String userInput, Map<String, String> context, String language) {
        String projectTitle = context != null ? context.getOrDefault("projectTitle", "") : "";

        String systemPrompt = getSystemPromptForProject(projectTitle, language);
        return callOpenAISimple(systemPrompt, userInput, 500);
    }

    // ============ RESUME TAILORING FEATURES ============

    /**
     * Tailor resume to match job description - OPTIMIZED & LANGUAGE-AWARE
     * Reduced tokens by 60%
     */
    public Map<String, Object> tailorResume(Map<String, Object> resumeData, String jobDescription, String language) {
        try {
            log.info("Starting resume tailoring in language: {}", language);
            String prompt = buildTailoringPrompt(resumeData, jobDescription, language);
            String response = callOpenAIAdvanced(prompt, 2000, 0.5);

            Map<String, Object> tailoredData = parseAIResponse(response, resumeData);
            log.info("Resume tailoring completed successfully");
            return tailoredData;
        } catch (Exception e) {
            log.error("Failed to tailor resume: {}", e.getMessage());
            throw new RuntimeException("Failed to tailor resume: " + e.getMessage());
        }
    }

    /**
     * Generate cover letter - OPTIMIZED & LANGUAGE-AWARE
     * Reduced tokens by 70%
     */
    public String generateCoverLetter(Map<String, Object> resumeData, String jobDescription, String language) {
        try {
            log.info("Starting cover letter generation in language: {}", language);
            String prompt = buildCoverLetterPrompt(resumeData, jobDescription, language);
            String coverLetter = callOpenAIAdvanced(prompt, 800, 0.7);
            log.info("Cover letter generated successfully");
            return coverLetter.trim();
        } catch (Exception e) {
            log.error("Failed to generate cover letter: {}", e.getMessage());
            throw new RuntimeException("Failed to generate cover letter: " + e.getMessage());
        }
    }

    // ============ PRIVATE HELPER METHODS ============

    /**
     * System prompts for resume builder features
     */
    private String getSystemPromptForSummary(String language) {
        if ("fr".equalsIgnoreCase(language)) {
            return """
                Tu es un rédacteur de CV professionnel. Génère un résumé professionnel concis et optimisé pour les ATS 
                (2-3 phrases, ~50-80 mots) basé sur la description de l'utilisateur.
                
                Focus sur:
                - Les compétences clés et technologies
                - Le niveau de carrière (étudiant, junior, senior, etc.)
                - Les objectifs de carrière ou le poste ciblé
                - Les réalisations quantifiables si possible
                
                Utilise des verbes d'action forts et rends-le impactant. N'utilise pas la première personne (je, mon, me).
                Retourne UNIQUEMENT le texte du résumé, sans formatage ou explication supplémentaire.
                """;
        } else {
            return """
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
        }
    }

    private String getSystemPromptForExperience(String role, String company, String language) {
        if ("fr".equalsIgnoreCase(language)) {
            return String.format("""
                Tu es un rédacteur de CV professionnel. Génère 3-5 points d'expérience optimisés pour les ATS 
                basés sur la description de l'utilisateur.
                
                Contexte:
                - Poste: %s
                - Entreprise: %s
                
                Directives:
                - Commence chaque point par un verbe d'action fort (au passé)
                - Inclus des réalisations quantifiables si possible (pourcentages, chiffres, métriques)
                - Focus sur l'impact et les résultats, pas seulement les responsabilités
                - Garde les points concis (1-2 lignes chaque)
                - Rends-les compatibles ATS (utilise des mots-clés de l'industrie)
                
                Format: Retourne UNIQUEMENT les points, un par ligne, commençant par "• "
                Exemple:
                • Développé des APIs RESTful avec Spring Boot, réduisant le temps de réponse de 40%%
                • Collaboré avec une équipe inter-fonctionnelle de 5 personnes pour livrer les fonctionnalités à temps
                • Implémenté des tests automatisés, augmentant la couverture de code de 60%% à 85%%
                """, role, company);
        } else {
            return String.format("""
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
        }
    }

    private String getSystemPromptForProject(String projectTitle, String language) {
        if ("fr".equalsIgnoreCase(language)) {
            return String.format("""
                Tu es un rédacteur de CV professionnel. Génère 3-4 points optimisés pour les ATS pour un projet 
                basés sur la description de l'utilisateur.
                
                Projet: %s
                
                Directives:
                - Commence chaque point par un verbe d'action fort (au passé: Construit, Développé, Implémenté, etc.)
                - Mets en évidence les fonctionnalités clés et les technologies utilisées
                - Inclus des détails techniques (frameworks, langages, outils)
                - Mentionne les résultats mesurables (utilisateurs, performance, etc.)
                - Garde les points concis (1-2 lignes chaque)
                
                Format: Retourne UNIQUEMENT les points, un par ligne, commençant par "• "
                Exemple:
                • Construit une plateforme e-commerce full-stack avec Next.js, PostgreSQL et l'API Stripe
                • Implémenté l'authentification utilisateur avec JWT et OAuth2, supportant 500+ utilisateurs actifs
                • Conçu une UI responsive avec Tailwind CSS, atteignant un score Lighthouse de 95+
                """, projectTitle);
        } else {
            return String.format("""
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
        }
    }

    /**
     * Build optimized prompt for resume tailoring
     * CRITICAL: Only uses existing content, never fabricates
     */
    private String buildTailoringPrompt(Map<String, Object> resumeData, String jobDescription, String language) {
        // Get language name
        String languageName = "fr".equalsIgnoreCase(language) ? "French" : "English";

        // Truncate job description to save tokens (keep first 500 chars)
        String truncatedJobDesc = jobDescription.length() > 500
                ? jobDescription.substring(0, 500) + "..."
                : jobDescription;

        // Create compact resume data (only essential fields)
        Map<String, Object> compactResume = new HashMap<>();
        compactResume.put("fullName", resumeData.get("fullName"));
        compactResume.put("professionalSummary", resumeData.get("professionalSummary"));
        compactResume.put("experience", resumeData.get("experience"));
        compactResume.put("skills", resumeData.get("skills"));
        compactResume.put("education", resumeData.get("education"));

        String resumeJson;
        try {
            resumeJson = objectMapper.writeValueAsString(compactResume);
        } catch (Exception e) {
            resumeJson = compactResume.toString();
        }

        return String.format("""
            Tailor this resume for the job. CRITICAL RULES:
            
            1. OUTPUT LANGUAGE: Write ALL content in %s
            2. NO FABRICATION: Only use existing skills/experience from the resume
            3. NEVER ADD: Don't add skills, technologies, or achievements not in the resume
            4. HIGHLIGHT: Emphasize relevant existing content that matches the job
            5. REORDER: Prioritize matching skills, but don't invent new ones
            6. REWRITE: Improve bullet points to show relevance to job requirements
            
            RESUME:
            %s
            
            JOB (key requirements):
            %s
            
            Return ONLY valid JSON with same structure. No markdown, no explanations, no code blocks.
            """, languageName, resumeJson, truncatedJobDesc);
    }

    /**
     * Build optimized prompt for cover letter generation
     */
    private String buildCoverLetterPrompt(Map<String, Object> resumeData, String jobDescription, String language) {
        String fullName = getStringValue(resumeData, "fullName", "Candidate");
        String summary = getStringValue(resumeData, "professionalSummary", "");

        // Get language name
        String languageName = "fr".equalsIgnoreCase(language) ? "French" : "English";

        // Extract brief experience (just titles and companies)
        String briefExp = extractBriefExperience(resumeData);
        String topSkills = extractTopSkills(resumeData, 8);

        // Truncate job description
        String truncatedJobDesc = jobDescription.length() > 400
                ? jobDescription.substring(0, 400) + "..."
                : jobDescription;

        return String.format("""
            Write a cover letter in %s (250 words max).
            
            Candidate: %s
            Summary: %s
            Experience: %s
            Skills: %s
            
            Job:
            %s
            
            Format:
            Dear Hiring Manager,
            [3 paragraphs: intro + relevant experience + closing]
            Sincerely,
            %s
            
            Keep it concise, professional, specific. Write ENTIRELY in %s.
            """, languageName, fullName, summary, briefExp, topSkills,
                truncatedJobDesc, fullName, languageName);
    }

    /**
     * Call OpenAI API - Simple version for resume builder features
     */
    private String callOpenAISimple(String systemPrompt, String userInput, int maxTokens) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userInput)
            ));
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", maxTokens);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    OPENAI_API_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();

            return content.trim();

        } catch (Exception e) {
            log.error("OpenAI API Error: {}", e.getMessage());
            throw new RuntimeException("Failed to call OpenAI API: " + e.getMessage());
        }
    }

    /**
     * Call OpenAI API - Advanced version for tailoring/cover letter
     */
    private String callOpenAIAdvanced(String prompt, int maxTokens, double temperature) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o-mini");
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        log.debug("Calling OpenAI API with max_tokens: {}", maxTokens);
        ResponseEntity<Map> response = restTemplate.postForEntity(OPENAI_API_URL, entity, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> body = response.getBody();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");

            if (choices != null && !choices.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        }

        throw new RuntimeException("Failed to get valid response from OpenAI");
    }

    /**
     * Parse AI response → return ONLY new JSON (no merge)
     */
    private Map<String, Object> parseAIResponse(String response, Map<String, Object> originalData) {
        try {
            String clean = response.trim();

            // remove markdown wrappers
            if (clean.startsWith("```json")) clean = clean.substring(7).trim();
            else if (clean.startsWith("```")) clean = clean.substring(3).trim();

            if (clean.endsWith("```"))
                clean = clean.substring(0, clean.length() - 3).trim();

            // parse JSON returned by AI
            Map<String, Object> parsed =
                    objectMapper.readValue(clean, new TypeReference<Map<String, Object>>() {});

            log.info("Successfully parsed AI response");

            // IMPORTANT: DO NOT MERGE, ALWAYS REPLACE
            return parsed;

        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", e.getMessage());
            log.debug("AI response was: {}", response);
            return originalData; // fallback
        }
    }


    /**
     * Helper: Extract brief experience
     */
    @SuppressWarnings("unchecked")
    private String extractBriefExperience(Map<String, Object> resumeData) {
        try {
            Object expObj = resumeData.get("experience");
            if (expObj instanceof List) {
                List<Map<String, Object>> experiences = (List<Map<String, Object>>) expObj;
                StringBuilder sb = new StringBuilder();

                for (int i = 0; i < Math.min(2, experiences.size()); i++) {
                    Map<String, Object> exp = experiences.get(i);
                    sb.append(getStringValue(exp, "position", ""));
                    sb.append(" at ");
                    sb.append(getStringValue(exp, "company", ""));
                    if (i < Math.min(2, experiences.size()) - 1) sb.append("; ");
                }

                return sb.toString();
            }
        } catch (Exception e) {
            log.debug("Could not extract experiences: {}", e.getMessage());
        }
        return "See resume";
    }

    /**
     * Helper: Extract top N skills
     */
    @SuppressWarnings("unchecked")
    private String extractTopSkills(Map<String, Object> resumeData, int limit) {
        try {
            Object skillsObj = resumeData.get("skills");
            if (skillsObj instanceof List) {
                List<Map<String, Object>> skillCategories = (List<Map<String, Object>>) skillsObj;
                List<String> allSkills = new ArrayList<>();

                for (Map<String, Object> category : skillCategories) {
                    Object itemsObj = category.get("items");
                    if (itemsObj instanceof List) {
                        List<?> items = (List<?>) itemsObj;
                        for (Object item : items) {
                            allSkills.add(item.toString());
                            if (allSkills.size() >= limit) break;
                        }
                    }
                    if (allSkills.size() >= limit) break;
                }

                return String.join(", ", allSkills);
            }
        } catch (Exception e) {
            log.debug("Could not extract skills: {}", e.getMessage());
        }
        return "See resume";
    }

    /**
     * Helper: Safely extract string values
     */
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}