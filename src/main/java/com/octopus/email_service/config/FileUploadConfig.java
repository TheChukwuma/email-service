package com.octopus.email_service.config;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import java.util.Set;

@Configuration
public class FileUploadConfig {
    
    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private String maxFileSize;
    
    @Value("${spring.servlet.multipart.max-request-size:10MB}")
    private String maxRequestSize;
    
    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
    
    @Bean
    public Tika tika() {
        return new Tika();
    }
    
    @Bean
    public Set<String> allowedMimeTypes() {
        return Set.of(
                "image/jpeg",
                "image/png",
                "image/gif",
                "image/webp",
                "application/pdf",
                "text/plain",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );
    }
    
    @Bean
    public long maxFileSizeBytes() {
        return parseSize(maxFileSize);
    }
    
    private long parseSize(String size) {
        if (size.endsWith("KB")) {
            return Long.parseLong(size.substring(0, size.length() - 2)) * 1024;
        } else if (size.endsWith("MB")) {
            return Long.parseLong(size.substring(0, size.length() - 2)) * 1024 * 1024;
        } else if (size.endsWith("GB")) {
            return Long.parseLong(size.substring(0, size.length() - 2)) * 1024 * 1024 * 1024;
        } else {
            return Long.parseLong(size);
        }
    }
}
