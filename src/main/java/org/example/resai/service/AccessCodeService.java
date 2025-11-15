package org.example.resai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.example.resai.model.AccessCode;
import org.example.resai.model.User;
import org.example.resai.repository.AccessCodeRepo;
import org.example.resai.repository.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessCodeService {

    private final AccessCodeRepo accessCodeRepo;
    private final UserRepo userRepo;

    /**
     * Generate a new access code
     */
    public AccessCode generateCode(int durationDays, String notes) {
        String code = generateUniqueCode();

        AccessCode accessCode = new AccessCode();
        accessCode.setCode(code);
        accessCode.setDurationDays(durationDays);
        accessCode.setIsUsed(false);
        accessCode.setNotes(notes);

        AccessCode saved = accessCodeRepo.save(accessCode);
        log.info("Generated access code: {} for {} days", code, durationDays);

        return saved;
    }

    /**
     * Generate multiple codes at once
     */
    public List<AccessCode> generateMultipleCodes(int count, int durationDays, String notes) {
        List<AccessCode> codes = new java.util.ArrayList<>();

        for (int i = 0; i < count; i++) {
            codes.add(generateCode(durationDays, notes));
        }

        log.info("Generated {} access codes", count);
        return codes;
    }

    /**
     * Activate an access code for a user
     */
    @Transactional
    public boolean activateCode(String code, Long userId) {
        Optional<AccessCode> codeOpt = accessCodeRepo.findByCode(code.toUpperCase());

        if (codeOpt.isEmpty()) {
            log.warn("Access code not found: {}", code);
            return false;
        }

        AccessCode accessCode = codeOpt.get();

        if (accessCode.getIsUsed()) {
            log.warn("Access code already used: {}", code);
            return false;
        }

        Optional<User> userOpt = userRepo.findById(Math.toIntExact(userId));
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", userId);
            return false;
        }

        User user = userOpt.get();

        // Mark code as used
        accessCode.setIsUsed(true);
        accessCode.setUsedByUser(user);
        accessCode.setActivatedAt(LocalDateTime.now());
        accessCode.setExpiresAt(LocalDateTime.now().plusDays(accessCode.getDurationDays()));
        accessCodeRepo.save(accessCode);

        // Update user premium status
        LocalDateTime newPremiumUntil;
        if (user.getPremiumUntil() != null && user.getPremiumUntil().isAfter(LocalDateTime.now())) {
            // Extend existing premium
            newPremiumUntil = user.getPremiumUntil().plusDays(accessCode.getDurationDays());
        } else {
            // Start new premium
            newPremiumUntil = LocalDateTime.now().plusDays(accessCode.getDurationDays());
        }

        user.setPremiumUntil(newPremiumUntil);
        userRepo.save(user);

        log.info("Activated code {} for user {} (premium until {})", code, userId, newPremiumUntil);
        return true;
    }

    /**
     * Get all codes
     */
    public List<AccessCode> getAllCodes() {
        return accessCodeRepo.findAll();
    }

    /**
     * Get unused codes
     */
    public List<AccessCode> getUnusedCodes() {
        return accessCodeRepo.findUnusedCodesOrderByCreatedAtDesc();
    }

    /**
     * Get used codes
     */
    public List<AccessCode> getUsedCodes() {
        return accessCodeRepo.findUsedCodesOrderByActivatedAtDesc();
    }

    /**
     * Delete a code (only if unused)
     */
    @Transactional
    public boolean deleteCode(Long codeId) {
        Optional<AccessCode> codeOpt = accessCodeRepo.findById(codeId);

        if (codeOpt.isEmpty()) {
            return false;
        }

        AccessCode code = codeOpt.get();

        if (code.getIsUsed()) {
            log.warn("Cannot delete used code: {}", code.getCode());
            return false;
        }

        accessCodeRepo.delete(code);
        log.info("Deleted unused code: {}", code.getCode());
        return true;
    }

    /**
     * Get statistics
     */
    public CodeStatistics getStatistics() {
        Long totalCodes = accessCodeRepo.count();
        Long usedCodes = accessCodeRepo.countUsedCodes();
        Long unusedCodes = accessCodeRepo.countUnusedCodes();

        // Get codes created in last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<AccessCode> recentCodes = accessCodeRepo.findCodesCreatedAfter(thirtyDaysAgo);

        return new CodeStatistics(totalCodes, usedCodes, unusedCodes, (long) recentCodes.size());
    }

    /**
     * Generate unique code (format: RES-XXXX-XXXX)
     */
    private String generateUniqueCode() {
        String code;
        do {
            code = "RES-"
                    + RandomStringUtils.randomAlphanumeric(4).toUpperCase()
                    + "-"
                    + RandomStringUtils.randomAlphanumeric(4).toUpperCase();
        } while (accessCodeRepo.findByCode(code).isPresent());

        return code;
    }

    // Statistics DTO
    public record CodeStatistics(
            Long totalCodes,
            Long usedCodes,
            Long unusedCodes,
            Long recentCodes
    ) {}
}