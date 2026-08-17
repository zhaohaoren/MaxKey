# Architecture and directory layout

SnowX IAM is a deployment monolith: one Maven module, one Spring application,
one PostgreSQL schema, and one executable JAR. Package boundaries remain
explicit so capabilities can evolve independently without introducing a
multi-module build or distributed-service overhead.

## Repository layout

```text
snowx-iam/
|-- pom.xml                         Maven build and dependency boundary
|-- libs/                           Legacy SAML runtime JARs kept locally
|-- docs/                           Architecture and migration records
`-- src/main/
    |-- java/com/snowx/iam/
    |   |-- SnowxIamApplication.java
    |   |-- web/                    Portal/admin REST endpoints and web config
    |   |-- authn/                  Login, token, session, MFA and SSO authn
    |   |-- authz/                  CAS, OAuth/OIDC, SAML and app authorization
    |   |-- passkey/                WebAuthn/passkey capability
    |   |-- password/               Password policy, OTP and SMS/mail support
    |   |-- persistence/            MyBatis-Plus mappers and application services
    |   |-- entity/                 Persistence and API data models
    |   |-- adapter/                Application protocol adapters
    |   |-- synchronizer/           Directory and SaaS synchronization
    |   |-- configuration/          Product configuration models
    |   |-- autoconfigure/          Spring runtime configuration
    |   `-- constants|crypto|http|id|ip2location|json|ldap|schedule|util/
    |                               Shared platform utilities
    `-- resources/
        |-- application.yml         Runtime defaults and environment overrides
        |-- application-local.yml   Local-development overrides
        |-- db/migration/            Versioned PostgreSQL Flyway migrations
        |-- mapper/postgresql/       PostgreSQL MyBatis statements
        |-- config/                  SSO keys and protocol configuration
        `-- messages|templates/      Localized messages and protocol templates
```

## Request flow

```text
maxkey-web-vue-app
        |
        | /sign/* and /sign/admin/*
        v
web controller -> authn/authz service -> persistence service -> mapper
                                                        |
                                                        v
                                                   PostgreSQL
```

Authentication and authorization concerns are shared inside the same process.
Portal endpoints and management endpoints use one token/session model and one
transaction boundary. `/sign` is the server context path; management
controllers are mounted below `/sign/admin`.

## Persistence rules

- MyBatis-Plus supplies generic CRUD through `BaseMapper`.
- Complex or compatibility-sensitive statements live in
  `resources/mapper/postgresql`.
- Entity mappings use MyBatis-Plus annotations; JPA and `mybatis-jpa` are not
  part of the runtime.
- Flyway is the only schema bootstrap path. Migrations are immutable after they
  are applied to a shared environment.
- PostgreSQL syntax is required for all new SQL.

## Dependency boundary

All repository-owned Java source needed at runtime is compiled in this module.
The Maven dependency graph must not contain `org.dromara.maxkey` artifacts.
Four legacy SAML 2 runtime JARs remain in `libs/` because their historical
versions are not reliably available from Maven Central; they are packaged into
the executable JAR through the Spring Boot plugin.

## Evolution rules

- Add a feature inside its capability package and expose it through `web`.
- Keep controller code thin; transaction and business rules belong in services.
- Do not reintroduce a second deployable management application.
- Do not add direct dependencies on modules in the legacy repository.
- Add every schema change as the next Flyway migration.
