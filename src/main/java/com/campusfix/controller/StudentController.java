package com.campusfix.controller;

import com.campusfix.dto.ComplaintForm;
import com.campusfix.model.*;
import com.campusfix.service.ComplaintService;
import com.campusfix.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final ComplaintService complaintService;
    private final UserService userService;

    public StudentController(ComplaintService complaintService, UserService userService) {
        this.complaintService = complaintService;
        this.userService = userService;
    }

    @ModelAttribute("categories")
    public Category[] categories() { return Category.values(); }

    @ModelAttribute("priorities")
    public Priority[] priorities() { return Priority.values(); }

    @ModelAttribute("statuses")
    public Status[] statuses() { return Status.values(); }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        AppUser user = userService.getByEmail(auth.getName());
        model.addAttribute("stats", complaintService.statsFor(user));
        model.addAttribute("recent", complaintService.recentFor(user));
        return "student/dashboard";
    }

    @GetMapping("/complaints")
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Category category,
                       @RequestParam(required = false) Priority priority,
                       @RequestParam(required = false) Status status,
                       @RequestParam(defaultValue = "0") int page,
                       Authentication auth, Model model) {
        AppUser user = userService.getByEmail(auth.getName());
        model.addAttribute("page", complaintService.search(user, q, category, priority, status, page));
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("priority", priority);
        model.addAttribute("status", status);
        return "student/complaints";
    }

    @GetMapping("/complaints/new")
    public String newForm(Model model) {
        model.addAttribute("complaintForm", new ComplaintForm());
        model.addAttribute("formAction", "/student/complaints/new");
        model.addAttribute("editing", false);
        return "student/submit";
    }

    @PostMapping("/complaints/new")
    public String submit(@Valid @ModelAttribute("complaintForm") ComplaintForm form, BindingResult result,
                         @RequestParam(value = "image", required = false) MultipartFile image,
                         Authentication auth, Model model, RedirectAttributes redirect) {
        model.addAttribute("formAction", "/student/complaints/new");
        model.addAttribute("editing", false);
        if (result.hasErrors()) return "student/submit";
        try {
            Complaint saved = complaintService.submit(form, image, userService.getByEmail(auth.getName()));
            redirect.addFlashAttribute("success", "Complaint " + saved.getDisplayId() + " submitted successfully.");
            return "redirect:/complaints/" + saved.getId();
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "student/submit";
        }
    }

    @GetMapping("/complaints/{id}/edit")
    public String editForm(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes redirect) {
        Complaint c = complaintService.getForUser(id, userService.getByEmail(auth.getName()));
        if (!c.getStatus().isEditableByStudent()) {
            redirect.addFlashAttribute("error", "This complaint can no longer be edited.");
            return "redirect:/complaints/" + id;
        }
        model.addAttribute("complaintForm", ComplaintForm.from(c));
        model.addAttribute("complaint", c);
        model.addAttribute("formAction", "/student/complaints/" + id + "/edit");
        model.addAttribute("editing", true);
        return "student/submit";
    }

    @PostMapping("/complaints/{id}/edit")
    public String edit(@PathVariable Long id, @Valid @ModelAttribute("complaintForm") ComplaintForm form,
                       BindingResult result, @RequestParam(value = "image", required = false) MultipartFile image,
                       Authentication auth, Model model, RedirectAttributes redirect) {
        AppUser user = userService.getByEmail(auth.getName());
        model.addAttribute("complaint", complaintService.getForUser(id, user));
        model.addAttribute("formAction", "/student/complaints/" + id + "/edit");
        model.addAttribute("editing", true);
        if (result.hasErrors()) return "student/submit";
        try {
            complaintService.update(id, form, image, user);
            redirect.addFlashAttribute("success", "Complaint updated.");
            return "redirect:/complaints/" + id;
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/complaints/" + id;
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "student/submit";
        }
    }

    @PostMapping("/complaints/{id}/cancel")
    public String cancel(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        try {
            complaintService.cancel(id, userService.getByEmail(auth.getName()));
            redirect.addFlashAttribute("success", "Complaint cancelled.");
            return "redirect:/student/complaints";
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/complaints/" + id;
        }
    }
}
