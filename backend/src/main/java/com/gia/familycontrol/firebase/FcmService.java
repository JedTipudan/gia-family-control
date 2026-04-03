package com.gia.familycontrol.firebase;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class FcmService {

    public void sendCommand(String fcmToken, String commandType, Map<String, String> data) {
        try {
            Message.Builder builder = Message.builder()
                    .setToken(fcmToken)
                    .putData("command", commandType)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build());

            if (data != null) {
                data.forEach(builder::putData);
            }

            String response = FirebaseMessaging.getInstance().send(builder.build());
            log.info("FCM sent: {} -> {}", commandType, response);
        } catch (FirebaseMessagingException e) {
            log.error("FCM send failed for command {}: {}", commandType, e.getMessage());
            throw new RuntimeException("Failed to send FCM command", e);
        }
    }

    public void sendLockCommand(String fcmToken, boolean lock) {
        sendCommand(fcmToken, lock ? "LOCK" : "UNLOCK", null);
    }

    public void sendAppBlockCommand(String fcmToken, String packageName, boolean block) {
        sendCommand(fcmToken, block ? "BLOCK_APP" : "UNBLOCK_APP",
                Map.of("packageName", packageName));
    }

    public void sendEmergencyAlert(String fcmToken, String message) {
        sendCommand(fcmToken, "EMERGENCY", Map.of("message", message));
    }
}
