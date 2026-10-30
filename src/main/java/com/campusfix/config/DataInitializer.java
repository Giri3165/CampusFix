package com.campusfix.config;

import com.campusfix.model.AppUser;
import com.campusfix.model.Role;
import com.campusfix.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Creates the first administrator account (from environment variables) if none exists. No fake complaints are created. */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           @Value("${app.admin.email}") String adminEmail,
                           @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByRole(Role.ADMIN)) return;
        AppUser admin = new AppUser();
        admin.setFullName("CampusFix Administrator");
        admin.setStudentId("ADMIN-001");
        admin.setEmail(adminEmail.toLowerCase(Locale.ROOT));
        admin.setDepartment("Administration");
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.warn("Created initial administrator '{}'. Change ADMIN_PASSWORD for any real deployment.", adminEmail);
    }
}
