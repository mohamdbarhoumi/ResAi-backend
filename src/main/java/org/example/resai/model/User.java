package org.example.resai.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.resai.security.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String fullName;
    private String profilePicture;
    private String password;
    private String authProvider;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private Role role;

    // ===== NEW PREMIUM FIELDS =====

    private LocalDateTime premiumUntil; // When premium expires

    @Column(nullable = true)
    private Integer tailoringCount = 0; // Monthly tailoring count

    @Column(nullable = true)
    private Integer coverLetterCount = 0; // Monthly cover letter count

    private LocalDate lastResetDate; // Last time counters were reset



    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastResetDate = LocalDate.now();
        if (role == null) {
            role = Role.USER;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== PREMIUM HELPER METHODS =====

    public boolean isPremium() {
        if (premiumUntil == null) return false;
        return LocalDateTime.now().isBefore(premiumUntil);
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public void resetMonthlyLimits() {
        this.tailoringCount = 0;
        this.coverLetterCount = 0;
        this.lastResetDate = LocalDate.now();
    }

    public boolean needsReset() {
        if (lastResetDate == null) return true;
        return !lastResetDate.getMonth().equals(LocalDate.now().getMonth());
    }

    // ============ UserDetails Implementation ============

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role != null) {
            return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}