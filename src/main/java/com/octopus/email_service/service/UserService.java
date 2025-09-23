package com.octopus.email_service.service;

import com.octopus.email_service.annotation.Auditable;
import com.octopus.email_service.dto.UserRequest;
import com.octopus.email_service.entity.EmailTenant;
import com.octopus.email_service.entity.User;
import com.octopus.email_service.enums.UserRole;
import com.octopus.email_service.repository.EmailTenantRepository;
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
    private final EmailTenantRepository emailTenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthorizationService authorizationService;
    
    @Transactional
    @Auditable(action = "CREATE_USER", resourceType = "USER", description = "Create a new user")
    public User createUser(UserRequest request) {
        return createUser(request, null); // No permission check for public registration
    }
    
    @Transactional
    @Auditable(action = "CREATE_USER", resourceType = "USER", description = "Create a new user with permission check")
    public User createUser(UserRequest request, User currentUser) {
        // Permission checks for admin operations
        if (currentUser != null) {
            authorizationService.requirePermission(currentUser, 
                request.getRole() == UserRole.USER_TENANT ? 
                    com.octopus.email_service.enums.Permission.CREATE_USER_TENANT : 
                    com.octopus.email_service.enums.Permission.CREATE_ADMIN);
        }
        
        // Validation
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }
        
        // Handle role and tenant assignment
        UserRole role = request.getRole() != null ? request.getRole() : UserRole.USER_TENANT;
        EmailTenant tenant = null;
        
        if (role == UserRole.USER_TENANT) {
            if (request.getTenantId() == null) {
                throw new IllegalArgumentException("Tenant ID is required for USER_TENANT role");
            }
            tenant = emailTenantRepository.findById(request.getTenantId())
                    .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + request.getTenantId()));
        }
        
        
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .tenant(tenant)
                .isActive(true)
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("Created user: {} with role: {} for tenant: {}", 
                savedUser.getUsername(), savedUser.getRole(), 
                tenant != null ? tenant.getTenantCode() : "none");
        return savedUser;
    }
    
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    @Auditable(action = "VIEW_ALL_USERS", resourceType = "USER", description = "View all users")
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
    
    public List<User> findUsersByRole(UserRole role) {
        return userRepository.findByRole(role);
    }
    
    public List<User> findUsersByTenant(Long tenantId) {
        return userRepository.findByTenantId(tenantId);
    }
    
    @Transactional
    @Auditable(action = "UPDATE_USER", resourceType = "USER", description = "Update user information")
    public User updateUser(Long userId, UserRequest request, User currentUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        // Permission check
        authorizationService.requireUserManagementPermission(currentUser, user);
        
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
        
        // Handle role changes
        if (request.getRole() != null && !request.getRole().equals(user.getRole())) {
            // Check if current user can create users with the target role
            if (!authorizationService.canCreateUserWithRole(currentUser, request.getRole())) {
                throw new SecurityException("You cannot assign role: " + request.getRole());
            }
            user.setRole(request.getRole());
        }
        
        // Handle tenant changes
        if (request.getTenantId() != null) {
            if (user.getRole() == UserRole.USER_TENANT) {
                EmailTenant tenant = emailTenantRepository.findById(request.getTenantId())
                        .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + request.getTenantId()));
                user.setTenant(tenant);
            }
        }
        
        
        User savedUser = userRepository.save(user);
        log.info("Updated user: {} with role: {} for tenant: {}", 
                savedUser.getUsername(), savedUser.getRole(),
                savedUser.getTenant() != null ? savedUser.getTenant().getTenantCode() : "none");
        return savedUser;
    }
    
    @Transactional
    @Auditable(action = "DELETE_USER", resourceType = "USER", description = "Deactivate user account")
    public void deleteUser(Long userId, User currentUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        // Permission check
        authorizationService.requireUserManagementPermission(currentUser, user);
        
        user.setIsActive(false);
        userRepository.save(user);
        log.info("Deactivated user: {} by: {}", user.getUsername(), currentUser.getUsername());
    }
}
