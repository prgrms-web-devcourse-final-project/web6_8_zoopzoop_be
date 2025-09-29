package org.tuna.zoopzoop.backend.global.config.sentry;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProfileChecker {
    private final Environment environment;

    public ProfileChecker(Environment environment) {
        this.environment = environment;
    }

    public String[] getActiveProfiles() {
        return environment.getActiveProfiles();
    }
}