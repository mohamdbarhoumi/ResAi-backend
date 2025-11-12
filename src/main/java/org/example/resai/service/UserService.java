package org.example.resai.service;

import org.example.resai.dto.LoginReq;
import org.example.resai.dto.LoginRes;
import org.example.resai.dto.SignupReq;
import org.example.resai.model.User;
import org.example.resai.repository.UserRepo;
import org.example.resai.security.JwtUtils;
import org.example.resai.security.Role;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepo userRepo;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public UserService(UserRepo userRepo, JwtUtils jwtUtils) {
        this.userRepo = userRepo;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.jwtUtils = jwtUtils;
    }



    public User signup(SignupReq signupReq) {
        // 1️⃣ Check if email exists
        if (userRepo.existsByEmail(signupReq.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // 2️⃣ Create new user entity
        User user = new User();
        user.setEmail(signupReq.getEmail());
        user.setPassword(passwordEncoder.encode(signupReq.getPassword()));
        user.setAuthProvider("LOCAL");
        user.setRole(Role.USER);
        // Optional fields
        user.setFullName(null);
        user.setProfilePicture(null);

        // 3️⃣ Save to DB
        return userRepo.save(user);
    }

    public LoginRes login(LoginReq loginReq) {
        Optional<User> user = Optional.ofNullable(userRepo.findByEmail(loginReq.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials")));
        if(!passwordEncoder.matches(loginReq.getPassword(),user.get().getPassword())){
            throw new RuntimeException("Invalid credentials");
        }
        String token = jwtUtils.generateToken(user.get().getEmail());
        return new LoginRes(token, user.get().getEmail(), user.get().getRole());

    }




}