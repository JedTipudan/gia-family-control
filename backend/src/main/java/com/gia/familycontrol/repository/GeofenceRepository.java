package com.gia.familycontrol.repository;

import com.gia.familycontrol.model.Geofence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GeofenceRepository extends JpaRepository<Geofence, Long> {
    List<Geofence> findByDeviceIdAndIsActiveTrue(Long deviceId);
    List<Geofence> findByParentId(Long parentId);
}
