package org.example.resai.service;

import com.fasterxml.jackson.core.type.TypeReference;
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
     * NOW WITH LANGUAGE SUPPORT!
     */
    public String generateSummary(String userInput, String language) {
        String systemPrompt = getSystemPromptForSummary(language);
        return callOpenAI(systemPrompt, userInput);
    }

    /**
     * Generate experience bullets based on user's description
     * NOW WITH LANGUAGE SUPPORT!
     */
    public String generateExperienceBullets(String userInput, Map<String, String> context, String language) {
        String role = context != null ? context.getOrDefault("role", "") : "";
        String company = context != null ? context.getOrDefault("company", "") : "";

        String systemPrompt = getSystemPromptForExperience(role, company, language);
        return callOpenAI(systemPrompt, userInput);
    }

    /**
     * Generate project bullets based on user's description
     * NOW WITH LANGUAGE SUPPORT!
     */
    public String generateProjectBullets(String userInput, Map<String, String> context, String language) {
        String projectTitle = context != null ? context.getOrDefault("projectTitle", "") : "";

        String systemPrompt = getSystemPromptForProject(projectTitle, language);
        return callOpenAI(systemPrompt, userInput);
    }

    // ============ PRIVATE HELPER METHODS ============

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
     * Call OpenAI API with system prompt and user input
     */
    private String callOpenAI(String systemPrompt, String userInput) {
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
            requestBody.put("max_tokens", 500);

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
            e.printStackTrace();
            throw new RuntimeException("Failed to call OpenAI API: " + e.getMessage());
        }
    }

    public Map<String, Object> tailorResume(Map<String, Object> resumeData, String jobDescription) {
        try {
            String prompt = buildTailoringPrompt(resumeData, jobDescription);
            String response = callOpenAI(prompt, 4000);
            return parseAIResponse(response, resumeData);
        } catch (Exception e) {
            System.err.println("Error tailoring resume: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to tailor resume: " + e.getMessage());
        }
    }

    public String generateCoverLetter(Map<String, Object> resumeData, String jobDescription) {
        try {
            String prompt = buildCoverLetterPrompt(resumeData, jobDescription);
            return callOpenAI(prompt, 1500);
        } catch (Exception e) {
            System.err.println("Error generating cover letter: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to generate cover letter: " + e.getMessage());
        }
    }

    private String buildTailoringPrompt(Map<String, Object> resumeData, String jobDescription) {
        ObjectMapper mapper = new ObjectMapper();
        String resumeJson;
        try {
            resumeJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resumeData);
        } catch (Exception e) {
            resumeJson = resumeData.toString();
        }

        return String.format("""
                THE GOD-LEVEL RESUME TAILORING PROMPT
                
                                                                                          You are an elite resume-optimization system. Your job is to rewrite and refine the provided resume only using information already contained in the resume JSON.
                                                                                          You must tailor it to the job description without adding, inventing, or assuming ANY new skills, experience, achievements, or technologies.
                
                                                                                          RESUME (JSON):
                
                                                                                          %s
                
                                                                                          JOB DESCRIPTION:
                
                                                                                          %s
                
                                                                                          MISSION RULES (READ CAREFULLY):
                
                                                                                          Truth Only:
                
                                                                                          You may rewrite existing content for clarity, impact, and alignment.
                
                                                                                          You may reorder or rephrase content.
                
                                                                                          You may NOT add any skill, tool, technology, responsibility, certification, achievement, or detail that is not explicitly present somewhere in the JSON.
                
                                                                                          Summary Optimization:
                
                                                                                          Rewrite the professionalSummary to emphasize elements already in the JSON that match the job.
                
                                                                                          Do not introduce new skills or claims.
                
                                                                                          Experience Optimization:
                
                                                                                          Rewrite bullet points using stronger action verbs.
                
                                                                                          Emphasize responsibilities and achievements that already exist and align with the job.
                
                                                                                          You may combine, condense, or clarify—but never fabricate or infer new work.
                
                                                                                          Skills Optimization:
                
                                                                                          Reorder and group existing skills to match the job description’s priorities.
                
                                                                                          You may only use skills that are already in the JSON.
                
                                                                                          You may NOT add missing skills even if the job description requires them.
                
                                                                                          Keyword Alignment:
                
                                                                                          Insert job-description keywords only when they truthfully match existing content.
                
                                                                                          Never force or fabricate alignment.
                
                                                                                          Structural Integrity:
                
                                                                                          Output MUST be a valid JSON object with the exact same structure, fields, and schema as the input.
                
                                                                                          No fields may be added or removed.
                
                                                                                          Do not change job titles, companies, dates, or education details.
                
                                                                                          OUTPUT FORMAT:
                
                                                                                          Return ONLY the optimized JSON.
                                                                                          No explanations, no commentary, no markdown, no code blocks.
            """, resumeJson, jobDescription);
    }

    private String buildCoverLetterPrompt(Map<String, Object> resumeData, String jobDescription) {
        String fullName = (String) resumeData.getOrDefault("fullName", "");
        String email = (String) resumeData.getOrDefault("email", "");
        String phone = (String) resumeData.getOrDefault("phone", "");
        String professionalSummary = (String) resumeData.getOrDefault("professionalSummary", "");

        // Extract experience if available
        StringBuilder experienceContext = new StringBuilder();
        if (resumeData.containsKey("experience")) {
            Object exp = resumeData.get("experience");
            if (exp instanceof List) {
                List<?> experiences = (List<?>) exp;
                for (Object e : experiences) {
                    if (e instanceof Map) {
                        Map<?, ?> expMap = (Map<?, ?>) e;
                        experienceContext.append("- ")
                                .append(expMap.get("position"))
                                .append(" at ")
                                .append(expMap.get("company"))
                                .append("\n");
                    }
                }
            }
        }

        return String.format("""
            You are a professional cover letter writer. Create a compelling, personalized cover letter.
            
            CANDIDATE INFORMATION:
            Name: %s
            Email: %s
            Phone: %s
            Professional Summary: %s
            
            Recent Experience:
            %s
            
            JOB DESCRIPTION:
            %s
            
            INSTRUCTIONS:
            1. Write a professional cover letter with this structure:
               - Opening paragraph: Express interest and briefly state why you're a great fit
               - Body (2 paragraphs): 
                 * Highlight 2-3 relevant achievements/experiences that match job requirements
                 * Show understanding of company needs and how you can address them
               - Closing: Express enthusiasm and call to action
            
            2. Style Guidelines:
               - Professional yet warm and authentic tone
               - Confident but not arrogant
               - Specific examples from experience
               - 300-400 words total
               - No placeholder company names - use "your organization" or "your team"
            
            3. Make it compelling by:
               - Using concrete examples and achievements
               - Showing genuine enthusiasm
               - Demonstrating knowledge of role requirements
               - Connecting candidate's experience to job needs
            
            Return ONLY the cover letter text (no subject line, no address block, just the letter body).
            Start with "Dear Hiring Manager," and end with "Sincerely,\n%s"
            """, fullName, email, phone, professionalSummary, experienceContext.toString(), jobDescription, fullName);
    }

    private String callOpenAI(String prompt, int maxTokens) throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o-mini");
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are a professional resume and career advisor expert."),
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", maxTokens);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(OPENAI_API_URL, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            throw new RuntimeException("Invalid response from OpenAI");
        } catch (Exception e) {
            System.err.println("OpenAI API Error: " + e.getMessage());
            throw new RuntimeException("Failed to call OpenAI API: " + e.getMessage());
        }
    }

    private Map<String, Object> parseAIResponse(String response, Map<String, Object> originalData) {
        try {
            // Clean the response
            String cleanResponse = response.trim();

            // Remove markdown code blocks if present
            if (cleanResponse.startsWith("```json")) {
                cleanResponse = cleanResponse.substring(7);
            } else if (cleanResponse.startsWith("```")) {
                cleanResponse = cleanResponse.substring(3);
            }

            if (cleanResponse.endsWith("```")) {
                cleanResponse = cleanResponse.substring(0, cleanResponse.length() - 3);
            }

            cleanResponse = cleanResponse.trim();

            // Parse JSON
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> parsedData = mapper.readValue(cleanResponse, new TypeReference<Map<String, Object>>() {});

            System.out.println("Successfully parsed AI response");
            return parsedData;
        } catch (Exception e) {
            System.err.println("Failed to parse AI response: " + e.getMessage());
            System.err.println("Response was: " + response);
            // Return original data if parsing fails
            return originalData;
        }
    }




}