package org.example.resai.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ResumeRes {

    private Long id;
    private String title;
    private Map<String, Object> data;  // ✅ Changed from String to Map
    private Map<String, Object> aiMetadata;  // ✅ Changed from String to Map
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}