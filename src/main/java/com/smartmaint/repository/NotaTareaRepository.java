package com.smartmaint.repository;

import com.smartmaint.model.NotaTarea;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotaTareaRepository extends JpaRepository<NotaTarea, Long> {
    List<NotaTarea> findByTareaIdOrderByFechaCreacionAsc(Long tareaId);
}
