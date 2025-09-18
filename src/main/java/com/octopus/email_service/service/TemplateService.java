package com.octopus.email_service.service;

import com.octopus.email_service.dto.TemplateRequest;
import com.octopus.email_service.dto.TemplateResponse;
import com.octopus.email_service.entity.Template;
import com.octopus.email_service.entity.User;
import com.octopus.email_service.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateService {
    
    private final TemplateRepository templateRepository;
    
    @Transactional
    public TemplateResponse createTemplate(User user, TemplateRequest request) {
        if (templateRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Template name already exists: " + request.getName());
        }
        
        Template template = Template.builder()
                .name(request.getName())
                .subjectTemplate(request.getSubjectTemplate())
                .bodyTemplate(request.getBodyTemplate())
                .bodyType(request.getBodyType())
                .createdBy(user)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        
        Template savedTemplate = templateRepository.save(template);
        log.info("Created template: {} by user: {}", savedTemplate.getName(), user.getUsername());
        
        return TemplateResponse.fromEntity(savedTemplate);
    }
    
    public List<TemplateResponse> getAllTemplates() {
        return templateRepository.findAllActive().stream()
                .map(TemplateResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<TemplateResponse> getUserTemplates(User user) {
        return templateRepository.findActiveByCreatedBy(user.getId()).stream()
                .map(TemplateResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    public Optional<TemplateResponse> getTemplateByName(String name) {
        return templateRepository.findActiveByName(name)
                .map(TemplateResponse::fromEntity);
    }
    
    public Optional<Template> getTemplateEntityByName(String name) {
        return templateRepository.findActiveByName(name);
    }
    
    @Transactional
    public TemplateResponse updateTemplate(String name, User user, TemplateRequest request) {
        Template template = templateRepository.findActiveByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + name));
        
        // Check if user has permission to update (created by user or admin)
        if (!template.getCreatedBy().getId().equals(user.getId()) && !user.getIsAdmin()) {
            throw new IllegalArgumentException("You don't have permission to update this template");
        }
        
        // Check if new name conflicts with existing template
        if (!template.getName().equals(request.getName()) && 
            templateRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Template name already exists: " + request.getName());
        }
        
        template.setName(request.getName());
        template.setSubjectTemplate(request.getSubjectTemplate());
        template.setBodyTemplate(request.getBodyTemplate());
        template.setBodyType(request.getBodyType());
        if (request.getIsActive() != null) {
            template.setIsActive(request.getIsActive());
        }
        
        Template savedTemplate = templateRepository.save(template);
        log.info("Updated template: {} by user: {}", savedTemplate.getName(), user.getUsername());
        
        return TemplateResponse.fromEntity(savedTemplate);
    }
    
    @Transactional
    public void deleteTemplate(String name, User user) {
        Template template = templateRepository.findActiveByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + name));
        
        // Check if user has permission to delete (created by user or admin)
        if (!template.getCreatedBy().getId().equals(user.getId()) && !user.getIsAdmin()) {
            throw new IllegalArgumentException("You don't have permission to delete this template");
        }
        
        template.setIsActive(false);
        templateRepository.save(template);
        log.info("Deactivated template: {} by user: {}", template.getName(), user.getUsername());
    }
}
