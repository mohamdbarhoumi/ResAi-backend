package org.example.resai.controller;

import lombok.RequiredArgsConstructor;
<<<<<<< HEAD
import org.example.resai.dto.ResumeReq;
import org.example.resai.dto.ResumeRes;
=======
>>>>>>> c5633a559802cdd7da40a63a27008bf59dc9e067
import org.example.resai.model.Resume;
import org.example.resai.model.User;
import org.example.resai.security.JwtUtils;
import org.example.resai.service.ResumeService;
import org.example.resai.service.UserService;
import org.springframework.http.ResponseEntity;
<<<<<<< HEAD
import org.springframework.security.core.annotation.AuthenticationPrincipal;
=======
>>>>>>> c5633a559802cdd7da40a63a27008bf59dc9e067
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resumes")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final UserService userService;
    private final JwtUtils jwtUtils;

    // Helper method to extract user from token
    private User getUserFromToken(String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer "
        String email = jwtUtils.extractEmail(token);
        return userService.findByEmail(email);
    }

<<<<<<< HEAD

    @PostMapping("/create")
    public ResponseEntity<ResumeRes> create(@RequestBody ResumeReq dto, @AuthenticationPrincipal User user) {
        ResumeRes createdResume = resumeService.createResume(user, dto);
        return ResponseEntity.ok(createdResume);
    }

=======
>>>>>>> c5633a559802cdd7da40a63a27008bf59dc9e067
    // Get all resumes for the authenticated user
    @GetMapping
    public ResponseEntity<?> getAllResumes(@RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not found"));
            }

            List<Resume> resumes = resumeService.getResumesByUserId(user.getId());
            return ResponseEntity.ok(resumes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch resumes: " + e.getMessage()));
        }
    }

    // Get a single resume by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getResumeById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not found"));
            }

            Resume resume = resumeService.getResumeById(id, user.getId());

            if (resume == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Resume not found"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("resume", resume);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch resume: " + e.getMessage()));
        }
    }

    // Update an existing resume
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateResume(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            @RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not found"));
            }

            Resume updatedResume = resumeService.updateResume(id, user.getId(), payload);

            if (updatedResume == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Resume not found or unauthorized"));
            }

            return ResponseEntity.ok(updatedResume);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to update resume: " + e.getMessage()));
        }
    }

    // Delete a resume
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteResume(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not found"));
            }

            boolean deleted = resumeService.deleteResume(id, user.getId());

            if (!deleted) {
                return ResponseEntity.status(404).body(Map.of("error", "Resume not found or unauthorized"));
            }

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to delete resume: " + e.getMessage()));
        }
    }
}