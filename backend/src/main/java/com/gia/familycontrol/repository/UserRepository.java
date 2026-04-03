package com.gia.familycontrol.repository;

import com.gia.familycontrol.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPairCode(String pairCode);
    boolean existsByEmail(String email);
    List<User> findByParentId(Long parentId);
}
