package org.example.resai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

@Data
public class ResumeReq {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Resume data is required")
    private Map<String, Object> data;  // ✅ Changed from String to Map

    private Map<String, Object> aiMetadata;  // ✅ Changed from String to Map
}