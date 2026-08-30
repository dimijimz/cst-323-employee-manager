package edu.gcu.cst323.employeemanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

/**
 * Entry point for the CST-323 Employee Manager.
 *
 * <p>The application is intentionally small: its purpose is to be redeployed onto
 * several cloud platforms, so every environment-specific value (database host,
 * credentials, HTTP port) is supplied externally rather than baked into the jar.
 */
@SpringBootApplication
public class EmployeeManagerApplication {

    private static final Logger log = LoggerFactory.getLogger(EmployeeManagerApplication.class);

    public static void main(String[] args) {
        Environment env = SpringApplication.run(EmployeeManagerApplication.class, args).getEnvironment();
        // Logged without credentials so the startup line is safe to keep in cloud log streams.
        log.info("Employee Manager started on port {} against datasource {}",
                env.getProperty("server.port"),
                env.getProperty("spring.datasource.url"));
    }
}
