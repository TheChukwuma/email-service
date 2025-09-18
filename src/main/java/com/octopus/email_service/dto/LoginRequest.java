package com.octopus.email_service.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginRequest {

    private String username;
    private String password;

}