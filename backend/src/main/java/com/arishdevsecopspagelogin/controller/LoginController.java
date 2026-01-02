package com.arishdevsecopspagelogin.controller;

import com.arishdevsecopspagelogin.model.User;
import com.arishdevsecopspagelogin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class LoginController {
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody User loginUser) {
        Map<String, String> response = new HashMap<>();
        
        Optional<User> user = userRepository.findByUsername(loginUser.getUsername());
        
        if (user.isPresent() && user.get().getPassword().equals(loginUser.getPassword())) {
            response.put("message", "Login successful");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "Invalid credentials");
            return ResponseEntity.status(401).body(response);
        }
    }
}
