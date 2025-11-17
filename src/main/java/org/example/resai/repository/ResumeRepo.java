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

    // FIX: explicitly query user.id to avoid Spring trying to resolve "userId" field
    @Query("SELECT r FROM Resume r WHERE r.user.id = :userId")
    List<Resume> findByUserId(@Param("userId") Long id);

    // FIX: same correction using explicit JPQL
    @Query("SELECT r FROM Resume r WHERE r.user.id = :userId ORDER BY r.updatedAt DESC")
    List<Resume> findByUserIdOrderByUpdatedAtDesc(@Param("userId") Long userId);

    // FIX: explicitly declare correct JPQL (your method name stays the same)
    @Query("SELECT r FROM Resume r WHERE r.id = :id AND r.user.id = :userId")
    Optional<Resume> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
