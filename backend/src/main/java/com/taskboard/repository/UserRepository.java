package com.taskboard.repository;

import com.taskboard.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by username.
     */
    Optional<User> findByUsername(String username);

    /**
     * Find a user by email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Find an active user by username.
     */
    Optional<User> findByUsernameAndActiveTrue(String username);

    /**
     * Check if a username exists.
     */
    boolean existsByUsername(String username);

    /**
     * Check if an email exists.
     */
    boolean existsByEmail(String email);
}

