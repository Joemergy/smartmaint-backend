package com.smartmaint.repository;

import com.smartmaint.model.TareaAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TareaAuditLogRepository extends JpaRepository<TareaAuditLog, Long> {
}
