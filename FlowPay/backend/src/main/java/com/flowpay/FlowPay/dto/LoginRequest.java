package com.flowpay.FlowPay.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO for the login request body.
 *
 * <p>Received at {@code POST /auth/login}. The supplied password is
 * compared against the BCrypt-hashed value stored in the database.</p>
 */
@Getter
@Setter
public class LoginRequest {

    /** Registered email address of the user. */
    public String email;

    /** Plain-text password submitted by the user (compared via BCrypt). */
    public String password;
}
