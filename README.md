# Employee Manager

A small Spring Boot CRUD web application built for **CST-323 Cloud Computing**.

The feature set is intentionally minimal. The point of the project is not the
application itself but moving it, unchanged, onto a series of cloud platforms
(Azure first) and observing what each platform asks of it. Everything that varies
between environments — database host, database name, credentials, HTTP port, TLS
mode — is read from environment variables, so the same jar runs everywhere.

## Domain

A single one-to-many relationship:

```
Department (1) ──────< (many) Employee
   id, name                    id, firstName, lastName, email, hireDate, department
```

## Tech stack

| Layer      | Choice                                       |
|------------|----------------------------------------------|
| Language   | Java 17                                      |
| Framework  | Spring Boot 3.4                              |
| Build      | Maven (wrapper included — no local install)  |
| Web        | Spring MVC + Thymeleaf                       |
| Styling    | Bootstrap 5 via CDN                          |
| Data       | Spring Data JPA / Hibernate                  |
| Database   | MySQL 8                                      |
| Migrations | Flyway                                       |
| Logging    | SLF4J over Logback                           |
| Health     | Spring Boot Actuator + a plain `/health`     |

## Prerequisites

- **JDK 17 or newer** (`java -version`)
- **Docker Desktop** — only to run MySQL locally. If MySQL is already installed
  natively, skip Docker and point the environment variables at that instance.

Maven itself is **not** required: `mvnw` / `mvnw.cmd` download it on first use.

## Quick start

```bash
docker compose up -d --wait
```

```bash
./mvnw spring-boot:run
```

On Windows `cmd` or PowerShell use `mvnw.cmd spring-boot:run` instead.

Then open <http://localhost:8080>.

The first startup runs the Flyway migrations, which create the two tables and
insert four departments and six employees, so the app has data immediately.

To stop the database again:

```bash
docker compose down
```

Add `-v` to that command to delete the volume as well; Flyway will rebuild and
reseed the schema the next time the application starts.

## Pages

| URL                     | Method   | Purpose                                    |
|-------------------------|----------|--------------------------------------------|
| `/`                     | GET      | Home — project description and navigation  |
| `/employees`            | GET      | All employees, with Edit and Delete buttons |
| `/employees/new`        | GET      | Blank form (shared template)               |
| `/employees`            | POST     | **Create**                                  |
| `/employees/{id}`       | GET      | Detail view, including the department       |
| `/employees/{id}/edit`  | GET      | Populated form (same shared template)      |
| `/employees/{id}`       | POST     | **Update**                                  |
| `/employees/{id}/delete`| POST     | **Delete** (POST so a link prefetch cannot fire it) |
| `/health`               | GET      | Liveness probe — JSON, does not touch the DB |
| `/actuator/health`      | GET      | Readiness probe — includes DB connectivity  |
| `/actuator/info`        | GET      | Build and application metadata              |

All four CRUD operations are reachable from the UI: **Add Employee** in the nav
bar, and **View / Edit / Delete** on every row of the employee table.

## Configuration

No credential or host name is compiled into the application. Every value below is
read from an environment variable, falling back to a local-development default
defined in `src/main/resources/application.properties`.

| Variable                | Default            | Purpose                                        |
|-------------------------|--------------------|------------------------------------------------|
| `DB_HOST`               | `localhost`        | Database host name                              |
| `DB_PORT`               | `3306`             | Database port                                   |
| `DB_NAME`               | `employeedb`       | Schema name                                     |
| `DB_USER`               | `employee_app`     | Database user                                   |
| `DB_PASSWORD`           | `employee_app_pw`  | Database password                               |
| `DB_SSL_MODE`           | `DISABLED`         | `REQUIRED` for managed MySQL such as Azure      |
| `DB_POOL_MAX`           | `10`               | Maximum Hikari pool size                        |
| `DB_POOL_MIN`           | `2`                | Minimum idle connections                        |
| `PORT`                  | `8080`             | HTTP port the app binds to                      |
| `FLYWAY_ENABLED`        | `true`             | Set `false` if migrations are run separately    |
| `APP_LOG_LEVEL`         | `INFO`             | Level for this application's own loggers        |
| `JPA_SHOW_SQL`          | `false`            | Echo generated SQL, for debugging only          |
| `ACTUATOR_ENDPOINTS`    | `health,info`      | Which actuator endpoints are exposed            |
| `ACTUATOR_HEALTH_DETAILS`| `always`          | Set `never` to hide component detail publicly   |

