package com.campusfix.service;

import com.campusfix.dto.RegistrationForm;
import com.campusfix.model.AppUser;
import com.campusfix.model.Role;
import com.campusfix.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserService userService;

    private RegistrationForm form() {
        RegistrationForm f = new RegistrationForm();
        f.setFullName("Asha Kumar");
        f.setStudentId("21CS001");
        f.setEmail("  Asha@College.edu ");
        f.setDepartment("CSE");
        f.setPassword("Password@123");
        f.setConfirmPassword("Password@123");
        return f;
    }

    @Test
    void registerStudent_hashesPasswordAndAssignsStudentRole() {
        when(passwordEncoder.encode("Password@123")).thenReturn("HASHED");
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AppUser saved = userService.registerStudent(form());

        assertEquals("asha@college.edu", saved.getEmail());
        assertEquals(Role.STUDENT, saved.getRole());
        assertEquals("HASHED", saved.getPassword());
        assertNotEquals("Password@123", saved.getPassword());
    }

    @Test
    void registerStudent_duplicateEmail_isRejected() {
        when(userRepository.existsByEmail("asha@college.edu")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.registerStudent(form()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerStudent_duplicateStudentId_isRejected() {
        when(userRepository.existsByStudentId("21CS001")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.registerStudent(form()));
        verify(userRepository, never()).save(any());
    }
}
