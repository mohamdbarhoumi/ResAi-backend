package org.example.resai.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResumeSum {
    private Long id;
    private String title;
    private LocalDateTime updatedAt;
}
