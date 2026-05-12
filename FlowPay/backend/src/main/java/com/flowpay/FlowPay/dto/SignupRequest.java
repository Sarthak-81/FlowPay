package com.flowpay.FlowPay.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO for the user registration request body.
 *
 * <p>Received at {@code POST /auth/signup}. The password is BCrypt-hashed
 * before being persisted; it is never stored in plain text.</p>
 */
@Getter
@Setter
public class SignupRequest {

    /** User's full display name. */
    public String name;

    /** Email address to register. Must be unique across all users. */
    public String email;

    /** Plain-text password chosen by the user. Will be BCrypt-encoded before storage. */
    public String password;
}
