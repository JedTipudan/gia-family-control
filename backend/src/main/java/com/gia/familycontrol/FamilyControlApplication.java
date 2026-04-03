package com.gia.familycontrol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FamilyControlApplication {
    public static void main(String[] args) {
        SpringApplication.run(FamilyControlApplication.class, args);
    }
}
