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

import java.time.LocalDateTime;
import java.util.*;

/**
 * AiService — Improved JSON safety & normalization
 *
 * Key fixes:
 *  - parseAIResponse now strips code fences, parses JSON, maps synonyms,
 *    normalizes nested structures (skills, experience, projects, education),
 *    and only merges allowed fields into the existing resume data.
 *  - prevents invented/unexpected fields from being saved into resume.data
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String openAiApiKey;

    // Chat completions endpoint (update if you use responses API)
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    // Canonical top-level resume keys we accept and their expected types
    private static final Set<String> ALLOWED_TOP_LEVEL_KEYS = Set.of(
            "fullName",
            "professionalSummary",
            "summary",
            "experience",
            "skills",
            "education",
            "projects",
            "languages",
            "certificates",
            "professionalSummary", // alias repeated intentionally
            "location",
            "linkedin",
            "github",
            "website",
            "title"
    );

    // Some synonyms mapping to canonical keys
    private static final Map<String, String> SYNONYMS = Map.ofEntries(
            Map.entry("summary", "professionalSummary"),
            Map.entry("experiences", "experience"),
            Map.entry("workExperience", "experience"),
            Map.entry("projectsList", "projects"),
            Map.entry("skillsList", "skills"),
            Map.entry("educationList", "education")
    );

    // ============ Public AI features ============

    public String generateSummary(String userInput, String language) {
        String systemPrompt = getSystemPromptForSummary(language);
        return callOpenAISimple(systemPrompt, userInput, 500);
    }

    public String generateExperienceBullets(String userInput, Map<String, String> context, String language) {
        String role = context != null ? context.getOrDefault("role", "") : "";
        String company = context != null ? context.getOrDefault("company", "") : "";
        String systemPrompt = getSystemPromptForExperience(role, company, language);
        return callOpenAISimple(systemPrompt, userInput, 500);
    }

    public String generateProjectBullets(String userInput, Map<String, String> context, String language) {
        String projectTitle = context != null ? context.getOrDefault("projectTitle", "") : "";
        String systemPrompt = getSystemPromptForProject(projectTitle, language);
        return callOpenAISimple(systemPrompt, userInput, 500);
    }

    /**
     * Tailor resume: returns a Map with only normalized/allowed fields.
     * If AI response cannot be parsed or normalized, returns original resumeData.
     */
    public Map<String, Object> tailorResume(Map<String, Object> resumeData, String jobDescription, String language) {
        try {
            log.info("Starting resume tailoring in language: {}", language);
            String prompt = buildTailoringPrompt(resumeData, jobDescription, language);
            String rawResponse = callOpenAIAdvanced(prompt, 2000, 0.5);

            Map<String, Object> parsed = parseAIResponse(rawResponse, resumeData);
            Map<String, Object> normalized = normalizeAndMergeResumeData(parsed, resumeData);

            log.info("Resume tailoring completed successfully (normalized fields: {})", normalized.keySet());
            return normalized;
        } catch (Exception e) {
            log.error("Failed to tailor resume: {}", e.getMessage(), e);
            // fallback to original
            return resumeData;
        }
    }

    public String generateCoverLetter(Map<String, Object> resumeData, String jobDescription, String language) {
        try {
            log.info("Starting cover letter generation in language: {}", language);
            String prompt = buildCoverLetterPrompt(resumeData, jobDescription, language);
            String cover = callOpenAIAdvanced(prompt, 800, 0.7);
            log.info("Cover letter generated successfully");
            return cover.trim();
        } catch (Exception e) {
            log.error("Failed to generate cover letter: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate cover letter: " + e.getMessage());
        }
    }

    // ============ OpenAI call helpers (kept similar to your implementation) ============

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
            ResponseEntity<String> response = restTemplate.exchange(OPENAI_API_URL, HttpMethod.POST, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();

            return content.trim();
        } catch (Exception e) {
            log.error("OpenAI API Error (simple): {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call OpenAI API: " + e.getMessage());
        }
    }

    private String callOpenAIAdvanced(String prompt, int maxTokens, double temperature) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o-mini");
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
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
                Object content = message.get("content");
                if (content instanceof String) return (String) content;
            }
        }

        throw new RuntimeException("Failed to get valid response from OpenAI");
    }

    // ============ Parsing & Normalization ============

    /**
     * parseAIResponse:
     *  - Strips code fences / markdown wrappers
     *  - Parses JSON into Map
     *  - Returns parsed map (raw), NOT merged
     *  - Throws on parse failure
     */
    private Map<String, Object> parseAIResponse(String response, Map<String, Object> originalData) {
        try {
            if (response == null) {
                throw new IllegalArgumentException("AI response was null");
            }

            String clean = response.trim();

            // Strip common markdown wrappers
            if (clean.startsWith("```json")) {
                clean = clean.substring(7).trim();
            } else if (clean.startsWith("```")) {
                clean = clean.substring(3).trim();
            }
            if (clean.endsWith("```")) {
                clean = clean.substring(0, clean.length() - 3).trim();
            }

            // If the model accidentally outputs other commentary before/after JSON,
            // attempt to extract the first {...} block.
            String jsonCandidate = extractFirstJsonObject(clean);

            Map<String, Object> parsed = objectMapper.readValue(jsonCandidate, new TypeReference<Map<String, Object>>() {});
            log.info("Successfully parsed AI response keys: {}", parsed.keySet());
            return parsed;
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", e.getMessage(), e);
            // bubble up by throwing to let caller decide; caller may fallback to originalData
            throw new RuntimeException("Failed to parse AI response: " + e.getMessage(), e);
        }
    }

    /**
     * normalizeAndMergeResumeData:
     *  - Accepts parsed AI map (possibly containing synonyms)
     *  - Maps synonyms to canonical keys
     *  - Only permits allowed top-level keys
     *  - Normalizes nested types (skills, experience, projects, education)
     *  - Merges recognized keys into originalData (so fields not touched remain)
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeAndMergeResumeData(Map<String, Object> parsed, Map<String, Object> originalData) {
        Map<String, Object> result = new HashMap<>(originalData != null ? originalData : Map.of());
        if (parsed == null || parsed.isEmpty()) return result;

        // iterate parsed entries, map synonyms and whitelist
        for (Map.Entry<String, Object> e : parsed.entrySet()) {
            String rawKey = e.getKey();
            String key = SYNONYMS.getOrDefault(rawKey, rawKey);

            if (!ALLOWED_TOP_LEVEL_KEYS.contains(key) && !SYNONYMS.containsKey(rawKey)) {
                log.warn("Dropping unrecognized top-level key from AI: {}", rawKey);
                continue; // ignore unknown fields
            }

            Object value = e.getValue();

            try {
                switch (key) {
                    case "fullName":
                    case "title":
                    case "location":
                    case "github":
                    case "website":
                    case "linkedin":
                        // accept as string
                        if (value != null) result.put(key, value.toString());
                        break;

                    case "professionalSummary":
                        // accept string
                        if (value != null) result.put("professionalSummary", value.toString());
                        break;

                    case "skills":
                        List<Map<String, Object>> normalizedSkills = normalizeSkills(value);
                        if (!normalizedSkills.isEmpty()) result.put("skills", normalizedSkills);
                        break;

                    case "experience":
                        List<Map<String, Object>> normalizedExp = normalizeExperienceList(value);
                        if (!normalizedExp.isEmpty()) result.put("experience", normalizedExp);
                        break;

                    case "projects":
                        List<Map<String, Object>> normalizedProjects = normalizeProjects(value);
                        if (!normalizedProjects.isEmpty()) result.put("projects", normalizedProjects);
                        break;

                    case "education":
                        List<Map<String, Object>> normalizedEduc = normalizeEducation(value);
                        if (!normalizedEduc.isEmpty()) result.put("education", normalizedEduc);
                        break;

                    case "languages":
                        List<Map<String, Object>> normalizedLangs = normalizeLanguages(value);
                        if (!normalizedLangs.isEmpty()) result.put("languages", normalizedLangs);
                        break;

                    case "certificates":
                        List<Map<String, Object>> normalizedCerts = normalizeCertificates(value);
                        if (!normalizedCerts.isEmpty()) result.put("certificates", normalizedCerts);
                        break;

                    default:
                        // If matched via synonym mapping, we already mapped them. Do not add unknown keys.
                        log.debug("Unhandled key (shouldn't happen): {}", key);
                        break;
                }
            } catch (Exception ex) {
                log.warn("Normalization for key {} failed: {}", key, ex.getMessage());
            }
        }

        // stamp updatedAt/version are handled by service layer; here we just return normalized data
        return result;
    }

    // ============ NORMALIZERS FOR NESTED STRUCTURES ============

    /**
     * Normalize skills:
     * Accepts:
     *  - List<String> -> convert to List<{id?, name}>
     *  - List<Map> where map may be {id, name} or string items
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeSkills(Object val) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (val == null) return out;

        if (val instanceof List) {
            List<?> list = (List<?>) val;
            for (Object item : list) {
                if (item == null) continue;
                if (item instanceof String) {
                    out.add(Map.of("id", generateId(), "name", ((String) item).trim()));
                } else if (item instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) item;
                    String name = Optional.ofNullable(map.get("name")).map(Object::toString).orElse(null);
                    if (name == null) {
                        // maybe it's a simple map with single key, take its toString
                        name = map.toString();
                    }
                    Map<String, Object> normalized = new HashMap<>();
                    normalized.put("id", map.getOrDefault("id", generateId()));
                    normalized.put("name", name.trim());
                    out.add(normalized);
                } else {
                    out.add(Map.of("id", generateId(), "name", item.toString()));
                }
            }
        } else {
            // single value
            out.add(Map.of("id", generateId(), "name", val.toString()));
        }

        return out;
    }

    /**
     * Normalize experience list:
     * Expect list of objects; ensure each has canonical keys:
     * id, position, company, startDate, endDate, bullets (List<String>)
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeExperienceList(Object val) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (val == null) return out;
        if (!(val instanceof List)) return out;

        List<?> list = (List<?>) val;
        for (Object item : list) {
            if (!(item instanceof Map)) continue;
            Map<String, Object> m = (Map<String, Object>) item;
            Map<String, Object> normalized = new HashMap<>();
            normalized.put("id", m.getOrDefault("id", generateId()));
            normalized.put("position", firstNonNullString(m, "position", "title", "role"));
            normalized.put("company", firstNonNullString(m, "company", "employer"));
            normalized.put("startDate", m.getOrDefault("startDate", m.get("from")));
            normalized.put("endDate", m.getOrDefault("endDate", m.get("to")));
            // bullets might be string or list
            List<String> bullets = extractStringList(m.get("bullets"));
            if (bullets.isEmpty()) {
                bullets = extractBulletsFromString(firstNonNullString(m, "description", "summary", ""));
            }
            normalized.put("bullets", bullets);
            out.add(normalized);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeProjects(Object val) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (val == null) return out;
        if (!(val instanceof List)) return out;

        List<?> list = (List<?>) val;
        for (Object item : list) {
            if (!(item instanceof Map)) continue;
            Map<String, Object> m = (Map<String, Object>) item;
            Map<String, Object> normalized = new HashMap<>();
            normalized.put("id", m.getOrDefault("id", generateId()));
            normalized.put("title", firstNonNullString(m, "title", "name"));
            normalized.put("link", m.getOrDefault("link", ""));
            normalized.put("tech", m.getOrDefault("tech", m.getOrDefault("technologies", List.of())));
            normalized.put("bullets", extractStringList(m.get("bullets")));
            normalized.put("startDate", m.getOrDefault("startDate", m.get("from")));
            normalized.put("endDate", m.getOrDefault("endDate", m.get("to")));
            out.add(normalized);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeEducation(Object val) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (val == null) return out;
        if (!(val instanceof List)) return out;
        for (Object item : (List<?>) val) {
            if (!(item instanceof Map)) continue;
            Map<String, Object> m = (Map<String, Object>) item;
            Map<String, Object> normalized = new HashMap<>();
            normalized.put("id", m.getOrDefault("id", generateId()));
            normalized.put("institution", firstNonNullString(m, "institution", "school", "college"));
            normalized.put("degree", firstNonNullString(m, "degree", "qualification"));
            normalized.put("startDate", m.getOrDefault("startDate", m.get("from")));
            normalized.put("endDate", m.getOrDefault("endDate", m.get("to")));
            out.add(normalized);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeLanguages(Object val) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (val == null) return out;
        if (val instanceof List) {
            for (Object item : (List<?>) val) {
                if (item instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) item;
                    Map<String, Object> normalized = new HashMap<>();
                    normalized.put("id", m.getOrDefault("id", generateId()));
                    normalized.put("name", firstNonNullString(m, "name"));
                    normalized.put("proficiency", firstNonNullString(m, "proficiency", "level", ""));
                    out.add(normalized);
                } else {
                    out.add(Map.of("id", generateId(), "name", item.toString(), "proficiency", ""));
                }
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeCertificates(Object val) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (val == null) return out;
        if (val instanceof List) {
            for (Object item : (List<?>) val) {
                if (item instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) item;
                    Map<String, Object> normalized = new HashMap<>();
                    normalized.put("id", m.getOrDefault("id", generateId()));
                    normalized.put("name", firstNonNullString(m, "name", "title"));
                    normalized.put("issuer", m.getOrDefault("issuer", ""));
                    normalized.put("date", m.getOrDefault("date", ""));
                    out.add(normalized);
                } else {
                    out.add(Map.of("id", generateId(), "name", item.toString()));
                }
            }
        }
        return out;
    }

    // ============ Small helpers ============

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String firstNonNullString(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            if (m.containsKey(k) && m.get(k) != null) {
                return m.get(k).toString();
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private List<String> extractStringList(Object maybeList) {
        List<String> out = new ArrayList<>();
        if (maybeList == null) return out;
        if (maybeList instanceof List) {
            for (Object o : (List<?>) maybeList) {
                if (o != null) out.add(o.toString());
            }
        } else {
            out.add(maybeList.toString());
        }
        return out;
    }

    private List<String> extractBulletsFromString(String s) {
        if (s == null || s.isBlank()) return Collections.emptyList();
        // split by common separators, then trim and drop empties
        String[] parts = s.split("\\r?\\n|\\u2022|•|- ");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    /**
     * tries to extract the first {...} JSON object from a string (useful when model adds commentary)
     */
    private String extractFirstJsonObject(String input) {
        input = input.trim();
        // quick check: if starts with { and parseable, return it
        if (input.startsWith("{")) return input;
        int start = input.indexOf('{');
        int end = input.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return input.substring(start, end + 1);
        }
        // fallback: return original (will cause parse exception upstream)
        return input;
    }

    // ============ Prompts (kept as in your original) ============

    private String getSystemPromptForSummary(String language) {
        if ("fr".equalsIgnoreCase(language)) {
            return """
                Tu es un rédacteur de CV professionnel. Génère un résumé professionnel concis et optimisé pour les ATS 
                (2-3 phrases, ~50-80 mots) basé sur la description de l'utilisateur.
                Focus on key elements...
                Return ONLY the summary text.
                """;
        } else {
            return """
                You are a professional resume writer. Generate a concise, ATS-friendly professional summary (2-3 sentences).
                Return ONLY the summary text.
                """;
        }
    }

    private String getSystemPromptForExperience(String role, String company, String language) {
        if ("fr".equalsIgnoreCase(language)) {
            return String.format("Tu es un rédacteur... Role: %s Company: %s", role, company);
        } else {
            return String.format("You are a professional resume writer... Role: %s Company: %s", role, company);
        }
    }

    private String getSystemPromptForProject(String projectTitle, String language) {
        if ("fr".equalsIgnoreCase(language)) {
            return String.format("Tu es un rédacteur... Project: %s", projectTitle);
        } else {
            return String.format("You are a professional resume writer... Project: %s", projectTitle);
        }
    }

    private String buildTailoringPrompt(Map<String, Object> resumeData, String jobDescription, String language) {
        String languageName = "fr".equalsIgnoreCase(language) ? "French" : "English";
        String truncatedJobDesc = jobDescription.length() > 500 ? jobDescription.substring(0, 500) + "..." : jobDescription;

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

            Return ONLY valid JSON with same structure (allowed keys: fullName, professionalSummary, experience, skills, education, projects, languages, certificates, title, location, website, github, linkedin).
            No markdown, no explanations, no code blocks.
            """, languageName, resumeJson, truncatedJobDesc);
    }

    private String buildCoverLetterPrompt(Map<String, Object> resumeData, String jobDescription, String language) {
        String fullName = getStringValue(resumeData, "fullName", "Candidate");
        String summary = getStringValue(resumeData, "professionalSummary", "");
        String languageName = "fr".equalsIgnoreCase(language) ? "French" : "English";
        String briefExp = extractBriefExperience(resumeData);
        String topSkills = extractTopSkills(resumeData, 8);
        String truncatedJobDesc = jobDescription.length() > 400 ? jobDescription.substring(0, 400) + "..." : jobDescription;

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

    // small helpers reused for prompts
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
                        for (Object item : (List<?>) itemsObj) {
                            allSkills.add(item.toString());
                            if (allSkills.size() >= limit) break;
                        }
                    } else if (category.get("name") != null) {
                        allSkills.add(category.get("name").toString());
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

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
