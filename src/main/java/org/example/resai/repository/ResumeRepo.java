package org.example.resai.repository;

import org.example.resai.model.Resume;
import org.example.resai.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResumeRepo extends JpaRepository<Resume, Long> {

    List<Resume> findByUser(User user);

    List<Resume> findByUserId(Long id);

    // Get all resumes ordered by updated date (newest first)
    @Query("SELECT r FROM Resume r WHERE r.user.id = :userId ORDER BY r.updatedAt DESC")
    List<Resume> findByUserIdOrderByUpdatedAtDesc(@Param("userId") Long userId);

    // Find a resume by ID that belongs to a specific user (for security)
    @Query("SELECT r FROM Resume r WHERE r.id = :id AND r.user.id = :userId")
    Optional<Resume> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}