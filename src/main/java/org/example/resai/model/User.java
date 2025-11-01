    package org.example.resai.model;

    import jakarta.persistence.*;
    import lombok.AllArgsConstructor;
    import lombok.Data;
    import lombok.NoArgsConstructor;
    import org.example.resai.security.Role;

    import java.time.LocalDateTime;

    @Data
    @Entity
    @AllArgsConstructor
    @NoArgsConstructor
    @Table(name = "users")
    public class User {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        // Common fields
        @Column(nullable = false, unique = true)
        private String email;
        private String fullName;
        private String profilePicture; // URL from Google or uploaded
        private String password; // optional, null if using Google login
        private String authProvider; // "LOCAL", "GOOGLE", "GITHUB", etc.
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        @Enumerated(EnumType.STRING)
        private Role role;
        @PrePersist
        protected void onCreate() {
            createdAt = LocalDateTime.now();
        }
        @PreUpdate
        protected void onUpdate() {
            updatedAt = LocalDateTime.now();
        }

    }
