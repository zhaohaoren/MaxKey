# Migration scope and verification

## Included capabilities

The migration retains backend behavior currently exercised by
`maxkey-web-vue-app`, including:

- Normal login, JWT refresh, sessions, profile, password, MFA and passkeys
- Portal application listing, account credentials and application launch
- Users, accounts, organizations, groups, roles and member assignment
- Applications and CAS, form, JWT, token, OAuth/OIDC and SAML details
- Access assignment, permissions and protected resources
- Adapters, connectors, social providers and synchronizers
- Login/application/system/synchronization history and dashboard reports
- CAS, OAuth/OIDC, SAML and related SSO protocol endpoints used by managed apps

The supporting repository-owned source for these paths is copied into the
`com.snowx.iam` package tree and built inside this project.

## Deliberately not retained

- Independently deployable `maxkey-web-maxkey` and `maxkey-web-mgt` services
- Legacy frontend bundles and server-rendered management UI packaging
- MySQL-specific schema scripts and SQL syntax
- JPA and the repository-specific `mybatis-jpa` persistence abstraction
- Maven dependencies on repository-owned `org.dromara.maxkey` artifacts
- Code with no route or runtime dependency required by `maxkey-web-vue-app`

This is a migration boundary, not a promise that every historical MaxKey
extension is supported. Reintroducing an excluded capability requires an
explicit use case and a PostgreSQL/MyBatis-Plus implementation.

## Database migrations

- `V1__snowx_iam_schema.sql`: PostgreSQL schema for the retained model
- `V2__snowx_iam_initial_data.sql`: baseline institution, admin and IAM data
- `V3__add_passkey_session_id.sql`: passkey challenge session compatibility

## Completed verification

The migrated service has been started against a clean PostgreSQL database and
verified through real HTTP requests for:

- Administrator login and `ROLE_ADMINISTRATORS` authority
- Portal application list and profile lookup
- Dashboard totals, province and country analysis
- User, account, organization and resource CRUD
- Group member, role member and application access assignment
- Adapter, connector, social provider and synchronizer CRUD
- CAS, form-based, JWT and token-based application detail CRUD
- Permission reads and application authorization queries

Mapper XML files are XML-valid and explicit mapper methods are backed by XML
statements or annotation SQL. A clean Maven package is the release gate:

```bash
mvn -DskipTests clean package
mvn dependency:tree
```

## Known follow-up work

- Replace the locally packaged legacy OpenSAML 2 runtime with a maintained SAML
  implementation after protocol compatibility tests are available.
- Add automated PostgreSQL integration tests for the HTTP scenarios currently
  covered by migration smoke tests.
- Rotate all seed credentials and protocol keys before production deployment.
