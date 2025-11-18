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

    /**
     * Tailor resume data to match a specific job description
     * Returns the entire resume data structure with tailored content
     */
    public Map<String, Object> tailorResume(Map<String, Object> resumeData, String jobDescription, String language) {
        try {
            String systemPrompt = getSystemPromptForTailoring(language);

            // Prepare the user message with resume data and job description
            String userMessage = prepareResumeTailoringMessage(resumeData, jobDescription, language);

            // Call OpenAI with structured output request
            String aiResponse = callOpenAIForStructuredOutput(systemPrompt, userMessage);

            // Parse the JSON response
            return objectMapper.readValue(aiResponse, Map.class);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to tailor resume: " + e.getMessage());
        }
    }

    /**
     * Generate a cover letter based on resume and job description
     */
    public String generateCoverLetter(Map<String, Object> resumeData, String jobDescription, String language) {
        try {
            String systemPrompt = getSystemPromptForCoverLetter(language);
            String userMessage = prepareCoverLetterMessage(resumeData, jobDescription, language);

            return callOpenAI(systemPrompt, userMessage);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to generate cover letter: " + e.getMessage());
        }
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

    private String getSystemPromptForTailoring(String language) {
        if ("fr".equalsIgnoreCase(language)) {
            return """
                Tu es un expert en rédaction de CV et en optimisation ATS. Ta tâche est d'adapter un CV existant 
                pour correspondre parfaitement à une description de poste spécifique.
                
                Instructions:
                1. Analyse la description de poste pour identifier les compétences clés, mots-clés et qualifications requises
                2. Adapte le résumé professionnel pour s'aligner sur le poste
                3. Réorganise et reformule les points d'expérience pour mettre en évidence les compétences pertinentes
                4. Mets en avant les compétences techniques qui correspondent à l'offre
                5. Garde TOUTES les informations factuelles (dates, entreprises, diplômes) EXACTEMENT telles quelles
                6. Optimise pour les ATS en utilisant les mots-clés de la description de poste
                7. Maintiens un ton professionnel et des verbes d'action forts
                
                IMPORTANT: Retourne UNIQUEMENT un objet JSON valide avec la structure exacte du CV original.
                Ne pas ajouter de texte avant ou après le JSON. Pas de markdown, pas d'explication.
                """;
        } else {
            return """
                You are an expert resume writer and ATS optimization specialist. Your task is to tailor an existing resume 
                to perfectly match a specific job description.
                
                Instructions:
                1. Analyze the job description to identify key skills, keywords, and required qualifications
                2. Tailor the professional summary to align with the target role
                3. Reorder and rephrase experience bullets to highlight relevant skills
                4. Emphasize technical skills that match the job posting
                5. Keep ALL factual information (dates, companies, degrees) EXACTLY as they are
                6. Optimize for ATS by incorporating keywords from the job description
                7. Maintain professional tone and strong action verbs
                
                IMPORTANT: Return ONLY a valid JSON object with the exact structure of the original resume.
                Do not add any text before or after the JSON. No markdown, no explanation.
                """;
        }
    }

    private String getSystemPromptForCoverLetter(String language) {
        if ("fr".equalsIgnoreCase(language)) {
            return """
                Tu es un rédacteur professionnel de lettres de motivation. Crée une lettre de motivation convaincante 
                et personnalisée basée sur le CV du candidat et la description de poste.
                
                Structure de la lettre:
                1. Introduction: Mentionne le poste et exprime ton enthousiasme
                2. Corps (2-3 paragraphes):
                   - Mets en évidence les expériences et compétences pertinentes du CV
                   - Connecte les qualifications du candidat avec les exigences du poste
                   - Montre la compréhension de l'entreprise et du rôle
                   - Inclus des exemples de réalisations spécifiques
                3. Conclusion: Réitère l'intérêt et propose un prochain pas
                
                Directives:
                - Garde un ton professionnel mais engageant
                - Longueur: 3-4 paragraphes (250-350 mots)
                - Évite les clichés génériques
                - Utilise des détails spécifiques du CV
                - Adapte au poste et à l'entreprise
                
                Retourne UNIQUEMENT le texte de la lettre de motivation, pas de titre ou signature.
                """;
        } else {
            return """
                You are a professional cover letter writer. Create a compelling, personalized cover letter 
                based on the candidate's resume and the job description.
                
                Letter structure:
                1. Introduction: Mention the position and express enthusiasm
                2. Body (2-3 paragraphs):
                   - Highlight relevant experience and skills from the resume
                   - Connect the candidate's qualifications to the job requirements
                   - Demonstrate understanding of the company and role
                   - Include specific achievement examples
                3. Conclusion: Reiterate interest and suggest next steps
                
                Guidelines:
                - Keep a professional yet engaging tone
                - Length: 3-4 paragraphs (250-350 words)
                - Avoid generic clichés
                - Use specific details from the resume
                - Tailor to the job and company
                
                Return ONLY the cover letter text, no title or signature.
                """;
        }
    }

    private String prepareResumeTailoringMessage(Map<String, Object> resumeData, String jobDescription, String language) {
        try {
            String resumeJson = objectMapper.writeValueAsString(resumeData);

            if ("fr".equalsIgnoreCase(language)) {
                return String.format("""
                    CV ACTUEL (JSON):
                    %s
                    
                    DESCRIPTION DU POSTE:
                    %s
                    
                    Adapte ce CV pour correspondre à cette description de poste. Retourne le CV modifié au format JSON 
                    avec la même structure exacte.
                    """, resumeJson, jobDescription);
            } else {
                return String.format("""
                    CURRENT RESUME (JSON):
                    %s
                    
                    JOB DESCRIPTION:
                    %s
                    
                    Tailor this resume to match this job description. Return the modified resume in JSON format 
                    with the exact same structure.
                    """, resumeJson, jobDescription);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare resume tailoring message: " + e.getMessage());
        }
    }

    private String prepareCoverLetterMessage(Map<String, Object> resumeData, String jobDescription, String language) {
        try {
            String resumeJson = objectMapper.writeValueAsString(resumeData);

            if ("fr".equalsIgnoreCase(language)) {
                return String.format("""
                    CV DU CANDIDAT (JSON):
                    %s
                    
                    DESCRIPTION DU POSTE:
                    %s
                    
                    Crée une lettre de motivation professionnelle basée sur ce CV et cette description de poste.
                    """, resumeJson, jobDescription);
            } else {
                return String.format("""
                    CANDIDATE'S RESUME (JSON):
                    %s
                    
                    JOB DESCRIPTION:
                    %s
                    
                    Create a professional cover letter based on this resume and job description.
                    """, resumeJson, jobDescription);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare cover letter message: " + e.getMessage());
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
            requestBody.put("max_tokens", 1500);

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

    /**
     * Call OpenAI API for structured JSON output (used for resume tailoring)
     */
    private String callOpenAIForStructuredOutput(String systemPrompt, String userInput) {
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
            requestBody.put("temperature", 0.5);
            requestBody.put("max_tokens", 3000);
            requestBody.put("response_format", Map.of("type", "json_object"));

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
            throw new RuntimeException("Failed to call OpenAI API for structured output: " + e.getMessage());
        }
    }
}