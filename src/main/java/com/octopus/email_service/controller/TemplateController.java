package com.octopus.email_service.controller;

import com.octopus.email_service.dto.ApiResponse;
import com.octopus.email_service.dto.TemplateRequest;
import com.octopus.email_service.dto.TemplateResponse;
import com.octopus.email_service.entity.User;
import com.octopus.email_service.service.TemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/templates")
@RequiredArgsConstructor
@Slf4j
public class TemplateController {
    
    private final TemplateService templateService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<TemplateResponse>> createTemplate(
            @Valid @RequestBody TemplateRequest request,
            Authentication authentication) {
        
        try {
            User user = (User) authentication.getPrincipal();
            TemplateResponse response = templateService.createTemplate(user, request);
            return ResponseEntity.ok(ApiResponse.success("Template created successfully", response));
        } catch (Exception e) {
            log.error("Failed to create template", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to create template: " + e.getMessage()));
        }
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<TemplateResponse>>> getAllTemplates(
            Authentication authentication) {
        
        try {
            List<TemplateResponse> templates = templateService.getAllTemplates();
            return ResponseEntity.ok(ApiResponse.success(templates));
        } catch (Exception e) {
            log.error("Failed to get all templates", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get templates: " + e.getMessage()));
        }
    }
    
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<TemplateResponse>>> getMyTemplates(
            Authentication authentication) {
        
        try {
            User user = (User) authentication.getPrincipal();
            List<TemplateResponse> templates = templateService.getUserTemplates(user);
            return ResponseEntity.ok(ApiResponse.success(templates));
        } catch (Exception e) {
            log.error("Failed to get user templates", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get templates: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{name}")
    public ResponseEntity<ApiResponse<TemplateResponse>> getTemplateByName(
            @PathVariable String name,
            Authentication authentication) {
        
        try {
            return templateService.getTemplateByName(name)
                    .map(template -> ResponseEntity.ok(ApiResponse.success(template)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Failed to get template by name: {}", name, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get template: " + e.getMessage()));
        }
    }
    
    @PutMapping("/{name}")
    public ResponseEntity<ApiResponse<TemplateResponse>> updateTemplate(
            @PathVariable String name,
            @Valid @RequestBody TemplateRequest request,
            Authentication authentication) {
        
        try {
            User user = (User) authentication.getPrincipal();
            TemplateResponse response = templateService.updateTemplate(name, user, request);
            return ResponseEntity.ok(ApiResponse.success("Template updated successfully", response));
        } catch (Exception e) {
            log.error("Failed to update template: {}", name, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to update template: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/{name}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @PathVariable String name,
            Authentication authentication) {
        
        try {
            User user = (User) authentication.getPrincipal();
            templateService.deleteTemplate(name, user);
            return ResponseEntity.ok(ApiResponse.success("Template deleted successfully", null));
        } catch (Exception e) {
            log.error("Failed to delete template: {}", name, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to delete template: " + e.getMessage()));
        }
    }
}
