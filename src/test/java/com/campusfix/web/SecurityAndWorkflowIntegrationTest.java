package com.campusfix.web;

import com.campusfix.dto.RegistrationForm;
import com.campusfix.model.*;
import com.campusfix.repository.ComplaintHistoryRepository;
import com.campusfix.repository.ComplaintRepository;
import com.campusfix.repository.StaffRepository;
import com.campusfix.repository.UserRepository;
import com.campusfix.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** End-to-end tests: registration, login, role-based access, and the full complaint lifecycle (H2 in-memory DB). */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityAndWorkflowIntegrationTest {

    private static final String PASSWORD = "Password@123";

    @Autowired MockMvc mvc;
    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired ComplaintRepository complaintRepository;
    @Autowired ComplaintHistoryRepository historyRepository;
    @Autowired StaffRepository staffRepository;

    private AppUser registerStudent(String email, String studentId) {
        RegistrationForm f = new RegistrationForm();
        f.setFullName("Test Student");
        f.setStudentId(studentId);
        f.setEmail(email);
        f.setDepartment("CSE");
        f.setPassword(PASSWORD);
        f.setConfirmPassword(PASSWORD);
        return userService.registerStudent(f);
    }

    private long submitComplaintAs(String email, String title) throws Exception {
        mvc.perform(multipart("/student/complaints/new")
                        .file(new MockMultipartFile("image", "", "application/octet-stream", new byte[0]))
                        .param("title", title)
                        .param("description", "The ceiling fan in room 204 does not turn on at all.")
                        .param("category", "ELECTRICAL")
                        .param("priority", "HIGH")
                        .param("location", "Block B, Room 204")
                        .with(user(email).roles("STUDENT")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        return complaintRepository.findAll().stream()
                .filter(c -> c.getTitle().equals(title)).findFirst().orElseThrow().getId();
    }

    // ---------------- public pages / registration / login ----------------

    @Test
    void publicPagesAreAccessible() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/register")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void registration_createsStudentWithHashedPassword() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("fullName", "New Student").param("studentId", "REG-001")
                        .param("email", "reg1@college.edu").param("department", "ECE")
                        .param("password", PASSWORD).param("confirmPassword", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        AppUser saved = userRepository.findByEmail("reg1@college.edu").orElseThrow();
        assertEquals(Role.STUDENT, saved.getRole());
        assertNotEquals(PASSWORD, saved.getPassword());
        assertTrue(saved.getPassword().startsWith("$2"), "password must be a BCrypt hash");
    }

    @Test
    void registration_withMismatchedPasswords_showsFormAgain() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("fullName", "Bad Student").param("studentId", "REG-002")
                        .param("email", "reg2@college.edu").param("department", "ECE")
                        .param("password", PASSWORD).param("confirmPassword", "different"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
        assertFalse(userRepository.existsByEmail("reg2@college.edu"));
    }

    @Test
    void login_studentIsRedirectedToStudentDashboard() throws Exception {
        registerStudent("login1@college.edu", "LOG-001");
        mvc.perform(formLogin("/login").user("email", "login1@college.edu").password("password", PASSWORD))
                .andExpect(authenticated().withRoles("STUDENT"))
                .andExpect(redirectedUrl("/student/dashboard"));
    }

    @Test
    void login_adminIsRedirectedToAdminDashboard() throws Exception {
        mvc.perform(formLogin("/login").user("email", "admin@campusfix.local").password("password", "TestAdmin@123"))
                .andExpect(authenticated().withRoles("ADMIN"))
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    void login_withWrongPassword_fails() throws Exception {
        registerStudent("login2@college.edu", "LOG-002");
        mvc.perform(formLogin("/login").user("email", "login2@college.edu").password("password", "wrong-password"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    // ---------------- role-based access control ----------------

    @Test
    void anonymousUser_isRedirectedToLogin() throws Exception {
        mvc.perform(get("/admin/dashboard")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/student/dashboard")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void student_cannotOpenAdminPages() throws Exception {
        mvc.perform(get("/admin/dashboard")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/complaints")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/staff")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canOpenAdminPages() throws Exception {
        mvc.perform(get("/admin/dashboard")).andExpect(status().isOk());
        mvc.perform(get("/admin/complaints")).andExpect(status().isOk());
        mvc.perform(get("/admin/staff")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_cannotOpenStudentPages() throws Exception {
        mvc.perform(get("/student/dashboard")).andExpect(status().isForbidden());
    }

    // ---------------- complaint lifecycle ----------------

    @Test
    void complaintLifecycle_submit_assign_inProgress_resolve() throws Exception {
        registerStudent("flow@college.edu", "FLOW-001");
        long id = submitComplaintAs("flow@college.edu", "Fan not working - lifecycle");

        assertEquals(Status.SUBMITTED, complaintRepository.findById(id).orElseThrow().getStatus());

        Staff staff = new Staff();
        staff.setName("Ravi Kumar");
        staff.setPhone("9876543210");
        staff.setSpecialization("Electrician");
        staff = staffRepository.save(staff);

        mvc.perform(post("/admin/complaints/" + id + "/assign").param("staffId", staff.getId().toString())
                        .with(user("admin@campusfix.local").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertEquals(Status.ASSIGNED, complaintRepository.findById(id).orElseThrow().getStatus());

        mvc.perform(post("/admin/complaints/" + id + "/status").param("status", "IN_PROGRESS").param("remarks", "Technician on site")
                        .with(user("admin@campusfix.local").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertEquals(Status.IN_PROGRESS, complaintRepository.findById(id).orElseThrow().getStatus());

        mvc.perform(post("/admin/complaints/" + id + "/status").param("status", "RESOLVED").param("remarks", "Motor replaced")
                        .with(user("admin@campusfix.local").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        Complaint resolved = complaintRepository.findById(id).orElseThrow();
        assertEquals(Status.RESOLVED, resolved.getStatus());
        assertEquals("Motor replaced", resolved.getAdminRemarks());
        assertTrue(historyRepository.count() >= 4, "every step must be written to complaint_history");

        // A resolved complaint can no longer be cancelled by the student
        mvc.perform(post("/student/complaints/" + id + "/cancel")
                        .with(user("flow@college.edu").roles("STUDENT")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertTrue(complaintRepository.existsById(id));
    }

    @Test
    void student_canCancelOwnSubmittedComplaint() throws Exception {
        registerStudent("cancel@college.edu", "CAN-001");
        long id = submitComplaintAs("cancel@college.edu", "Leaking tap - cancel test");

        mvc.perform(post("/student/complaints/" + id + "/cancel")
                        .with(user("cancel@college.edu").roles("STUDENT")).with(csrf()))
                .andExpect(redirectedUrl("/student/complaints"));
        assertFalse(complaintRepository.existsById(id));
    }

    @Test
    void student_cannotViewAnotherStudentsComplaint() throws Exception {
        registerStudent("owner@college.edu", "OWN-001");
        registerStudent("other@college.edu", "OTH-001");
        long id = submitComplaintAs("owner@college.edu", "Broken desk - privacy test");

        mvc.perform(get("/complaints/" + id).with(user("owner@college.edu").roles("STUDENT"))).andExpect(status().isOk());
        mvc.perform(get("/complaints/" + id).with(user("other@college.edu").roles("STUDENT"))).andExpect(status().isForbidden());
        mvc.perform(get("/complaints/" + id).with(user("admin@campusfix.local").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void invalidComplaintForm_isRejectedWithValidationErrors() throws Exception {
        registerStudent("invalid@college.edu", "INV-001");
        long before = complaintRepository.count();
        mvc.perform(multipart("/student/complaints/new")
                        .param("title", "").param("description", "short")
                        .with(user("invalid@college.edu").roles("STUDENT")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("student/submit"))
                .andExpect(model().attributeHasFieldErrors("complaintForm", "title", "description", "category", "location"));
        assertEquals(before, complaintRepository.count());
    }
}
