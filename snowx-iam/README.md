# SnowX IAM

SnowX IAM is a single-process IAM service migrated from the legacy MaxKey web
applications and their repository-owned Java modules. It is built as one Maven
module and produces one executable Spring Boot JAR.

Only backend capabilities consumed by `maxkey-web-vue-app` are in the migration
scope. The new service does not depend on repository-owned
`org.dromara.maxkey` Maven artifacts.

## Technology baseline

- JDK 21
- Spring Boot 4.1.0
- PostgreSQL
- MyBatis-Plus 3.5.17
- Flyway
- Maven

## Prerequisites

- JDK 21 available through `JAVA_HOME`
- Maven 3.9 or newer
- PostgreSQL 14 or newer

Create a database and user before starting the application:

```sql
CREATE USER snowx WITH PASSWORD 'snowx';
CREATE DATABASE snowx_iam OWNER snowx;
```

Flyway creates and seeds the schema automatically on the first startup.

## Build and run

```bash
mvn clean package
java -jar target/snowx-iam-1.0.0-SNAPSHOT.jar
```

The default local configuration is equivalent to:

```bash
DATABASE_HOST=localhost \
DATABASE_PORT=5432 \
DATABASE_NAME=snowx_iam \
DATABASE_USER=snowx \
DATABASE_PASSWORD=snowx \
SERVER_PORT=9527 \
java -jar target/snowx-iam-1.0.0-SNAPSHOT.jar
```

The service base URL is `http://localhost:9527/sign`. The initial administrator
account imported by the seed migration is `admin` / `maxkey`; change this
password immediately outside a disposable local environment.

## Frontend integration

Use the unified backend for both portal and management requests:

```dotenv
VITE_API_BASE_URL=/sign/
VITE_ADMIN_API_BASE_URL=/sign/admin/
```

During Vite development, proxy `/sign` to `http://127.0.0.1:9527`. A separate
`maxkey-web-mgt` backend is no longer required by the migrated routes.

## Configuration

Common environment variables are:

- `DATABASE_HOST`, `DATABASE_PORT`, `DATABASE_NAME`
- `DATABASE_USER`, `DATABASE_PASSWORD`
- `SERVER_PORT` (default `9527`)
- `SPRING_PROFILES_ACTIVE` (default `local`)
- `LOGIN_CAPTCHA` (set `false` for API-only local verification)
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` when Redis-backed features are enabled

Spring standard overrides such as `SPRING_DATASOURCE_URL` are also supported.

## Project documentation

- [Architecture and directory layout](docs/architecture.md)
- [Migration scope and verification](docs/migration-scope.md)

The migrated MaxKey source retains its original copyright notices and Apache
License 2.0 attribution.
