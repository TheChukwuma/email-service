package com.octopus.email_service.service;

import com.octopus.email_service.dto.UserRequest;
import com.octopus.email_service.entity.User;
import com.octopus.email_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional
    public User createUser(UserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }
        
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isActive(true)
                .isAdmin(request.getIsAdmin() != null ? request.getIsAdmin() : false)
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("Created user: {}", savedUser.getUsername());
        return savedUser;
    }
    
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
    
    @Transactional
    public User updateUser(Long userId, UserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        if (!user.getUsername().equals(request.getUsername()) && 
            userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }
        
        if (!user.getEmail().equals(request.getEmail()) && 
            userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }
        
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getIsAdmin() != null) {
            user.setIsAdmin(request.getIsAdmin());
        }
        
        User savedUser = userRepository.save(user);
        log.info("Updated user: {}", savedUser.getUsername());
        return savedUser;
    }
    
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        user.setIsActive(false);
        userRepository.save(user);
        log.info("Deactivated user: {}", user.getUsername());
    }
}
