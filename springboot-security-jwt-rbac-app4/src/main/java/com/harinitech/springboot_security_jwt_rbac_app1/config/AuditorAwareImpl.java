package com.harinitech.springboot_security_jwt_rbac_app1.config;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuditorAwareImpl implements AuditorAware<Long> {

    private static final Long SYSTEM_USER_ID = 0L;

    @Override
    public Optional<Long> getCurrentAuditor() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // No authentication context (startup jobs, schedulers, seeders)
        if (auth == null) {
            return Optional.of(SYSTEM_USER_ID);
        }

        // Anonymous user
        if (!auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.of(SYSTEM_USER_ID);
        }

        Object principal = auth.getPrincipal();

        if (principal == null) {
            return Optional.of(SYSTEM_USER_ID);
        }

        try {

            // JwtFilter stores principal as userId string
            return Optional.of(Long.parseLong(principal.toString()));

        } catch (NumberFormatException e) {

            // Fallback for invalid principal format
            return Optional.of(SYSTEM_USER_ID);
        }
    }
}