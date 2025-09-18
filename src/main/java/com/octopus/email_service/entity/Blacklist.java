package com.octopus.email_service.entity;

import com.octopus.email_service.enums.BlacklistType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "blacklist")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Blacklist {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "email_address", unique = true, nullable = false)
    private String emailAddress;
    
    @Column(length = 100)
    private String reason;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "blacklist_type", length = 20)
    @Builder.Default
    private BlacklistType blacklistType = BlacklistType.HARD_BOUNCE;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
