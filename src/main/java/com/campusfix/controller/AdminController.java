package com.campusfix.controller;

import com.campusfix.model.*;
import com.campusfix.repository.ComplaintRepository;
import com.campusfix.repository.StaffRepository;
import com.campusfix.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ComplaintService complaintService;
    private final StaffRepository staffRepository;
    private final ComplaintRepository complaintRepository;

    public AdminController(ComplaintService complaintService, StaffRepository staffRepository,
                           ComplaintRepository complaintRepository) {
        this.complaintService = complaintService;
        this.staffRepository = staffRepository;
        this.complaintRepository = complaintRepository;
    }

    @ModelAttribute("categories")
    public Category[] categories() { return Category.values(); }

    @ModelAttribute("priorities")
    public Priority[] priorities() { return Priority.values(); }

    @ModelAttribute("statuses")
    public Status[] statuses() { return Status.values(); }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", complaintService.statsAll());
        model.addAttribute("recent", complaintService.recentAll());
        model.addAttribute("categoryStats", complaintService.categoryStats());
        model.addAttribute("staffCount", staffRepository.count());
        return "admin/dashboard";
    }

    @GetMapping("/complaints")
    public String complaints(@RequestParam(required = false) String q,
                             @RequestParam(required = false) Category category,
                             @RequestParam(required = false) Priority priority,
                             @RequestParam(required = false) Status status,
                             @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", complaintService.search(null, q, category, priority, status, page));
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("priority", priority);
        model.addAttribute("status", status);
        return "admin/complaints";
    }

    @PostMapping("/complaints/{id}/assign")
    public String assign(@PathVariable Long id, @RequestParam Long staffId, Authentication auth, RedirectAttributes redirect) {
        try {
            complaintService.assign(id, staffId, auth.getName());
            redirect.addFlashAttribute("success", "Complaint assigned.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/complaints/" + id;
    }

    @PostMapping("/complaints/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam Status status,
                               @RequestParam(required = false) String remarks,
                               Authentication auth, RedirectAttributes redirect) {
        try {
            complaintService.updateStatus(id, status, remarks, auth.getName());
            redirect.addFlashAttribute("success", "Status updated to " + status.getLabel() + ".");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/complaints/" + id;
    }

    // ---------------- staff management ----------------

    @GetMapping("/staff")
    public String staff(Model model) {
        model.addAttribute("staffList", staffRepository.findAll());
        model.addAttribute("staffForm", new Staff());
        return "admin/staff";
    }

    @PostMapping("/staff")
    public String addStaff(@Valid @ModelAttribute("staffForm") Staff staff, BindingResult result,
                           Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("staffList", staffRepository.findAll());
            return "admin/staff";
        }
        staffRepository.save(staff);
        redirect.addFlashAttribute("success", "Staff member added.");
        return "redirect:/admin/staff";
    }

    @PostMapping("/staff/{id}/delete")
    public String deleteStaff(@PathVariable Long id, RedirectAttributes redirect) {
        if (complaintRepository.existsByAssignedStaffId(id)) {
            redirect.addFlashAttribute("error", "This staff member has assigned complaints and cannot be removed.");
        } else {
            staffRepository.deleteById(id);
            redirect.addFlashAttribute("success", "Staff member removed.");
        }
        return "redirect:/admin/staff";
    }
}
