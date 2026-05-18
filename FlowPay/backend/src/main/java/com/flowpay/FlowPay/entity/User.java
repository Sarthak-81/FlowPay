package com.flowpay.FlowPay.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity representing a registered user in the FlowPay system.
 *
 * <p>Users authenticate via email/password and receive a JWT on login.
 * The {@code role} field is used by Spring Security for authorization
 * (e.g. {@code ROLE_USER}, {@code ROLE_ADMIN}).</p>
 *
 * <p>Maps to the {@code users} table in the database.</p>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** User's full name. */
    private String name;

    /**
     * User's email address. Used as the login identifier and stored in the JWT subject.
     * Must be unique across all users.
     */
    @Column(unique = true)
    private String email;

    /** BCrypt-hashed password. Never stored in plain text. */
    private String password;

    /**
     * Spring Security role string (e.g. {@code ROLE_USER}, {@code ROLE_ADMIN}).
     * Must include the {@code ROLE_} prefix for Spring Security's
     * {@code hasRole()} / {@code hasAnyRole()} matchers to work correctly.
     */
    private String role;

    /** Timestamp when the user account was created. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
