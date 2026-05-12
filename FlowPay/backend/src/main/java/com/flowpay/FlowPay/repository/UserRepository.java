package com.flowpay.FlowPay.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.flowpay.FlowPay.entity.User;

/**
 * Spring Data JPA repository for {@link User} entities.
 *
 * <p>Provides standard CRUD operations via {@link JpaRepository} plus two
 * custom query methods for email-based lookups.</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their email address.
     * Used during login and JWT validation.
     *
     * @param email the email address to search for
     * @return an {@link Optional} containing the user, or empty if not found
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user with the given email already exists.
     * Used during signup to enforce email uniqueness.
     *
     * @param email the email address to check
     * @return {@code true} if a user with that email exists; {@code false} otherwise
     */
    boolean existsByEmail(String email);
}