> **The defaults are for local development only.** They match `docker-compose.yml`
> so the app runs out of the box on a laptop. Any deployed environment must supply
> its own `DB_USER` and `DB_PASSWORD` from the platform's secret store — Azure App
> Service application settings, Key Vault references, container secrets, and so on.
> `ACTUATOR_HEALTH_DETAILS=never` is also worth setting on anything public-facing,
> since the default `always` reveals component-level health detail to anonymous callers.

Overriding for a different environment is just environment variables:

```bash
DB_HOST=my-server.mysql.database.azure.com DB_SSL_MODE=REQUIRED DB_USER=appuser DB_PASSWORD='...' java -jar target/employee-manager.jar
```

## Database and migrations

Flyway owns the schema; Hibernate is set to `validate` and never alters tables.
Migrations live in `src/main/resources/db/migration`:

- `V1__create_schema.sql` — the `department` and `employee` tables plus the
  foreign key between them
- `V2__seed_data.sql` — four departments and six employees

Flyway records what it has applied in a `flyway_schema_history` table, so pointing
the app at a fresh cloud database provisions it automatically, and pointing it at
an existing one is a no-op. That is what makes redeploying to a new platform a
matter of setting variables rather than running scripts by hand.

To add a change later, add `V3__...sql` rather than editing an applied migration.

Inspecting the local database directly:

```bash
docker compose exec mysql mysql -uemployee_app -pemployee_app_pw employeedb -e "SELECT * FROM employee;"
```

## Logging

SLF4J over Logback, configured in `src/main/resources/logback-spring.xml`. Every
CRUD operation is logged at **INFO** by `EmployeeService`:

```
2026-08-30 15:57:34.481 INFO  [http-nio-8080-exec-8] e.g.c.e.service.EmployeeService - CREATE employee: id=5 name=Alan Turing email=alan.turing@example.com department=Engineering
2026-08-30 15:57:12.851 INFO  [http-nio-8080-exec-1] e.g.c.e.service.EmployeeService - DELETE employee: id=1 name=Ada Lovelace
```

Output goes to the console only. That is deliberate: cloud instances are
disposable and their local disks are not durable, so stdout — which every
platform's log collector already reads — is the one destination that works
unchanged on all of them.

## Tests

```bash
./mvnw test
```

The suite runs against an in-memory H2 database in MySQL compatibility mode, so
no MySQL container is needed. It drives all four CRUD operations through MockMvc,
which also renders every Thymeleaf template for real.

## Build

```bash
./mvnw clean package
```

Produces a self-contained `target/employee-manager.jar`:

```bash
java -jar target/employee-manager.jar
```

## Project layout

```
employee-manager/
├── docker-compose.yml              local MySQL
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/           Maven wrapper - no local Maven needed
└── src/
    ├── main/
    │   ├── java/edu/gcu/cst323/employeemanager/
    │   │   ├── EmployeeManagerApplication.java
    │   │   ├── config/DepartmentFormatter.java     dropdown <-> entity binding
    │   │   ├── controller/                          Home, Employee, Health
    │   │   ├── model/                               Department, Employee
    │   │   ├── repository/                          Spring Data JPA interfaces
    │   │   └── service/                             CRUD plus the INFO logging
    │   └── resources/
    │       ├── application.properties               all config externalized
    │       ├── logback-spring.xml
    │       ├── db/migration/                        Flyway V1 schema, V2 seed
    │       ├── static/css/app.css
    │       └── templates/                           Thymeleaf views
    └── test/                                        H2-backed CRUD flow test
```

## Notes for cloud deployment

The application is packaged as one executable jar and configured entirely through
environment variables, which is what each platform's deployment story needs:

1. Provision a managed MySQL database and create an empty schema.
2. Set `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` and `DB_SSL_MODE=REQUIRED`
   in the platform's application settings.
3. Deploy the jar. Flyway builds the schema and seeds it on first start.
4. Point the platform's health probe at `/actuator/health` (readiness) or
   `/health` (liveness).

Nothing in the source tree changes between platforms.
