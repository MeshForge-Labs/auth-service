# Auth Service

Production-ready Spring Boot 3 authentication microservice for Azure AKS: register, login, JWT validation, BCrypt password hashing, and role-based access (USER, ADMIN).

## Stack

- **Java 17**, **Maven**, **JAR**
- Spring Web, Spring Data JPA, Spring Security
- PostgreSQL, Lombok, Springdoc OpenAPI (Swagger)
- JWT (JJWT), Actuator (health, liveness, readiness)

## Project structure

```
auth-service/
├── src/main/java/com/booking/auth/
│   ├── AuthServiceApplication.java
│   ├── config/           # OpenAPI, DB readiness health
│   ├── controller/       # REST API
│   ├── dto/              # Request/response and error DTOs
│   ├── exception/        # Custom exceptions + global handler
│   ├── model/            # JPA entities, Role (USER, ADMIN)
│   ├── repository/       # JPA repositories
│   ├── security/         # Spring Security config, JWT filter
│   └── service/          # Auth + JWT services
├── src/main/resources/
│   ├── application.yml   # Config (env-based)
│   └── application-dev.yml
├── k8s/
│   ├── deployment.yaml
│   ├── service.yaml
│   └── secret.example.yaml
├── Dockerfile
├── .dockerignore
└── pom.xml
```

## API endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | No | Register user (BCrypt), returns JWT |
| POST | `/api/auth/login` | No | Login, returns JWT |
| GET | `/api/auth/validate` | No | Validate Bearer token, returns subject + roles |
| GET | `/api/auth/me` | JWT | Current user (subject + roles) |
| GET | `/api/auth/admin` | JWT + ADMIN | Admin-only (role-based) |
| GET | `/actuator/health` | No | Health (liveness/readiness) |
| GET | `/swagger-ui.html` | No | Springdoc Swagger UI |
| GET | `/api-docs` | No | OpenAPI JSON |

## Configuration (environment variables)

Required in production:

- `JWT_SECRET` – at least 32 characters (HS256)
- `DATABASE_URL` – JDBC URL (e.g. `jdbc:postgresql://host:5432/auth_db`)
- `DATABASE_USERNAME`, `DATABASE_PASSWORD`

Optional:

- `SERVER_PORT` (default `8080`)
- `JWT_EXPIRATION_MS` (default `900000`)
- `JWT_ISSUER` (default `auth-service`)
- `JPA_DDL_AUTO` (default `validate`; use `update` only for dev)
- `LOG_LEVEL`, `DB_POOL_SIZE`, `DB_SCHEMA`

No credentials are hardcoded; use Kubernetes Secrets or your orchestrator’s secret management.

## Build and run

```bash
# Build
mvn clean package

# Run (set JWT_SECRET and DB vars, or use dev profile)
export JWT_SECRET=your-secret-at-least-32-characters-long
mvn spring-boot:run

# Or with dev profile (dev-only JWT default, ddl-auto=update)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Docker

```bash
docker build -t auth-service:1.0.0 .
docker run -p 8080:8080 \
  -e JWT_SECRET=your-secret-at-least-32-characters \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/auth_db \
  -e DATABASE_USERNAME=postgres \
  -e DATABASE_PASSWORD=postgres \
  auth-service:1.0.0
```

## Kubernetes

1. Create secret from example (replace placeholders):

   ```bash
   kubectl apply -f k8s/secret.example.yaml
   ```

   Or create secret manually and set the same keys as in `envFrom.secretRef` in `deployment.yaml`.

2. Deploy:

   ```bash
   kubectl apply -f k8s/deployment.yaml
   kubectl apply -f k8s/service.yaml
   ```

3. Liveness: `/actuator/health/liveness`  
   Readiness: `/actuator/health/readiness` (includes DB check)

## JWT and roles

- **Roles**: `USER` (`ROLE_USER`), `ADMIN` (`ROLE_ADMIN`) – see `model/Role.java`. New users get USER by default.
- Tokens include subject (email) and `roles` claim. Use `Authorization: Bearer <token>` for `/me` and `/admin`.
- Role-based access: `/api/auth/admin` is protected with `@PreAuthorize("hasRole('ADMIN')")`.

## Validation and errors

- DTOs use Jakarta Validation (`@NotBlank`, `@Email`, `@Size`, etc.).
- Global exception handler returns structured error payloads (e.g. 400 validation, 401 bad credentials, 409 conflict).
