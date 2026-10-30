package com.campusfix.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegistrationForm {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must be 100 characters or fewer")
    private String fullName;

    @NotBlank(message = "Student ID is required")
    @Size(max = 30, message = "Student ID must be 30 characters or fewer")
    private String studentId;

    @NotBlank(message = "College email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 120)
    private String email;

    @NotBlank(message = "Department is required")
    @Size(max = 100)
    private String department;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be 8 to 64 characters")
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;
}
