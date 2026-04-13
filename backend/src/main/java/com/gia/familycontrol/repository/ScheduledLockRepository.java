package com.gia.familycontrol.repository;

import com.gia.familycontrol.model.ScheduledLock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScheduledLockRepository extends JpaRepository<ScheduledLock, Long> {
    List<ScheduledLock> findByParentId(Long parentId);
    List<ScheduledLock> findByIsActiveTrue();
}
