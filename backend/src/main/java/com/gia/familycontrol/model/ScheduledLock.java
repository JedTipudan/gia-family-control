package com.gia.familycontrol.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalTime;

@Entity
@Table(name = "scheduled_locks")
@Data
public class ScheduledLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id", nullable = false)
    private Long parentId;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    /** HH:mm — time to LOCK */
    @Column(name = "lock_time", nullable = false)
    private LocalTime lockTime;

    /** HH:mm — time to UNLOCK */
    @Column(name = "unlock_time", nullable = false)
    private LocalTime unlockTime;

    /** Comma-separated: MON,TUE,WED,THU,FRI,SAT,SUN  or empty = every day */
    @Column(name = "days")
    private String days = "";

    @Column(name = "label")
    private String label = "Bedtime";

    @Column(name = "is_active")
    private Boolean isActive = true;
}
