-- V1: base schema for the Employee Manager.
--
-- Flyway records this migration in flyway_schema_history, so redeploying the
-- application against an existing database is a no-op rather than an error.

CREATE TABLE department (
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_department_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE employee (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    first_name    VARCHAR(50) NOT NULL,
    last_name     VARCHAR(50) NOT NULL,
    email         VARCHAR(120) NOT NULL,
    hire_date     DATE        NOT NULL,
    department_id BIGINT      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_employee_email UNIQUE (email),
    CONSTRAINT fk_employee_department
        FOREIGN KEY (department_id) REFERENCES department (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- No explicit index on employee.department_id: InnoDB creates one automatically
-- for the foreign key, and a second copy would only cost write throughput.
