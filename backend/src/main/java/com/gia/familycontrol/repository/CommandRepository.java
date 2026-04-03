package com.gia.familycontrol.repository;

import com.gia.familycontrol.model.Command;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommandRepository extends JpaRepository<Command, Long> {
    List<Command> findByTargetDeviceIdOrderBySentAtDesc(Long deviceId);
}
