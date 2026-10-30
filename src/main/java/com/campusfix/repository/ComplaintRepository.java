package com.campusfix.repository;

import com.campusfix.model.AppUser;
import com.campusfix.model.Complaint;
import com.campusfix.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long>, JpaSpecificationExecutor<Complaint> {

    long countByStatus(Status status);
    long countByStatusIn(Collection<Status> statuses);
    long countByStudent(AppUser student);
    long countByStudentAndStatus(AppUser student, Status status);
    long countByStudentAndStatusIn(AppUser student, Collection<Status> statuses);

    boolean existsByAssignedStaffId(Long staffId);

    List<Complaint> findTop5ByOrderByCreatedAtDesc();
    List<Complaint> findTop5ByStudentOrderByCreatedAtDesc(AppUser student);

    @Query("select c.category, count(c) from Complaint c group by c.category")
    List<Object[]> countGroupedByCategory();
}
