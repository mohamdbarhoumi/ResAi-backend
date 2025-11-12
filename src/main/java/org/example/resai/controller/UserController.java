package org.example.resai.controller;
import org.apache.coyote.Response;
import org.example.resai.repository.UserRepo;
import org.springframework.security.core.Authentication;
import org.example.resai.dto.LoginReq;
import org.example.resai.dto.LoginRes;
import org.example.resai.dto.SignupReq;
import org.example.resai.model.User;
import org.example.resai.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.Map;


@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    private final UserService userService;
    private final UserRepo userRepo;

    public UserController(UserService userService, UserRepo userRepo) {
        this.userService = userService;
        this.userRepo = userRepo;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupReq signupReq) {
        try {
            User savedUser = userService.signup(signupReq);
            return ResponseEntity.ok(savedUser); // returns user data
        } catch (IllegalArgumentException e) {
            // 400 Bad Request for client errors
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // 500 Internal Server Error for unexpected errors
            e.printStackTrace();
            return ResponseEntity.status(500).body("Internal server error");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginReq loginReq) {
        try {
            LoginRes response = userService.login(loginReq);
            return ResponseEntity.ok(response);
        } catch (Exception e){
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }


    @GetMapping("/me")

    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if(authentication == null){
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepo.findByEmail(userDetails.getUsername()).orElseThrow();

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "role", user.getRole()
        ));
    }




}