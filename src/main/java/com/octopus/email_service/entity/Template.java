package com.octopus.email_service.entity;

import com.octopus.email_service.enums.BodyType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Template {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 100)
    private String name;
    
    @Column(name = "subject_template", nullable = false, columnDefinition = "TEXT")
    private String subjectTemplate;
    
    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "body_type", length = 20)
    @Builder.Default
    private BodyType bodyType = BodyType.HTML;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Email> emails;
}
