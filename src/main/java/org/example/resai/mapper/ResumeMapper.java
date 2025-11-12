package org.example.resai.mapper;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.resai.dto.ResumeReq;
import org.example.resai.dto.ResumeRes;
import org.example.resai.model.Resume;
import org.example.resai.model.User;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class ResumeMapper {

    public ResumeRes toResponseDTO(Resume resume) {
        if (resume == null) {
            return null;
        }

        ResumeRes dto = new ResumeRes();
        dto.setId(resume.getId());
        dto.setTitle(resume.getTitle());
        dto.setData(resume.getData());
        dto.setAiMetadata(resume.getAiMetadata());
        dto.setVersion(resume.getVersion());
        dto.setCreatedAt(resume.getCreatedAt());
        dto.setUpdatedAt(resume.getUpdatedAt());
        return dto;
    }

    // ✅ FIXED: Don't set the ID! Let Hibernate auto-generate it
    public Resume fromRequestDTO(ResumeReq dto, Long userId) {
        if (dto == null) {
            return null;
        }

        Resume resume = new Resume();
        resume.setTitle(dto.getTitle());
        resume.setData(dto.getData());
        resume.setAiMetadata(dto.getAiMetadata());
        // ❌ DON'T DO THIS: resume.setId(userId);
        // The User will be set in the service layer
        return resume;
    }

    public void updateResumeFromDTO(ResumeReq dto, Resume resume) {
        if (dto.getTitle() != null) {
            resume.setTitle(dto.getTitle());
        }
        if (dto.getData() != null) {
            resume.setData(dto.getData());
        }
        if (dto.getAiMetadata() != null) {
            resume.setAiMetadata(dto.getAiMetadata());
        }
    }
}