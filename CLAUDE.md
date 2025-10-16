# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**VigilApp** is a Spring Boot 3.5.6 REST API for a community alert and vigilance system. Users can create, validate, and receive notifications about safety alerts in their geographic area. The application uses PostgreSQL with PostGIS for geospatial data, JWT authentication, and Liquibase for database migrations.

**Tech Stack:**
- Java 21
- Spring Boot 3.5.6 (Web, Security, Data JPA, Validation)
- PostgreSQL with PostGIS extension
- Hibernate Spatial with JTS for geographic data
- Liquibase for database migrations
- JWT (jjwt 0.9.1) for authentication
- Lombok for boilerplate reduction
- ModelMapper for DTO mapping
- Gradle for build management

## Common Commands

### Build & Run
```bash
# Build the project
./gradlew build

# Run the application (inicia automáticamente el servicio Python de verificación facial)
./gradlew bootRun

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests com.fram.vigilapp.SomeTest

# Clean build artifacts
./gradlew clean
```

**Nota**: Al ejecutar `bootRun`, Spring Boot inicia automáticamente el servicio Python de verificación facial en puerto 8000. No es necesario iniciarlo manualmente.

### Database
- PostgreSQL runs on `localhost:5433`
- Database name: `vigilapp`
- Credentials: `postgres/root` (configured in `src/main/resources/application.yml`)
- Liquibase manages all schema changes via `src/main/resources/db.changelog.xml`
- Liquibase is enabled with `drop-first: false` - schema changes are applied incrementally

## Architecture & Code Structure

### Package Organization
```
com.fram.vigilapp/
├── config/                          # Spring configuration classes
│   ├── auth/                       # JWT filter and authentication request
│   ├── exception/                  # Global exception handler
│   ├── FaceVerificationServiceManager  # Gestiona el servicio Python (inicio/detención)
│   └── SecurityConfig              # Spring Security configuration
├── controller/                     # REST controllers (e.g., AuthController)
├── dto/                           # Data Transfer Objects
│   ├── UserDto, SaveUserDto       # Usuario
│   ├── FaceVerificationResponse   # Respuesta de comparación facial
│   └── IdValidationResponse       # Respuesta de validación de cédula
├── entity/                        # JPA entities
│   └── id/                       # Composite ID classes (e.g., AlertMediaId)
├── repository/                    # Spring Data JPA repositories
├── service/                       # Service interfaces
│   ├── FaceVerificationService    # Servicio de verificación facial
│   └── impl/                     # Service implementations
└── util/                         # Utility classes (e.g., JwtUtil, DateUtil)
```

### Servicio Python (face-verification-service/)
```
face-verification-service/
├── main.py                    # FastAPI application
├── services/
│   ├── id_validator.py       # Validación de cédula (OpenCV)
│   └── face_comparator.py    # Comparación facial (face_recognition)
├── requirements.txt
└── venv/                     # Entorno virtual Python
```

### Domain Model

The application centers around a **community alert system** with these core entities:

**User Management:**
- `User`: Core user entity with roles (USER, MOD, ADMIN) and statuses (ACTIVE, BLOCKED, PENDING)
- `IdentityVerification`: Biometric identity verification with liveness/match scores
- `UserDevice`: Push notification device tokens
- `UserZone`: Geographic zones of interest (PostgreSQL GEOGRAPHY type for polygons)

**Alert System:**
- `Alert`: Geospatial alerts with categories (EMERGENCY, PRECAUTION, INFO, COMMUNITY), verification status, and PostGIS Point geometry
- `AlertValidation`: Community voting on alert validity
- `Media`: Uploaded files (images/videos) for alerts
- `AlertMedia`: Many-to-many relationship with composite ID (AlertMediaId)
- `ModerationCase`: Moderation workflow for flagged alerts

**Notification System:**
- `Notification`: Tracks sent notifications with delivery status
- `City`: Geographic reference data for addresses

### Key Architectural Patterns

