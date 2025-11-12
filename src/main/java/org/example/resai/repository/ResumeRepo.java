package org.example.resai.repository;

import io.micrometer.common.KeyValues;
import org.example.resai.model.Resume;
import org.example.resai.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeRepo extends JpaRepository<Resume,Integer> {
    List<Resume> findByUser(User user);

    List<Resume> findByUserId(Long id);
}
