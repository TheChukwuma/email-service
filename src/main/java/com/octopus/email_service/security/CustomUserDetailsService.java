package com.octopus.email_service.security;

import com.octopus.email_service.entity.User;
import com.octopus.email_service.enums.UserRole;
import com.octopus.email_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Try to find user by username first, then by email
        User user = userRepository.findActiveByUsernameOrEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username or email: " + username));
        
        // Build authorities based on the user's role
        List<SimpleGrantedAuthority> authorities = buildAuthorities(user.getRole());
        
        return new UserPrincipal(user, authorities);
    }
    
    private List<SimpleGrantedAuthority> buildAuthorities(UserRole role) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        
        // Add role-based authorities
        switch (role) {
            case SUPERADMIN:
                authorities.add(new SimpleGrantedAuthority("ROLE_SUPERADMIN"));
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                break;
            case ADMIN:
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                break;
            case USER_TENANT:
                authorities.add(new SimpleGrantedAuthority("ROLE_USER_TENANT"));
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                break;
            default:
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        
        return authorities;
    }
}
