package edu.gcu.cst323.employeemanager.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Fails startup immediately when DB_PASSWORD has not been supplied.
 *
 * <p>Removing the default from application.properties is not enough on its own.
 * Spring Boot binds spring.datasource.password through a resolver that ignores
 * unresolvable placeholders, so an unset DB_PASSWORD does not raise an error: the
 * literal text "${DB_PASSWORD}" is handed to the driver as the password. The
 * application then starts, connects, and fails with "Access denied for user" -
 * which points at the database instead of at the missing configuration.
 *
 * <p>This runs as an EnvironmentPostProcessor at LOWEST_PRECEDENCE, meaning after
 * application.properties and any profile-specific file have been loaded but before
 * a single bean is created, so the failure lands before Flyway or Hikari ever open
 * a connection.
 */
public class RequiredDatabasePasswordValidator implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY = "spring.datasource.password";

    private static final String MESSAGE =
            "DB_PASSWORD is not set. This application has no default database password by design: "
            + "a committed fallback would let a misconfigured deployment start and then fail with an "
            + "opaque authentication error. Set the DB_PASSWORD environment variable (or pass "
            + "-DDB_PASSWORD=...) before starting. See the Quick start section of README.md for the "
            + "local development value. An intentionally empty password is still valid: set DB_PASSWORD=";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String value;
        try {
            value = environment.getProperty(PROPERTY);
        } catch (IllegalArgumentException ex) {
            // Thrown by the environment's own resolver when the placeholder has no value.
            throw new IllegalStateException(MESSAGE, ex);
        }
        if (value != null && value.contains("${")) {
            // Belt and braces: a resolver configured to ignore unresolvable placeholders
            // hands the raw text back rather than throwing.
            throw new IllegalStateException(MESSAGE);
        }
    }

    @Override
    public int getOrder() {
        // After ConfigDataEnvironmentPostProcessor, so the properties files are in place.
        return Ordered.LOWEST_PRECEDENCE;
    }
}
