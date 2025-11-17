package org.example.resai.controller;

import lombok.RequiredArgsConstructor;
import org.example.resai.dto.ResumeReq;
import org.example.resai.dto.ResumeRes;
import org.example.resai.model.Resume;
import org.example.resai.model.User;
import org.example.resai.service.ResumeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
@CrossOrigin(
        origins = {"http://localhost:3000", "https://res-ai-frontend.vercel.app"},
        allowCredentials = "true",
        allowedHeaders = "*",
        maxAge = 3600
)
public class ResumeController {

    private final ResumeService resumeService;

    // CREATE
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody ResumeReq dto,
            @AuthenticationPrincipal User user) {

        if (user == null) return ResponseEntity.status(401).build();

        ResumeRes resumeRes = resumeService.createResume(user, dto);
        // ResumeRes is just a DTO → extract the actual Resume entity from the service another way
        // Since we can't get it from ResumeRes, we re-fetch it safely
        Resume createdResume = resumeService.getResumeById(resumeRes.getId(), user.getId());

        return ResponseEntity.ok(Map.of("resume", createdResume));
    }

    // GET ALL
    @GetMapping
    public ResponseEntity<List<Resume>> getAllResumes(@AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(resumeService.getResumesByUserId(user.getId()));
    }

    // GET ONE
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getResumeById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        if (user == null) return ResponseEntity.status(401).build();

        Resume resume = resumeService.getResumeById(id, user.getId());
        if (resume == null) return ResponseEntity.status(404).build();

        return ResponseEntity.ok(Map.of("resume", resume));
    }

    // UPDATE — uses your existing Map-based updateResume()
    @PutMapping("/{id}")  // ← Critical fix: standard path
    public ResponseEntity<Map<String, Object>> updateResume(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal User user) {

        if (user == null) return ResponseEntity.status(401).build();

        Resume updated = resumeService.updateResume(id, user.getId(), payload);
        if (updated == null) return ResponseEntity.status(404).build();

        return ResponseEntity.ok(Map.of("resume", updated));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        if (user == null) return ResponseEntity.status(401).build();

        boolean deleted = resumeService.deleteResume(id, user.getId());
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    // TAILOR
    @PostMapping("/{id}/tailor")
    public ResponseEntity<Map<String, Object>> tailorResume(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {

        if (user == null) return ResponseEntity.status(401).build();

        String jd = body.get("jobDescription");
        if (jd == null || jd.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "jobDescription is required"));
        }

        Resume tailored = resumeService.tailorResume(id, user.getId(), jd);
        return ResponseEntity.ok(Map.of("resume", tailored));
    }

    // COVER LETTER
    @PostMapping("/{id}/cover-letter")
    public ResponseEntity<Map<String, Object>> generateCoverLetter(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {

        if (user == null) return ResponseEntity.status(401).build();

        String jd = body.get("jobDescription");
        if (jd == null || jd.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "jobDescription is required"));
        }

        String coverLetter = resumeService.generateCoverLetter(id, user.getId(), jd);
        return ResponseEntity.ok(Map.of("coverLetter", coverLetter));
    }
}