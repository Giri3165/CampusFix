package com.campusfix.service;

import com.campusfix.dto.ComplaintForm;
import com.campusfix.model.*;
import com.campusfix.repository.ComplaintHistoryRepository;
import com.campusfix.repository.ComplaintRepository;
import com.campusfix.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock ComplaintRepository complaintRepository;
    @Mock ComplaintHistoryRepository historyRepository;
    @Mock StaffRepository staffRepository;
    @Mock FileStorageService storageService;
    @InjectMocks ComplaintService service;

    private AppUser user(long id, Role role) {
        AppUser u = new AppUser();
        u.setId(id);
        u.setFullName("User " + id);
        u.setRole(role);
        return u;
    }

    private Complaint complaint(long id, AppUser owner, Status status) {
        Complaint c = new Complaint();
        c.setId(id);
        c.setStudent(owner);
        c.setStatus(status);
        c.setTitle("Broken fan");
        return c;
    }

    private ComplaintForm form() {
        ComplaintForm f = new ComplaintForm();
        f.setTitle("Fan not working");
        f.setDescription("The ceiling fan in room 204 does not turn on.");
        f.setCategory(Category.ELECTRICAL);
        f.setPriority(Priority.HIGH);
        f.setLocation("Block B, Room 204");
        return f;
    }

    // ---- complaint submission ----

    @Test
    void submit_createsSubmittedComplaintAndHistoryEntry() {
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));
        AppUser student = user(1, Role.STUDENT);

        Complaint saved = service.submit(form(), null, student);

        assertEquals(Status.SUBMITTED, saved.getStatus());
        assertEquals("Fan not working", saved.getTitle());
        assertSame(student, saved.getStudent());
        verify(historyRepository).save(any(ComplaintHistory.class));
        verifyNoInteractions(storageService);
    }

    // ---- assignment ----

    @Test
    void assign_setsStaffAndMovesToAssigned() {
        Staff staff = new Staff();
        staff.setId(5L);
        staff.setName("Ravi");
        staff.setSpecialization("Electrician");
        when(complaintRepository.findById(10L)).thenReturn(Optional.of(complaint(10, user(1, Role.STUDENT), Status.SUBMITTED)));
        when(staffRepository.findById(5L)).thenReturn(Optional.of(staff));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));

        Complaint result = service.assign(10L, 5L, "admin@campusfix.local");

        assertEquals(Status.ASSIGNED, result.getStatus());
        assertSame(staff, result.getAssignedStaff());
    }

    @Test
    void assign_closedComplaint_isRejected() {
        when(complaintRepository.findById(10L)).thenReturn(Optional.of(complaint(10, user(1, Role.STUDENT), Status.RESOLVED)));

        assertThrows(IllegalStateException.class, () -> service.assign(10L, 5L, "admin"));
    }

    // ---- status updates ----

    @Test
    void updateStatus_resolvedWithoutRemarks_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.updateStatus(10L, Status.RESOLVED, "  ", "admin"));
        verifyNoInteractions(complaintRepository);
    }

    @Test
    void updateStatus_inProgressWithoutStaff_isRejected() {
        when(complaintRepository.findById(10L)).thenReturn(Optional.of(complaint(10, user(1, Role.STUDENT), Status.SUBMITTED)));

        assertThrows(IllegalArgumentException.class, () -> service.updateStatus(10L, Status.IN_PROGRESS, "start", "admin"));
    }

    @Test
    void updateStatus_resolvedWithRemarks_savesStatusAndRemarks() {
        when(complaintRepository.findById(10L)).thenReturn(Optional.of(complaint(10, user(1, Role.STUDENT), Status.IN_PROGRESS)));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));

        Complaint result = service.updateStatus(10L, Status.RESOLVED, "  Fan motor replaced  ", "admin");

        assertEquals(Status.RESOLVED, result.getStatus());
        assertEquals("Fan motor replaced", result.getAdminRemarks());
        verify(historyRepository).save(any(ComplaintHistory.class));
    }

    // ---- student rules + access control ----

    @Test
    void update_assignedComplaint_cannotBeEditedByStudent() {
        AppUser owner = user(1, Role.STUDENT);
        when(complaintRepository.findById(10L)).thenReturn(Optional.of(complaint(10, owner, Status.ASSIGNED)));

        assertThrows(IllegalStateException.class, () -> service.update(10L, form(), null, owner));
    }

    @Test
    void cancel_submittedComplaint_deletesIt() {
        AppUser owner = user(1, Role.STUDENT);
        Complaint c = complaint(10, owner, Status.SUBMITTED);
        when(complaintRepository.findById(10L)).thenReturn(Optional.of(c));

        service.cancel(10L, owner);

        verify(complaintRepository).delete(c);
    }

    @Test
    void cancel_otherStudentsComplaint_isForbidden() {
        when(complaintRepository.findById(10L)).thenReturn(Optional.of(complaint(10, user(1, Role.STUDENT), Status.SUBMITTED)));

        assertThrows(AccessDeniedException.class, () -> service.cancel(10L, user(2, Role.STUDENT)));
        verify(complaintRepository, never()).delete(org.mockito.ArgumentMatchers.<Complaint>any());
    }

    @Test
    void getForUser_studentCannotReadOthers_butAdminCan() {
        Complaint c = complaint(10, user(1, Role.STUDENT), Status.SUBMITTED);
        when(complaintRepository.findById(10L)).thenReturn(Optional.of(c));

        assertThrows(AccessDeniedException.class, () -> service.getForUser(10L, user(2, Role.STUDENT)));
        assertSame(c, service.getForUser(10L, user(99, Role.ADMIN)));
        assertSame(c, service.getForUser(10L, user(1, Role.STUDENT)));
    }
}
