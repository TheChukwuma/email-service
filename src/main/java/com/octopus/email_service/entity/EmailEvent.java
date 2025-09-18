package com.octopus.email_service.entity;

import com.octopus.email_service.enums.EventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.net.InetAddress;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id", nullable = false)
    private Email email;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EventType eventType;
    
    @Column(columnDefinition = "jsonb")
    private String detail;
    
    @Column(name = "ip_address")
    private InetAddress ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
