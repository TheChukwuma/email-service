package com.octopus.email_service.entity;

import com.octopus.email_service.enums.EmailStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "emails")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Email {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    @Builder.Default
    private UUID uuid = UUID.randomUUID();
    
    @Column(name = "from_address", nullable = false)
    private String fromAddress;
    
    @Column(name = "reply_to_address")
    private String replyToAddress;
    
    @ElementCollection
    @CollectionTable(name = "email_to_addresses", joinColumns = @JoinColumn(name = "email_id"))
    @Column(name = "to_address")
    private List<String> toAddresses;
    
    @ElementCollection
    @CollectionTable(name = "email_cc_addresses", joinColumns = @JoinColumn(name = "email_id"))
    @Column(name = "cc_address")
    private List<String> ccAddresses;
    
    @ElementCollection
    @CollectionTable(name = "email_bcc_addresses", joinColumns = @JoinColumn(name = "email_id"))
    @Column(name = "bcc_address")
    private List<String> bccAddresses;
    
    @Column(nullable = false, length = 500)
    private String subject;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_name", referencedColumnName = "name")
    private Template template;

    @Column(name = "template_vars", columnDefinition = "TEXT")
    private String templateVars;
    
    @Column(columnDefinition = "TEXT")
    private String body;
    
    @Column(columnDefinition = "TEXT")
    private String attachments;
    
    @Column(name = "is_html_body")
    @Builder.Default
    private Boolean isHtmlBody = false;
    
    @Column(name = "needs_fallback_template")
    @Builder.Default
    private Boolean needsFallbackTemplate = false;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private EmailStatus status = EmailStatus.ENQUEUED;
    
    @Builder.Default
    private Integer attempts = 0;
    
    @Column(name = "max_attempts")
    @Builder.Default
    private Integer maxAttempts = 5;
    
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "email", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EmailEvent> events;
}
