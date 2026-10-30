package com.campusfix.service;

import com.campusfix.dto.RegistrationForm;
import com.campusfix.model.AppUser;
import com.campusfix.model.Role;
import com.campusfix.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUser registerStudent(RegistrationForm form) {
        String email = form.getEmail().trim().toLowerCase(Locale.ROOT);
        String studentId = form.getStudentId().trim();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        if (userRepository.existsByStudentId(studentId)) {
            throw new IllegalArgumentException("This Student ID is already registered.");
        }
        AppUser user = new AppUser();
        user.setFullName(form.getFullName().trim());
        user.setStudentId(studentId);
        user.setEmail(email);
        user.setDepartment(form.getDepartment().trim());
        user.setPassword(passwordEncoder.encode(form.getPassword()));   // BCrypt hash
        user.setRole(Role.STUDENT);
        return userRepository.save(user);
    }

    public AppUser getByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
