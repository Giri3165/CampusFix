package com.campusfix.service;

import com.campusfix.dto.ComplaintForm;
import com.campusfix.model.*;
import com.campusfix.repository.ComplaintHistoryRepository;
import com.campusfix.repository.ComplaintRepository;
import com.campusfix.repository.StaffRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ComplaintService {

    public static final int PAGE_SIZE = 8;

    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryRepository historyRepository;
    private final StaffRepository staffRepository;
    private final FileStorageService storageService;

    public ComplaintService(ComplaintRepository complaintRepository,
                            ComplaintHistoryRepository historyRepository,
                            StaffRepository staffRepository,
                            FileStorageService storageService) {
        this.complaintRepository = complaintRepository;
        this.historyRepository = historyRepository;
        this.staffRepository = staffRepository;
        this.storageService = storageService;
    }

    // ------------------------------------------------------------ student actions

    @Transactional
    public Complaint submit(ComplaintForm form, MultipartFile image, AppUser student) {
        Complaint c = new Complaint();
        apply(form, c);
        if (image != null && !image.isEmpty()) {
            c.setImagePath(storageService.store(image));
        }
        c.setStudent(student);
        c.setStatus(Status.SUBMITTED);
        Complaint saved = complaintRepository.save(c);
        record(saved, Status.SUBMITTED, "Complaint submitted", student.getFullName());
        return saved;
    }

    @Transactional
    public Complaint update(Long id, ComplaintForm form, MultipartFile image, AppUser student) {
        Complaint c = find(id);
        ensureOwner(c, student);
        ensureEditable(c);
        apply(form, c);
        if (image != null && !image.isEmpty()) {
            String old = c.getImagePath();
            c.setImagePath(storageService.store(image));
            storageService.delete(old);
        }
        Complaint saved = complaintRepository.save(c);
        record(saved, saved.getStatus(), "Complaint edited by student", student.getFullName());
        return saved;
    }

    @Transactional
    public void cancel(Long id, AppUser student) {
        Complaint c = find(id);
        ensureOwner(c, student);
        ensureEditable(c);
        String image = c.getImagePath();
        complaintRepository.delete(c);
        storageService.delete(image);
    }

    // ------------------------------------------------------------ admin actions

    @Transactional
    public Complaint assign(Long complaintId, Long staffId, String changedBy) {
        Complaint c = find(complaintId);
        if (c.getStatus().isClosed()) {
            throw new IllegalStateException("This complaint is already " + c.getStatus().getLabel().toLowerCase() + ".");
        }
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff member not found"));
        c.setAssignedStaff(staff);
        if (c.getStatus() == Status.SUBMITTED || c.getStatus() == Status.ACKNOWLEDGED) {
            c.setStatus(Status.ASSIGNED);
        }
        Complaint saved = complaintRepository.save(c);
        record(saved, saved.getStatus(), "Assigned to " + staff.getName() + " (" + staff.getSpecialization() + ")", changedBy);
        return saved;
    }

    @Transactional
    public Complaint updateStatus(Long complaintId, Status newStatus, String remarks, String changedBy) {
        boolean noRemarks = remarks == null || remarks.isBlank();
        if ((newStatus == Status.RESOLVED || newStatus == Status.REJECTED) && noRemarks) {
            throw new IllegalArgumentException("Remarks are required when resolving or rejecting a complaint.");
        }
        Complaint c = find(complaintId);
        if ((newStatus == Status.ASSIGNED || newStatus == Status.IN_PROGRESS) && c.getAssignedStaff() == null) {
            throw new IllegalArgumentException("Assign a staff member before setting the status to " + newStatus.getLabel() + ".");
        }
        c.setStatus(newStatus);
        if (!noRemarks) {
            c.setAdminRemarks(remarks.trim());
        }
        Complaint saved = complaintRepository.save(c);
        record(saved, newStatus, noRemarks ? "Status changed to " + newStatus.getLabel() : remarks.trim(), changedBy);
        return saved;
    }

    // ------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public Complaint getForUser(Long id, AppUser user) {
        Complaint c = find(id);
        if (user.getRole() != Role.ADMIN && !isOwner(c, user)) {
            throw new AccessDeniedException("You cannot access this complaint.");
        }
        return c;
    }

    /** Search with optional filters. Pass a student to restrict results to that student's complaints. */
    @Transactional(readOnly = true)
    public Page<Complaint> search(AppUser student, String q, Category category, Priority priority, Status status, int page) {
        Specification<Complaint> spec = (root, query, cb) -> cb.conjunction();
        if (student != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("student"), student));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("description")), like),
                    cb.like(cb.lower(root.get("location")), like)));
        }
        if (category != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
        if (priority != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), priority));
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        return complaintRepository.findAll(spec,
                PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public List<Complaint> recentFor(AppUser student) {
        return complaintRepository.findTop5ByStudentOrderByCreatedAtDesc(student);
    }

    public List<Complaint> recentAll() {
        return complaintRepository.findTop5ByOrderByCreatedAtDesc();
    }

    public DashboardStats statsFor(AppUser student) {
        return new DashboardStats(
                complaintRepository.countByStudent(student),
                complaintRepository.countByStudentAndStatusIn(student, Status.pending()),
                complaintRepository.countByStudentAndStatus(student, Status.IN_PROGRESS),
                complaintRepository.countByStudentAndStatus(student, Status.RESOLVED),
                complaintRepository.countByStudentAndStatus(student, Status.REJECTED));
    }

    public DashboardStats statsAll() {
        return new DashboardStats(
                complaintRepository.count(),
                complaintRepository.countByStatusIn(Status.pending()),
                complaintRepository.countByStatus(Status.IN_PROGRESS),
                complaintRepository.countByStatus(Status.RESOLVED),
                complaintRepository.countByStatus(Status.REJECTED));
    }

    public Map<Category, Long> categoryStats() {
        Map<Category, Long> result = new LinkedHashMap<>();
        for (Category c : Category.values()) result.put(c, 0L);
        for (Object[] row : complaintRepository.countGroupedByCategory()) {
            result.put((Category) row[0], (Long) row[1]);
        }
        return result;
    }

    // ------------------------------------------------------------ helpers

    private Complaint find(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found"));
    }

    private boolean isOwner(Complaint c, AppUser user) {
        return Objects.equals(c.getStudent().getId(), user.getId());
    }

    private void ensureOwner(Complaint c, AppUser user) {
        if (!isOwner(c, user)) throw new AccessDeniedException("You can only change your own complaints.");
    }

    private void ensureEditable(Complaint c) {
        if (!c.getStatus().isEditableByStudent()) {
            throw new IllegalStateException("This complaint is already " + c.getStatus().getLabel().toLowerCase()
                    + " and can no longer be edited or cancelled.");
        }
    }

    private void apply(ComplaintForm form, Complaint c) {
        c.setTitle(form.getTitle().trim());
        c.setDescription(form.getDescription().trim());
        c.setCategory(form.getCategory());
        c.setPriority(form.getPriority());
        c.setLocation(form.getLocation().trim());
    }

    private void record(Complaint complaint, Status status, String remarks, String changedBy) {
        ComplaintHistory h = new ComplaintHistory();
        h.setComplaint(complaint);
        h.setStatus(status);
        h.setRemarks(remarks);
        h.setChangedBy(changedBy);
        historyRepository.save(h);
    }
}
