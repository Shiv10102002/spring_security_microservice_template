package com.shiv.springboot_security_jwt_rbac_app1.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UsernameChangeRequest {

    @NotBlank(message = "New email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String newUsername;
    
    @NotBlank(message = "Password is required")
    private String password;
}