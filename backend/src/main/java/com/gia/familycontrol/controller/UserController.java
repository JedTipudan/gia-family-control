package com.gia.familycontrol.controller;

import com.gia.familycontrol.model.User;
import com.gia.familycontrol.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .map(u -> ResponseEntity.ok(Map.of(
                        "id", u.getId(),
                        "email", u.getEmail(),
                        "fullName", u.getFullName(),
                        "role", u.getRole().name(),
                        "pairCode", u.getPairCode() != null ? u.getPairCode() : ""
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable Long userId, Principal principal) {
        User requester = userRepository.findByEmail(principal.getName()).orElse(null);
        if (requester == null) return ResponseEntity.status(403).build();

        return userRepository.findById(userId)
                .map(u -> ResponseEntity.ok(Map.of(
                        "id", u.getId(),
                        "email", u.getEmail(),
                        "fullName", u.getFullName(),
                        "role", u.getRole().name(),
                        "pairCode", u.getPairCode() != null ? u.getPairCode() : ""
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}
