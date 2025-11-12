package org.example.resai.controller;

import lombok.RequiredArgsConstructor;
import org.example.resai.dto.AiRequest;
import org.example.resai.dto.AiResponse;
import org.example.resai.model.User;
import org.example.resai.security.JwtUtils;
import org.example.resai.service.AiService;
import org.example.resai.service.AiService;
import org.example.resai.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final UserService userService;
    private final JwtUtils jwtUtils;

    // Helper method to extract user from token
    private User getUserFromToken(String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer "
        String email = jwtUtils.extractEmail(token);
        return userService.findByEmail(email);
    }

    /**
     * Generate professional summary based on user description
     */
    @PostMapping("/generate-summary")
    public ResponseEntity<?> generateSummary(
            @RequestBody AiRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not found"));
            }

            String summary = aiService.generateSummary(request.getUserInput());

            AiResponse response = AiResponse.builder()
                    .success(true)
                    .generatedText(summary)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to generate summary: " + e.getMessage()
            ));
        }
    }

    /**
     * Generate experience bullets based on user description
     */
    @PostMapping("/generate-experience-bullets")
    public ResponseEntity<?> generateExperienceBullets(
            @RequestBody AiRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not found"));
            }

            String bullets = aiService.generateExperienceBullets(
                    request.getUserInput(),
                    request.getContext()
            );

            AiResponse response = AiResponse.builder()
                    .success(true)
                    .generatedText(bullets)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to generate experience bullets: " + e.getMessage()
            ));
        }
    }

    /**
     * Generate project bullets based on user description
     */
    @PostMapping("/generate-project-bullets")
    public ResponseEntity<?> generateProjectBullets(
            @RequestBody AiRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not found"));
            }

            String bullets = aiService.generateProjectBullets(
                    request.getUserInput(),
                    request.getContext()
            );

            AiResponse response = AiResponse.builder()
                    .success(true)
                    .generatedText(bullets)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to generate project bullets: " + e.getMessage()
            ));
        }
    }
}