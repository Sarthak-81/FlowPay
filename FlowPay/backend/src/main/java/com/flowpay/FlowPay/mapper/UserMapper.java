package com.flowpay.FlowPay.mapper;

import com.flowpay.FlowPay.dto.SignupRequest;
import com.flowpay.FlowPay.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between {@link SignupRequest} DTOs and
 * {@link User} entities.
 *
 * <p>The {@code password} field is intentionally ignored here because it must
 * be BCrypt-encoded by {@link com.flowpay.FlowPay.service.AuthService} before
 * being set on the entity. The {@code role} and {@code createdAt} fields are
 * also excluded as they are set programmatically in the service layer.</p>
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Converts a {@link SignupRequest} to a {@link User} entity.
     * Password encoding, role assignment, and {@code createdAt} are handled
     * by the caller ({@link com.flowpay.FlowPay.service.AuthService}).
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    User toUser(SignupRequest request);
}
