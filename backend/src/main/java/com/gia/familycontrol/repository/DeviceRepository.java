package com.gia.familycontrol.repository;

import com.gia.familycontrol.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByUserId(Long userId);
}
