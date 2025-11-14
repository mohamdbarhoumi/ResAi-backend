package org.example.resai.service;

import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepo resumeRepo;
    private final ResumeMapper resumeMapper;
    private final AiService aiService;


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

    // Get a single resume by ID (only if it belongs to the user)
    public Resume getResumeById(Long id, Long userId) {
        Optional<Resume> resume = resumeRepo.findByIdAndUserId(id, userId);
        return resume.orElse(null);
    }

    // Update an existing resume
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
            e.printStackTrace();
            throw new RuntimeException("Failed to update resume: " + e.getMessage());
        }
    }

    // Delete a resume
    @Transactional
    public boolean deleteResume(Long id, Long userId) {
        Optional<Resume> resume = resumeRepo.findByIdAndUserId(id, userId);

        if (resume.isEmpty()) {
            return false;
        }

        resumeRepo.delete(resume.get());
        return true;
    }



    @Transactional
    public Resume tailorResume(Long resumeId, Long userId, String jobDescription) {
        // Find and validate resume
        Resume resume = resumeRepo.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new RuntimeException("Resume not found or unauthorized"));

        // Get current resume data
        Map<String, Object> currentData = resume.getData();

        if (currentData == null || currentData.isEmpty()) {
            throw new RuntimeException("Resume has no data to tailor");
        }

        // Call OpenAI to tailor the resume
        Map<String, Object> tailoredData = aiService.tailorResume(currentData, jobDescription);

        // Update resume with tailored data
        resume.setData(tailoredData);
        resume.setVersion(resume.getVersion() + 1);
        resume.setUpdatedAt(LocalDateTime.now());

        // Optionally store metadata about the tailoring
        Map<String, Object> metadata = resume.getAiMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put("lastTailoredAt", LocalDateTime.now().toString());
        metadata.put("tailoredFor", jobDescription.substring(0, Math.min(100, jobDescription.length())));
        resume.setAiMetadata(metadata);

        return resumeRepo.save(resume);
    }

    public String generateCoverLetter(Long resumeId, Long userId, String jobDescription) {
        // Find and validate resume
        Resume resume = resumeRepo.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new RuntimeException("Resume not found or unauthorized"));

        Map<String, Object> resumeData = resume.getData();

        if (resumeData == null || resumeData.isEmpty()) {
            throw new RuntimeException("Resume has no data to generate cover letter");
        }

        // Call OpenAI to generate cover letter
        return aiService.generateCoverLetter(resumeData, jobDescription);
    }


}