package org.example.resai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.resai.security.Role;

@Data
@AllArgsConstructor
public class LoginRes {
    private String token;
    private String email;
    private Role role;
}