package com.campusfix.controller;

import com.campusfix.model.AppUser;
import com.campusfix.model.Complaint;
import com.campusfix.model.Role;
import com.campusfix.model.Status;
import com.campusfix.repository.StaffRepository;
import com.campusfix.service.ComplaintService;
import com.campusfix.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Complaint details page shared by students (own complaints only) and administrators. */
@Controller
public class ComplaintController {

    private final ComplaintService complaintService;
    private final UserService userService;
    private final StaffRepository staffRepository;

    public ComplaintController(ComplaintService complaintService, UserService userService, StaffRepository staffRepository) {
        this.complaintService = complaintService;
        this.userService = userService;
        this.staffRepository = staffRepository;
    }

    @GetMapping("/complaints/{id}")
    public String details(@PathVariable Long id, Authentication auth, Model model) {
        AppUser user = userService.getByEmail(auth.getName());
        Complaint complaint = complaintService.getForUser(id, user);
        boolean admin = user.getRole() == Role.ADMIN;
        model.addAttribute("complaint", complaint);
        model.addAttribute("isAdmin", admin);
        model.addAttribute("canEdit", !admin && complaint.getStatus().isEditableByStudent());
        if (admin) {
            model.addAttribute("staffList", staffRepository.findAll());
            model.addAttribute("statuses", Status.values());
        }
        return "complaint-details";
    }
}
