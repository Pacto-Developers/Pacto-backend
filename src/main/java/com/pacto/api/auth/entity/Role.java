package com.pacto.api.auth.entity;

import com.pacto.api.common.exception.InvalidRoleException;
import com.pacto.api.common.exception.MissingRoleException;

public enum Role {
    BLOGGER,
    ADVERTISER;

    public static Role from(String role) {
        if (role == null || role.isBlank()) {
            throw new MissingRoleException();
        }

        try {
            return Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRoleException();
        }
    }
}
