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

    List<Resume> findByUser_Id(Long userId);

    @Query("SELECT r FROM Resume r WHERE r.user.id = :userId ORDER BY r.updatedAt DESC")
    List<Resume> findByUserIdOrderByUpdatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT r FROM Resume r WHERE r.id = :id AND r.user.id = :userId")
    Optional<Resume> findByIdAndUser_Id(@Param("id") Long id, @Param("userId") Long userId);
}
