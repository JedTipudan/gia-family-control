package com.gia.familycontrol.service;

import com.gia.familycontrol.firebase.FcmService;
import com.gia.familycontrol.model.Device;
import com.gia.familycontrol.model.ScheduledLock;
import com.gia.familycontrol.model.User;
import com.gia.familycontrol.repository.DeviceRepository;
import com.gia.familycontrol.repository.ScheduledLockRepository;
import com.gia.familycontrol.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledLockService {

    private final ScheduledLockRepository repo;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    public List<ScheduledLock> getByParent(Long parentId) {
        return repo.findByParentId(parentId);
    }

    @Transactional
    public ScheduledLock create(Long parentId, ScheduledLock body) {
        body.setId(null);
        body.setParentId(parentId);
        return repo.save(body);
    }

    @Transactional
    public ScheduledLock update(Long parentId, Long id, ScheduledLock body) {
        ScheduledLock existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));
        if (!existing.getParentId().equals(parentId))
            throw new RuntimeException("Unauthorized");
        body.setId(id);
        body.setParentId(parentId);
        return repo.save(body);
    }

    @Transactional
    public void delete(Long parentId, Long id) {
        ScheduledLock existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));
        if (!existing.getParentId().equals(parentId))
            throw new RuntimeException("Unauthorized");
        repo.deleteById(id);
    }

    /** Runs every minute — checks if any schedule matches current time */
    @Scheduled(cron = "0 * * * * *")
    public void processTick() {
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        String today = DayOfWeek.from(LocalDateTime.now().getDayOfWeek()).name().substring(0, 3); // MON,TUE...

        List<ScheduledLock> schedules = repo.findByIsActiveTrue();
        for (ScheduledLock s : schedules) {
            if (!matchesDay(s.getDays(), today)) continue;

            Device device = deviceRepository.findById(s.getDeviceId()).orElse(null);
            if (device == null || device.getFcmToken() == null) continue;

            if (s.getLockTime().equals(now)) {
                log.info("⏰ Scheduled LOCK for device {}", s.getDeviceId());
                fcmService.sendLockCommand(device.getFcmToken(), true);
                device.setIsLocked(true);
                deviceRepository.save(device);
            } else if (s.getUnlockTime().equals(now)) {
                log.info("⏰ Scheduled UNLOCK for device {}", s.getDeviceId());
                fcmService.sendLockCommand(device.getFcmToken(), false);
                device.setIsLocked(false);
                deviceRepository.save(device);
            }
        }
    }

    private boolean matchesDay(String days, String today) {
        if (days == null || days.isBlank()) return true; // every day
        for (String d : days.split(",")) {
            if (d.trim().equalsIgnoreCase(today)) return true;
        }
        return false;
    }
}
