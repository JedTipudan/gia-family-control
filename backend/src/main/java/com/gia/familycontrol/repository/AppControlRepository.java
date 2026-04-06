package com.gia.familycontrol.repository;

import com.gia.familycontrol.model.AppControl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface AppControlRepository extends JpaRepository<AppControl, Long> {
    List<AppControl> findByDeviceId(Long deviceId);

    // Find by deviceId + packageName + controlType (unique per type)
    Optional<AppControl> findByDeviceIdAndPackageNameAndControlType(
        Long deviceId, String packageName, AppControl.ControlType controlType);

    // Keep old one for backward compat
    Optional<AppControl> findByDeviceIdAndPackageName(Long deviceId, String packageName);

    // Delete only a specific controlType row — not all rows for that package
    @Modifying
    @Query("DELETE FROM AppControl a WHERE a.deviceId = :deviceId AND a.packageName = :packageName AND a.controlType = :controlType")
    void deleteByDeviceIdAndPackageNameAndControlType(
        Long deviceId, String packageName, AppControl.ControlType controlType);

    // Delete all rows for a package (used by unpair)
    void deleteByDeviceIdAndPackageName(Long deviceId, String packageName);
}
