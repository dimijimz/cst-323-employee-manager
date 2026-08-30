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

Start the database:

```bash
docker compose up -d --wait
```

Then run the app. `DB_PASSWORD` has no default, so it has to be supplied — for local
development that is the password `docker-compose.yml` gives the container:

```bash
DB_PASSWORD=employee_app_pw ./mvnw spring-boot:run
```

In PowerShell:

```bash
$env:DB_PASSWORD = 'employee_app_pw'; .\mvnw.cmd spring-boot:run
```

Leaving `DB_PASSWORD` unset is not a silent failure — the app refuses to start and
says `Could not resolve placeholder 'DB_PASSWORD'`. That is deliberate; see
[Configuration](#configuration).

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
| `DB_PASSWORD`           | **none — required** | Database password. No fallback; unset fails at startup |
| `DB_SSL_MODE`           | `DISABLED`         | `REQUIRED` for managed MySQL such as Azure      |
| `DB_POOL_MAX`           | `10`               | Maximum Hikari pool size                        |
| `DB_POOL_MIN`           | `2`                | Minimum idle connections                        |
| `PORT`                  | `8080`             | HTTP port the app binds to                      |
| `FLYWAY_ENABLED`        | `true`             | Set `false` if migrations are run separately    |
| `APP_LOG_LEVEL`         | `INFO`             | Level for this application's own loggers        |
| `JPA_SHOW_SQL`          | `false`            | Echo generated SQL, for debugging only          |
| `ACTUATOR_ENDPOINTS`    | `health,info`      | Which actuator endpoints are exposed            |
| `ACTUATOR_HEALTH_DETAILS`| `always`          | Set `never` to hide component detail publicly   |

> **`DB_PASSWORD` has no default, on purpose.** Every other setting falls back to a
> local development value, but a committed fallback password is a trap: a deployment
> that forgets to set `DB_PASSWORD` would start, connect with the placeholder, and fail
> with an opaque `Access denied for user` error that points at the database rather than
> at the missing configuration.
>
> Removing the default is not sufficient by itself. Spring Boot binds
> `spring.datasource.password` through a resolver that *ignores* unresolvable
> placeholders, so an unset `DB_PASSWORD` is silently passed to the driver as the
> literal string `${DB_PASSWORD}`. `RequiredDatabasePasswordValidator` closes that gap:
> it runs as an `EnvironmentPostProcessor` before any bean is created, so startup stops
> before Flyway or Hikari open a connection:
>
> ```
> java.lang.IllegalStateException: DB_PASSWORD is not set. This application has no
> default database password by design: a committed fallback would let a misconfigured
> deployment start and then fail with an opaque authentication error. Set the
> DB_PASSWORD environment variable (or pass -DDB_PASSWORD=...) before starting.
>   Caused by: PlaceholderResolutionException: Could not resolve placeholder 'DB_PASSWORD'
> ```
>
> An intentionally empty password is still allowed: set `DB_PASSWORD=` explicitly.
>
> Supply it from the platform's secret store — Azure App Service application settings,
> Key Vault references, container secrets, and so on. The remaining defaults are for
> local development only and match `docker-compose.yml`.
>
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

Produces a self-contained `target/employee-manager.jar`, which needs the same
environment variables as `spring-boot:run`:

```bash
DB_PASSWORD=employee_app_pw java -jar target/employee-manager.jar
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
    │   │   ├── config/
    │   │   │   ├── DepartmentFormatter.java          dropdown <-> entity binding
    │   │   │   └── RequiredDatabasePasswordValidator.java  fail fast if DB_PASSWORD unset
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
