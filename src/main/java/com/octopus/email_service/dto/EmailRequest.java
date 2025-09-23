package com.octopus.email_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {
    
    @NotBlank(message = "From address is required")
    @Email(message = "From address must be a valid email")
    private String from;
    
    @NotEmpty(message = "At least one To address is required")
    private List<@Email String> to;
    
    private List<@Email String> cc;
    
    private List<@Email String> bcc;
    
    private String templateName;
    
    private Map<String, Object> templateVars;
    
    private String subject;
    
    private String body;
    
    private List<EmailAttachmentDto> attachments;

}
