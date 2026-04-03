package com.gia.familycontrol.repository;

import com.gia.familycontrol.model.App;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppRepository extends JpaRepository<App, Long> {
    List<App> findByDeviceId(Long deviceId);
    void deleteByDeviceId(Long deviceId);
}
