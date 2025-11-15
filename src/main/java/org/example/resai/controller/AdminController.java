package org.example.resai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.resai.model.AccessCode;
import org.example.resai.model.Resume;
import org.example.resai.model.User;
import org.example.resai.repository.ResumeRepo;
import org.example.resai.repository.UserRepo;
import org.example.resai.security.Role;
import org.example.resai.service.AccessCodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class AdminController {

    private final AccessCodeService accessCodeService;
    private final UserRepo userRepo;
    private final ResumeRepo resumeRepo;

    // ============ ACCESS CODE MANAGEMENT ============

    /**
     * Generate single access code
     * POST /api/admin/codes/generate
     */
    @PostMapping("/codes/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> generateCode(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal User admin) {
        try {
            Integer durationDays = (Integer) request.get("durationDays");
            String notes = (String) request.getOrDefault("notes", "");

            if (durationDays == null || durationDays <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid duration"));
            }

            AccessCode code = accessCodeService.generateCode(durationDays, notes);

            log.info("Admin {} generated code: {}", admin.getEmail(), code.getCode());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "code", code
            ));
        } catch (Exception e) {
            log.error("Error generating code: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Generate multiple access codes
     * POST /api/admin/codes/generate-bulk
     */
    @PostMapping("/codes/generate-bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> generateBulkCodes(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal User admin) {
        try {
            Integer count = (Integer) request.get("count");
            Integer durationDays = (Integer) request.get("durationDays");
            String notes = (String) request.getOrDefault("notes", "");

            if (count == null || count <= 0 || count > 100) {
                return ResponseEntity.badRequest().body(Map.of("error", "Count must be 1-100"));
            }

            if (durationDays == null || durationDays <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid duration"));
            }

            List<AccessCode> codes = accessCodeService.generateMultipleCodes(count, durationDays, notes);

            log.info("Admin {} generated {} codes", admin.getEmail(), count);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "codes", codes,
                    "count", codes.size()
            ));
        } catch (Exception e) {
            log.error("Error generating bulk codes: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all access codes
     * GET /api/admin/codes
     */
    @GetMapping("/codes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllCodes(@AuthenticationPrincipal User admin) {
        try {
            List<AccessCode> codes = accessCodeService.getAllCodes();
            return ResponseEntity.ok(codes);
        } catch (Exception e) {
            log.error("Error fetching codes: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get unused codes
     * GET /api/admin/codes/unused
     */
    @GetMapping("/codes/unused")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUnusedCodes(@AuthenticationPrincipal User admin) {
        try {
            List<AccessCode> codes = accessCodeService.getUnusedCodes();
            return ResponseEntity.ok(codes);
        } catch (Exception e) {
            log.error("Error fetching unused codes: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get used codes
     * GET /api/admin/codes/used
     */
    @GetMapping("/codes/used")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUsedCodes(@AuthenticationPrincipal User admin) {
        try {
            List<AccessCode> codes = accessCodeService.getUsedCodes();
            return ResponseEntity.ok(codes);
        } catch (Exception e) {
            log.error("Error fetching used codes: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete unused code
     * DELETE /api/admin/codes/{id}
     */
    @DeleteMapping("/codes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCode(
            @PathVariable Long id,
            @AuthenticationPrincipal User admin) {
        try {
            boolean deleted = accessCodeService.deleteCode(id);

            if (!deleted) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot delete used code"));
            }

            log.info("Admin {} deleted code {}", admin.getEmail(), id);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Error deleting code: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ============ USER MANAGEMENT ============

    /**
     * Get all users
     * GET /api/admin/users
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers(@AuthenticationPrincipal User admin) {
        try {
            List<User> users = userRepo.findAll();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error fetching users: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get user by ID with details
     * GET /api/admin/users/{id}
     */
    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserById(
            @PathVariable Long id,
            @AuthenticationPrincipal User admin) {
        try {
            User user = userRepo.findById(Math.toIntExact(id)).orElse(null);

            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            List<Resume> resumes = resumeRepo.findByUserId(id);

            Map<String, Object> response = new HashMap<>();
            response.put("user", user);
            response.put("resumeCount", resumes.size());
            response.put("resumes", resumes);
            response.put("isPremium", user.isPremium());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching user: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update user role
     * PUT /api/admin/users/{id}/role
     */
    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User admin) {
        try {
            User user = userRepo.findById(Math.toIntExact(id)).orElse(null);

            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            String roleStr = request.get("role");
            Role newRole = Role.valueOf(roleStr);

            user.setRole(newRole);
            userRepo.save(user);

            log.info("Admin {} updated user {} role to {}", admin.getEmail(), id, newRole);

            return ResponseEntity.ok(Map.of("success", true, "user", user));
        } catch (Exception e) {
            log.error("Error updating user role: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Grant premium to user manually
     * POST /api/admin/users/{id}/grant-premium
     */
    @PostMapping("/users/{id}/grant-premium")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> grantPremium(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> request,
            @AuthenticationPrincipal User admin) {
        try {
            User user = userRepo.findById(Math.toIntExact(id)).orElse(null);

            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            Integer days = request.get("days");
            if (days == null || days <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid days"));
            }

            LocalDateTime newPremiumUntil;
            if (user.getPremiumUntil() != null && user.getPremiumUntil().isAfter(LocalDateTime.now())) {
                newPremiumUntil = user.getPremiumUntil().plusDays(days);
            } else {
                newPremiumUntil = LocalDateTime.now().plusDays(days);
            }

            user.setPremiumUntil(newPremiumUntil);
            userRepo.save(user);

            log.info("Admin {} granted {} days premium to user {}", admin.getEmail(), days, id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "user", user,
                    "premiumUntil", newPremiumUntil
            ));
        } catch (Exception e) {
            log.error("Error granting premium: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Revoke premium from user
     * POST /api/admin/users/{id}/revoke-premium
     */
    @PostMapping("/users/{id}/revoke-premium")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> revokePremium(
            @PathVariable Long id,
            @AuthenticationPrincipal User admin) {
        try {
            User user = userRepo.findById(Math.toIntExact(id)).orElse(null);

            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            user.setPremiumUntil(null);
            userRepo.save(user);

            log.info("Admin {} revoked premium from user {}", admin.getEmail(), id);

            return ResponseEntity.ok(Map.of("success", true, "user", user));
        } catch (Exception e) {
            log.error("Error revoking premium: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ============ STATISTICS ============

    /**
     * Get dashboard statistics
     * GET /api/admin/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStatistics(@AuthenticationPrincipal User admin) {
        try {
            // User stats
            Long totalUsers = userRepo.count();
            Long premiumUsers = userRepo.countByPremiumUntilAfter(LocalDateTime.now());
            Long freeUsers = totalUsers - premiumUsers;

            // Resume stats
            Long totalResumes = resumeRepo.count();

            // Code stats
            AccessCodeService.CodeStatistics codeStats = accessCodeService.getStatistics();

            Map<String, Object> stats = new HashMap<>();
            stats.put("users", Map.of(
                    "total", totalUsers,
                    "premium", premiumUsers,
                    "free", freeUsers
            ));
            stats.put("resumes", Map.of(
                    "total", totalResumes
            ));
            stats.put("codes", Map.of(
                    "total", codeStats.totalCodes(),
                    "used", codeStats.usedCodes(),
                    "unused", codeStats.unusedCodes(),
                    "recent", codeStats.recentCodes()
            ));

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching statistics: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}