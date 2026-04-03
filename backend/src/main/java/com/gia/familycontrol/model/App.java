package com.gia.familycontrol.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "apps")
@Data
public class App {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long deviceId;

    @Column(nullable = false)
    private String packageName;

    @Column(nullable = false)
    private String appName;

    @Column(nullable = false)
    private Boolean isSystem = false;
}
