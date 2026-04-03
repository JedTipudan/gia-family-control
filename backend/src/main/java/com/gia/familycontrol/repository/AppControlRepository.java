package com.gia.familycontrol.repository;

import com.gia.familycontrol.model.AppControl;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AppControlRepository extends JpaRepository<AppControl, Long> {
    List<AppControl> findByDeviceId(Long deviceId);
    Optional<AppControl> findByDeviceIdAndPackageName(Long deviceId, String packageName);
    void deleteByDeviceIdAndPackageName(Long deviceId, String packageName);
}
