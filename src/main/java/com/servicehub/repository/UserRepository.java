package com.servicehub.repository;

import com.servicehub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByFullNameContainingIgnoreCase(String name);

    // JPQL — Java Persistence Query Language
    @Query("SELECT u FROM User u WHERE u.createdAt >= :date")
    List<User> findRecentUsers(@Param("date") LocalDateTime date);

    // SQL nativo quando necessário
    @Query(value = "SELECT * FROM users WHERE is_active = true LIMIT :limit", nativeQuery = true)
    List<User> findActiveUsers(@Param("limit") int limit);

}
