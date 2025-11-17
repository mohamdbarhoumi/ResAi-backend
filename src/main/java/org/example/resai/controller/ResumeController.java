package org.example.resai.controller;

import lombok.RequiredArgsConstructor;
import org.example.resai.dto.ResumeReq;
import org.example.resai.dto.ResumeRes;
import org.example.resai.model.Resume;
import org.example.resai.model.User;
import org.example.resai.service.ResumeService;
import org.example.resai.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resumes")
// CORS is already handled globally in SecConfig.java → NO @CrossOrigin here!
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final UserService userService;

    @PostMapping("/create")
    public ResponseEntity<ResumeRes> create(
            @RequestBody ResumeReq dto,
            @AuthenticationPrincipal User user) {
        ResumeRes createdResume = resumeService.createResume(user, dto);
        return ResponseEntity.ok(createdResume);
    }

    @GetMapping
    public ResponseEntity<List<Resume>> getAllResumes(@AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(resumeService.getResumesByUserId(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getResumeById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Resume resume = resumeService.getResumeById(id, user.getId());
        if (resume == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Resume not found"));
        }

        return ResponseEntity.ok(Map.of("resume", resume));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateResume(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Resume updated = resumeService.updateResume(id, user.getId(), payload);
        if (updated == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Resume not found or unauthorized"));
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deleteResume(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        boolean deleted = resumeService.deleteResume(id, user.getId());
        if (!deleted) {
            return ResponseEntity.status(404).body(Map.of("error", "Resume not found or unauthorized"));
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    // FIXED: Tailor Resume
    @PostMapping("/{id}/tailor")
    public ResponseEntity<?> tailorResume(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String jobDescription = request.get("jobDescription");
        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "jobDescription is required"));
        }

        try {
            Resume tailored = resumeService.tailorResume(id, user.getId(), jobDescription);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Resume tailored successfully",
                    "resume", tailored
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Tailoring failed: " + e.getMessage()));
        }
    }

    // FIXED: Generate Cover Letter
    @PostMapping("/{id}/cover-letter")
    public ResponseEntity<?> generateCoverLetter(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String jobDescription = request.get("jobDescription");
        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "jobDescription is required"));
        }

        try {
            String coverLetter = resumeService.generateCoverLetter(id, user.getId(), jobDescription);
            return ResponseEntity.ok(Map.of("coverLetter", coverLetter));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Cover letter generation failed: " + e.getMessage()));
        }
    }
}