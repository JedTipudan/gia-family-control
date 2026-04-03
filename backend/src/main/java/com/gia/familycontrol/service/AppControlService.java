package com.gia.familycontrol.service;

import com.gia.familycontrol.dto.CommandDto;
import com.gia.familycontrol.firebase.FcmService;
import com.gia.familycontrol.model.AppControl;
import com.gia.familycontrol.model.Device;
import com.gia.familycontrol.model.User;
import com.gia.familycontrol.repository.AppControlRepository;
import com.gia.familycontrol.repository.DeviceRepository;
import com.gia.familycontrol.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppControlService {

    private final AppControlRepository appControlRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;
    private final com.gia.familycontrol.repository.AppRepository appRepository;

    public List<AppControl> getAppControls(Long deviceId) {
        return appControlRepository.findByDeviceId(deviceId);
    }

    @Transactional
    public AppControl setAppControl(String parentEmail, CommandDto.AppControlRequest request) {
        User parent = userRepository.findByEmail(parentEmail)
                .orElseThrow(() -> new RuntimeException("Parent not found"));
        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new RuntimeException("Device not found"));

        AppControl control = appControlRepository
                .findByDeviceIdAndPackageName(device.getId(), request.getPackageName())
                .orElse(new AppControl());

        control.setParentId(parent.getId());
        control.setDeviceId(device.getId());
        control.setPackageName(request.getPackageName());
        control.setControlType(AppControl.ControlType.valueOf(request.getControlType()));

        if (request.getScheduleStart() != null)
            control.setScheduleStart(LocalTime.parse(request.getScheduleStart()));
        if (request.getScheduleEnd() != null)
            control.setScheduleEnd(LocalTime.parse(request.getScheduleEnd()));
        control.setScheduleDays(request.getScheduleDays());

        appControlRepository.save(control);

        boolean block = control.getControlType() == AppControl.ControlType.BLOCKED;
        fcmService.sendAppBlockCommand(device.getFcmToken(), request.getPackageName(), block);

        return control;
    }

    @Transactional
    public void removeAppControl(Long deviceId, String packageName) {
        appControlRepository.deleteByDeviceIdAndPackageName(deviceId, packageName);
    }
    
    public List<com.gia.familycontrol.model.App> getInstalledApps(Long deviceId) {
        return appRepository.findByDeviceId(deviceId);
    }
    
    @Transactional
    public void syncApps(String childEmail, List<CommandDto.AppInfo> apps) {
        User child = userRepository.findByEmail(childEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Device device = deviceRepository.findByUserId(child.getId())
                .orElseThrow(() -> new RuntimeException("Device not found"));
        
        // Delete old apps
        appRepository.deleteByDeviceId(device.getId());
        
        // Insert new apps
        for (CommandDto.AppInfo appInfo : apps) {
            com.gia.familycontrol.model.App app = new com.gia.familycontrol.model.App();
            app.setDeviceId(device.getId());
            app.setPackageName(appInfo.getPackageName());
            app.setAppName(appInfo.getAppName());
            app.setIsSystem(appInfo.getIsSystem() != null ? appInfo.getIsSystem() : false);
            appRepository.save(app);
        }
    }
}
