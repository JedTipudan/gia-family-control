package com.gia.familycontrol.controller;

import com.gia.familycontrol.model.ScheduledLock;
import com.gia.familycontrol.model.User;
import com.gia.familycontrol.repository.UserRepository;
import com.gia.familycontrol.service.ScheduledLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduledLockController {

    private final ScheduledLockService service;
    private final UserRepository userRepository;

    private Long parentId(Principal p) {
        return userRepository.findByEmail(p.getName())
                .orElseThrow(() -> new RuntimeException("User not found")).getId();
    }

    @GetMapping
    public ResponseEntity<List<ScheduledLock>> list(Principal p) {
        return ResponseEntity.ok(service.getByParent(parentId(p)));
    }

    @PostMapping
    public ResponseEntity<ScheduledLock> create(Principal p, @RequestBody ScheduledLock body) {
        return ResponseEntity.ok(service.create(parentId(p), body));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduledLock> update(Principal p, @PathVariable Long id, @RequestBody ScheduledLock body) {
        return ResponseEntity.ok(service.update(parentId(p), id, body));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Principal p, @PathVariable Long id) {
        service.delete(parentId(p), id);
        return ResponseEntity.ok().build();
    }
}
