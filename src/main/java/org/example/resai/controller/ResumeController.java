package org.example.resai.controller;

import lombok.RequiredArgsConstructor;
import org.example.resai.dto.ResumeReq;
import org.example.resai.dto.ResumeRes;
import org.example.resai.model.Resume;
import org.example.resai.model.User;
import org.example.resai.security.JwtUtils;
import org.example.resai.service.ResumeService;
import org.example.resai.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
@CrossOrigin(
        origins = {
                "http://localhost:3000",
                "https://res-ai-frontend.vercel.app"
        },
        allowedHeaders = {"Authorization", "Content-Type"},
        exposedHeaders = {"Authorization"},
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS}
)
public class ResumeController {

    private final ResumeService resumeService;
    private final UserService userService;
    private final JwtUtils jwtUtils;

    private User getUserFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return null;

        String token = authHeader.substring(7);
        String email = jwtUtils.extractEmail(token);
        return userService.findByEmail(email);
    }

    @PostMapping("/create")
    public ResponseEntity<ResumeRes> create(
            @RequestBody ResumeReq dto,
            @RequestHeader("Authorization") String authHeader) {

        User user = getUserFromToken(authHeader);
        if (user == null)
            return ResponseEntity.status(401).body(null);

        ResumeRes createdResume = resumeService.createResume(user, dto);
        return ResponseEntity.ok(createdResume);
    }

    @GetMapping
    public ResponseEntity<?> getAllResumes(@RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null)
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

            List<Resume> resumes = resumeService.getResumesByUserId(user.getId());
            return ResponseEntity.ok(resumes);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getResumeById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        try {
            User user = getUserFromToken(authHeader);
            if (user == null)
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

            Resume resume = resumeService.getResumeById(id, user.getId());
            if (resume == null)
                return ResponseEntity.status(404).body(Map.of("error", "Resume not found"));

            return ResponseEntity.ok(Map.of("resume", resume));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            @RequestHeader("Authorization") String authHeader) {

        User user = getUserFromToken(authHeader);
        if (user == null)
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        Resume updated = resumeService.updateResume(id, user.getId(), payload);
        if (updated == null)
            return ResponseEntity.status(404).body(Map.of("error", "Resume not found"));

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        User user = getUserFromToken(authHeader);
        if (user == null)
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        boolean deleted = resumeService.deleteResume(id, user.getId());
        if (!deleted)
            return ResponseEntity.status(404).body(Map.of("error", "Resume not found"));

        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{id}/tailor")
    public ResponseEntity<?> tailor(
            @PathVariable Long id,
            @RequestBody Map<String, String> req,
            @RequestHeader("Authorization") String authHeader) {

        User user = getUserFromToken(authHeader);
        if (user == null)
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        String jd = req.get("jobDescription");
        if (jd == null || jd.isEmpty())
            return ResponseEntity.status(400).body(Map.of("error", "Job description required"));

        return ResponseEntity.ok(
                Map.of(
                        "resume", resumeService.tailorResume(id, user.getId(), jd),
                        "success", true
                )
        );
    }

    @PostMapping("/{id}/cover-letter")
    public ResponseEntity<?> coverLetter(
            @PathVariable Long id,
            @RequestBody Map<String, String> req,
            @RequestHeader("Authorization") String authHeader) {

        User user = getUserFromToken(authHeader);
        if (user == null)
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        String jd = req.get("jobDescription");
        if (jd == null)
            return ResponseEntity.status(400).body(Map.of("error", "Missing job description"));

        return ResponseEntity.ok(
                Map.of(
                        "coverLetter", resumeService.generateCoverLetter(id, user.getId(), jd),
                        "success", true
                )
        );
    }
}
