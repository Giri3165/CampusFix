package com.campusfix.repository;

import com.campusfix.model.ComplaintHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintHistoryRepository extends JpaRepository<ComplaintHistory, Long> {
}