**Face Verification (Python Microservice):**
- El componente `FaceVerificationServiceManager` gestiona el ciclo de vida del servicio Python
- Se inicia automáticamente al arrancar Spring Boot (puerto 8000)
- Se detiene automáticamente al cerrar Spring Boot
- Configuración en `application.yml`: `face.verification.service.*`
- Logs del servicio Python se muestran en la consola de Spring Boot
- Endpoints del servicio Python:
  - `POST /validate-id`: Valida que una imagen sea una cédula (OpenCV, contornos, aspect ratio)
  - `POST /verify-face`: Compara rostros entre cédula y selfie (face_recognition embeddings)
- El servicio `FaceVerificationService` (Spring Boot) actúa como cliente HTTP hacia el servicio Python
- El endpoint `/api/register` ahora acepta `multipart/form-data` con imágenes: `fotoCedula` y `selfie`
- Flujo de registro: validar campos → validar cédula → comparar rostros → si match, crear usuario
- Si rostros no coinciden, retorna HTTP 400 con detalles de similitud

**Geospatial Data:**
- Uses Hibernate Spatial with `@JdbcTypeCode(SqlTypes.GEOMETRY)` for PostGIS integration
- Geographic columns use `GEOGRAPHY(POINT,4326)` and `GEOGRAPHY(POLYGON,4326)` types
- JTS library (`org.locationtech.jts.geom.Point`) for geometry objects
- PostGIS extension must be enabled in the database

**Authentication & Security:**
- JWT-based stateless authentication via `JwtRequestFilter` (extends `OncePerRequestFilter`)
- `JwtUtil` handles token generation, validation, and claims extraction
- `CustomUserDetailsService` loads users by email for Spring Security
- Public endpoints: `/api/register`, `/api/login`
- All other endpoints require authentication
- CORS configured for `http://localhost:4200` (Angular frontend)

**Database Management:**
- Liquibase changesets in `db.changelog.xml` define all schema
- Extensions enabled: `postgis`, `citext` (case-insensitive text)
- UUID primary keys with `gen_random_uuid()` defaults
- Hibernate DDL auto is disabled (`ddl-auto: none`) - Liquibase manages schema

**Entity Conventions:**
- All entities use Lombok annotations: `@Getter`, `@Setter`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor`
- Timestamps use `@CreationTimestamp` and `@UpdateTimestamp` with `OffsetDateTime`
- String enums stored as TEXT columns (e.g., `role`, `status`, `category`)
- Lazy loading for relationships (`FetchType.LAZY`)

**Service Layer:**
- Service interfaces in `service/` package with implementations in `service/impl/`
- Currently implemented: `AuthService` / `AuthServiceImpl` for registration and login
- Uses `ModelMapper` for entity-DTO conversions

### Configuration Notes

**application.yml:**
- JPA/Hibernate configured with `ddl-auto: none` (Liquibase handles schema)
- Error messages always included in responses (`include-message: always`)
- Default schema: `public`

**SecurityConfig:**
- Disables CSRF (stateless JWT authentication)
- Session management: `SessionCreationPolicy.STATELESS`
- `BCryptPasswordEncoder` for password hashing
- `JwtRequestFilter` runs before `UsernamePasswordAuthenticationFilter`

## Development Guidelines

When adding new features:

1. **New entities:** Add Liquibase changesets to `db.changelog.xml` before creating JPA entities
2. **Repositories:** Extend `JpaRepository` in the `repository/` package
3. **Services:** Create interface in `service/`, implementation in `service/impl/`
4. **Controllers:** Add to `controller/` package with appropriate `@PreAuthorize` annotations for role-based access
5. **DTOs:** Place in `dto/` package and use `ModelMapper` for conversions
6. **Geospatial queries:** Use PostGIS functions via native queries or JPA Criteria API with Hibernate Spatial predicates

For composite IDs (like `AlertMediaId`), use `@Embeddable` and `@EmbeddedId` following the existing pattern.
