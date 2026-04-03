package com.gia.familycontrol.service;

import com.gia.familycontrol.dto.AuthDto;
import com.gia.familycontrol.model.User;
import com.gia.familycontrol.repository.UserRepository;
import com.gia.familycontrol.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthDto.AuthResponse(token, user.getRole().name(), user.getId(), user.getFullName());
    }

    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(User.Role.valueOf(request.getRole().toUpperCase()));

        if (user.getRole() == User.Role.PARENT) {
            user.setPairCode("GIA-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        }

        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthDto.AuthResponse(token, user.getRole().name(), user.getId(), user.getFullName());
    }
}
