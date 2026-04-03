package com.gia.familycontrol.repository;

import com.gia.familycontrol.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Long> {
    List<Location> findByDeviceIdOrderByRecordedAtDesc(Long deviceId, Pageable pageable);
    List<Location> findTop1ByDeviceIdOrderByRecordedAtDesc(Long deviceId);
}
