package org.example.resai.service;

import lombok.RequiredArgsConstructor;
import org.example.resai.dto.ResumeReq;
import org.example.resai.dto.ResumeRes;
import org.example.resai.dto.ResumeSum;
import org.example.resai.mapper.ResumeMapper;
import org.example.resai.model.Resume;
import org.example.resai.model.User;
import org.example.resai.repository.ResumeRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepo resumeRepo;
    private final ResumeMapper resumeMapper;

    // Create resume safely
    public ResumeRes createResume(User user, ResumeReq dto) {
        if (user == null) {
            throw new IllegalArgumentException("Authenticated user cannot be null");
        }

        // Mapper handles filling resume fields
        Resume resume = resumeMapper.fromRequestDTO(dto, user.getId());

        resume.setUser(user);

        Resume saved = resumeRepo.save(resume);
        return resumeMapper.toResponseDTO(saved);
    }

    public List<ResumeSum> getUserResumes(User user) {
        if (user == null) return List.of();

        return resumeRepo.findByUserId(user.getId())
                .stream()
                .map(r -> {
                    ResumeSum dto = new ResumeSum();
                    dto.setId(r.getId()); // Auto-generated ID is used here
                    dto.setTitle(r.getTitle());
                    dto.setUpdatedAt(r.getUpdatedAt());
                    return dto;
                })
                .toList();
    }
}
