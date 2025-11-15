package org.example.resai.repository;

import org.example.resai.model.AccessCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccessCodeRepo extends JpaRepository<AccessCode, Long> {

    Optional<AccessCode> findByCode(String code);

    List<AccessCode> findByIsUsed(Boolean isUsed);

    @Query("SELECT ac FROM AccessCode ac WHERE ac.isUsed = true ORDER BY ac.activatedAt DESC")
    List<AccessCode> findUsedCodesOrderByActivatedAtDesc();

    @Query("SELECT ac FROM AccessCode ac WHERE ac.isUsed = false ORDER BY ac.createdAt DESC")
    List<AccessCode> findUnusedCodesOrderByCreatedAtDesc();

    @Query("SELECT COUNT(ac) FROM AccessCode ac WHERE ac.isUsed = false")
    Long countUnusedCodes();

    @Query("SELECT COUNT(ac) FROM AccessCode ac WHERE ac.isUsed = true")
    Long countUsedCodes();

    @Query("SELECT ac FROM AccessCode ac WHERE ac.createdAt >= :startDate")
    List<AccessCode> findCodesCreatedAfter(@Param("startDate") LocalDateTime startDate);
}