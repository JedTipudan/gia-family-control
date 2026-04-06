package com.gia.familycontrol.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "commands")
@Data
public class Command {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "target_device_id", nullable = false)
    private Long targetDeviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "command_type", nullable = false)
    private CommandType commandType;

    @Column(columnDefinition = "JSON")
    private String payload;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @Column(name = "sent_at")
    private LocalDateTime sentAt = LocalDateTime.now();

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    public enum CommandType {
        LOCK, UNLOCK, BLOCK_APP, UNBLOCK_APP, SOS, EMERGENCY,
        GRANT_TEMP_ACCESS, REVOKE_TEMP_ACCESS,
        ENABLE_LAUNCHER, DISABLE_LAUNCHER,
        SET_PIN, HIDE_APP, UNHIDE_APP
    }
    public enum Status { PENDING, DELIVERED, EXECUTED, FAILED }
}
