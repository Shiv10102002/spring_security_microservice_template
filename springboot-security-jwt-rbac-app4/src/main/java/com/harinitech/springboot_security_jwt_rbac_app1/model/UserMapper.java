package com.harinitech.springboot_security_jwt_rbac_app1.model;

import com.harinitech.springboot_security_jwt_rbac_app1.entity.User;

public class UserMapper {

    public static UserResponseDto toResponse(User user) {
        return UserResponseDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().getName())
                .status(user.getStatus())
                .enabled(user.isEnabled())
                .accountLocked(user.isAccountLocked())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .lastLoginAt(user.getLastLoginAt())
                .lastLoginIp(user.getLastLoginIp())
                .lastLoginDevice(user.getLastLoginDevice())
                .passwordChangedAt(user.getPasswordChangedAt())
                .build();
    }

    public static UserSummaryDto toSummary(User user) {
        return UserSummaryDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().getName())
                .status(user.getStatus())
                .enabled(user.isEnabled())
                .build();
    }
}