package org.example.resai.controller;

import org.example.resai.dto.ResumeReq;
import org.example.resai.dto.ResumeRes;
import org.example.resai.model.Resume;
import org.example.resai.model.User;
import org.example.resai.repository.ResumeRepo;
import org.example.resai.service.ResumeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(
        origins = "http://localhost:3000",
        allowedHeaders = "*",
        allowCredentials = "true"
)
@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService service;

    public ResumeController(ResumeService service) {
        this.service = service;
    }

    @PostMapping("/create")
    public ResponseEntity<ResumeRes> create(@RequestBody ResumeReq dto, @AuthenticationPrincipal User user) {
        ResumeRes createdResume = service.createResume(user, dto);
        return ResponseEntity.ok(createdResume);
    }
}
