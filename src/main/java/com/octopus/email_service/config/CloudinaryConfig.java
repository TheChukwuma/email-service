package com.octopus.email_service.config;

import com.cloudinary.Cloudinary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class CloudinaryConfig {
    
    @Value("${cloudinary.cloud-name}")
    private String cloudName;
    
    @Value("${cloudinary.api-key}")
    private String apiKey;
    
    @Value("${cloudinary.api-secret}")
    private String apiSecret;
    
    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        
        Cloudinary cloudinary = new Cloudinary(config);
        log.info("Cloudinary configured for cloud: {}", cloudName);
        return cloudinary;
    }
}
