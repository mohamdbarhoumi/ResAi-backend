package org.example.resai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.resai.dto.ResumeReq;
import org.example.resai.dto.ResumeRes;
import org.example.resai.dto.ResumeSum;
import org.example.resai.mapper.ResumeMapper;
import org.example.resai.model.Resume;
import org.example.resai.model.User;
import org.example.resai.repository.ResumeRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepo resumeRepo;
    private final ResumeMapper resumeMapper;
    private final AiService aiService;

    // ===== EXISTING METHODS =====

    public ResumeRes createResume(User user, ResumeReq dto) {
        if (user == null) {
            throw new IllegalArgumentException("Authenticated user cannot be null");
        }

        Resume resume = resumeMapper.fromRequestDTO(dto, user.getId());
        resume.setUser(user);

        Resume saved = resumeRepo.save(resume);
        return resumeMapper.toResponseDTO(saved);
    }

    public List<ResumeSum> getUserResumes(User user) {
        if (user == null) return List.of();

        return resumeRepo.findByUserId(user.getId())
                .stream()
                .map(r -> {
                    ResumeSum dto = new ResumeSum();
                    dto.setId(r.getId());
                    dto.setTitle(r.getTitle());
                    dto.setUpdatedAt(r.getUpdatedAt());
                    return dto;
                })
                .toList();
    }

    public List<Resume> getResumesByUserId(Long userId) {
        return resumeRepo.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public Resume getResumeById(Long id, Long userId) {
        Optional<Resume> resume = resumeRepo.findByIdAndUserId(id, userId);
        return resume.orElse(null);
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Resume updateResume(Long id, Long userId, Map<String, Object> payload) {
        Optional<Resume> existingResume = resumeRepo.findByIdAndUserId(id, userId);

        if (existingResume.isEmpty()) {
            return null;
        }

        Resume resume = existingResume.get();

        try {
            // Update title if provided
            if (payload.containsKey("title")) {
                String title = (String) payload.get("title");
                if (title != null && !title.trim().isEmpty()) {
                    resume.setTitle(title);
                }
            }

            // Update data if provided
            if (payload.containsKey("data")) {
                Object dataObj = payload.get("data");
                if (dataObj instanceof Map) {
                    resume.setData((Map<String, Object>) dataObj);
                }
            }

            // Update aiMetadata if provided
            if (payload.containsKey("aiMetadata") && payload.get("aiMetadata") != null) {
                Object metadataObj = payload.get("aiMetadata");
                if (metadataObj instanceof Map) {
                    resume.setAiMetadata((Map<String, Object>) metadataObj);
                }
            }

            // Increment version
            resume.setVersion(resume.getVersion() + 1);

            return resumeRepo.save(resume);
        } catch (Exception e) {
            log.error("Failed to update resume: {}", e.getMessage());
            throw new RuntimeException("Failed to update resume: " + e.getMessage());
        }
    }

    @Transactional
    public boolean deleteResume(Long id, Long userId) {
        Optional<Resume> resume = resumeRepo.findByIdAndUserId(id, userId);

        if (resume.isEmpty()) {
            return false;
        }

        resumeRepo.delete(resume.get());
        return true;
    }

    // ===== UPDATED TAILORING METHODS (with language support) =====

    /**
     * Tailor resume to match job description using AI
     * NOW WITH LANGUAGE SUPPORT (English/French)
     */
    @Transactional
    public Resume tailorResume(Long resumeId, Long userId, String jobDescription) {
        log.info("Tailoring resume {} for user {}", resumeId, userId);

        // Find and validate resume
        Resume resume = resumeRepo.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new RuntimeException("Resume not found or unauthorized"));

        // Get current resume data
        Map<String, Object> currentData = resume.getData();

        if (currentData == null || currentData.isEmpty()) {
            throw new RuntimeException("Resume has no data to tailor");
        }

        // Get the resume language (default to "en" if not specified)
        String language = resume.getLanguage();
        if (language == null || language.trim().isEmpty()) {
            language = "en";
        }

        log.info("Tailoring resume in language: {}", language);

        // Call AI service to tailor the resume WITH LANGUAGE
        Map<String, Object> tailoredData = aiService.tailorResume(currentData, jobDescription, language);

        // Update resume with tailored data
        resume.setData(tailoredData);
        resume.setVersion(resume.getVersion() + 1);
        resume.setUpdatedAt(LocalDateTime.now());

        // Store metadata about the tailoring
        Map<String, Object> metadata = resume.getAiMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put("lastTailoredAt", LocalDateTime.now().toString());
        metadata.put("tailoredFor", jobDescription.substring(0, Math.min(200, jobDescription.length())) + "...");
        metadata.put("tailoredLanguage", language);
        resume.setAiMetadata(metadata);

        Resume saved = resumeRepo.save(resume);
        log.info("Resume {} tailored successfully in language: {}", resumeId, language);

        return saved;
    }

    /**
     * Generate cover letter based on resume and job description
     * NOW WITH LANGUAGE SUPPORT (English/French)
     */
    public String generateCoverLetter(Long resumeId, Long userId, String jobDescription) {
        log.info("Generating cover letter for resume {} and user {}", resumeId, userId);

        // Find and validate resume
        Resume resume = resumeRepo.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new RuntimeException("Resume not found or unauthorized"));

        Map<String, Object> resumeData = resume.getData();

        if (resumeData == null || resumeData.isEmpty()) {
            throw new RuntimeException("Resume has no data to generate cover letter");
        }

        // Get the resume language (default to "en" if not specified)
        String language = resume.getLanguage();
        if (language == null || language.trim().isEmpty()) {
            language = "en";
        }

        log.info("Generating cover letter in language: {}", language);

        // Call AI service to generate cover letter WITH LANGUAGE
        String coverLetter = aiService.generateCoverLetter(resumeData, jobDescription, language);

        log.info("Cover letter generated successfully for resume {} in language: {}", resumeId, language);
        return coverLetter;
    }
}