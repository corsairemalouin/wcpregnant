# WC Pregnant Service - Architecture & Design Documentation

## Overview

WC Pregnant Service is a backend API providing location services for toilets accessible to pregnant women.

**Users:**
- **Admin**: Manage toilet locations via web UI
- **Toilet Owner**: Submit requests via support (processed by admin)
- **Pregnant Women**: Mobile app user querying nearby toilet locations

## Architecture Pattern: Layered / Clean Architecture

```
┌─────────────────────────────────────┐
│         REST API Layer (Web)        │  ← HTTP Controllers (ToilettesApiImpl, CustomerApiImpl)
├─────────────────────────────────────┤
│       Business Logic Layer          │  ← Services (ToiletteService, CustomerService)
├─────────────────────────────────────┤
│        Data Access Layer (DAO)      │  ← Repositories (ToiletteRepository, CustomerRepository)
├─────────────────────────────────────┤
│      Database (PostgreSQL)          │  ← Entity/JPA (ToiletDAO, etc.)
└─────────────────────────────────────┘
```

Additionally:
- **Mappers**: MapStruct for DTO ↔ DAO conversion
- **Utils**: Geolocation calculations
- **Config**: Application configuration (ToiletteServiceConfig)
- **Security**: Spring Security + JWT (if implemented)

## Key Components

### 1. Domain Models

**Toilet** (DTO for API)
- Properties: id, libelle (name), adress, grade, position, distance
- Used in REST responses
- Distance calculated by GeoPositionUtils

**ToiletDAO** (JPA Entity)
- Database entity with cascading relationships
- OneToOne with AdressDAO, GraduationDAO, GeoPositionDAO

### 2. Services

**ToiletteService** (interface) / **ToiletteServiceImpl** (implementation)
- `getAllToilets(page, size)`: Paginated list
- `getNearestToilettes(currentPosition)`: Filter by distance, sort, limit to config max
- `lireToilet(id)`: Get single by ID
- `addToilet(toilet)`: Create
- `updateToilet(id, toilet)`: Update
- `deleteToilet(id)`: Delete

**CustomerService** (stub for future implementation)
- Can extend for user management

### 3. Controllers (REST API)

**ToilettesApiImpl**
- Implements `ToiletApi` interface (OpenAPI generated)
- Endpoints:
  - `GET /toilets` — list all or search by coordinates (lat/lon)
  - `POST /toilets` — create (ADMIN only)
  - `GET /toilets/{id}` — get single
  - `PUT /toilets/{id}` — update (ADMIN only)
  - `DELETE /toilets/{id}` — delete (ADMIN only)

**Security**:
- `@PreAuthorize("hasRole('USER')")` for read
- `@PreAuthorize("hasRole('ADMIN')")` for write/delete

### 4. Data Access

**Repositories** (Spring Data JPA)
- `ToiletteRepository` extends `JpaRepository<ToiletDAO, Long>`
- Provides basic CRUD; can extend with custom queries

**Entities** (JPA)
- Cascade delete: ToiletDAO → AdressDAO, GraduationDAO, GeoPositionDAO
- All have IDs and standard JPA annotations

### 5. Mappers (MapStruct)

**ToiletteDAOMapper**
- `daoToBo()`: ToiletDAO → Toilet (DTO)
- `boToDao()`: Toilet → ToiletDAO
- `updateToiletteDAOFromBO()`: Partial update

**AdressDAOMapper**
- Uppercases `voie` and `ville` fields on mapping

**GraduationDAOMapper**
- Simple field mapping (accueil, espace, proprete)

### 6. Geolocation Utils

**GeoPositionUtils**
- `distance(lat1, lon1, lat2, lon2, el1, el2)`: Haversine formula
- `distance(GeoPosition p1, GeoPosition p2)`: Convenience overload
- `setDistance(toilet, position)`: Calculate and set toilet distance
- `compareWithDistance(t1, t2)`: Comparator for sorting

**Fixed Issues** (recent improvements):
- Corrected parameter order in primitive distance method
- Added null-safety checks (returns Double.MAX_VALUE for missing data)

### 7. Configuration

**ToiletteServiceConfig**
- `app.distance.max` (default: 100 km): max distance filter
- `app.result.max` (default: 5): max results returned

### 8. API Documentation (OpenAPI/Swagger)

**Files**: `src/main/resources/api/*.yaml`
- `ToiletAPI.yaml`: Toilet CRUD operations
- `CustomerAPI.yaml`: Customer operations

**Generated Code** (by maven plugin):
- Interfaces in `target/generated-sources/openapi/src/main/java/`
- DTOs in `org.manu.wcpregnant.web.dto`
- Controllers implement these interfaces

## Testing Strategy

### Unit Tests

**Coverage Areas**:
- `GeoPositionUtils` — distance calculations, null handling
- `ToiletteServiceImpl` — filtering, sorting, limiting
- Mappers — field transformations
- Services — CRUD operations

**Tools**:
- JUnit 5
- Mockito (mocking dependencies)
- Spring Boot Test for integration tests
- JaCoCo for coverage reporting

### Integration Tests

- `ToilettesApiImplTest`: HTTP endpoints via MockMvc
- Uses `@SpringBootTest` + `@AutoConfigureMockMvc`
- Tests security annotations

### Coverage Goals

- Target: >80% code coverage
- Priority: business logic (services, utils), security (controller endpoints)

## CI/CD Pipeline

**.github/workflows/docker-image.yml**

1. Checkout code
2. Setup JDK 25
3. Build with Maven (`mvn -B -DskipTests package`)
4. Build & push Docker image to GHCR

**Trigger**: Pushes to `main` or `master`

## Deployment Architecture

### Docker

**Dockerfile** (multi-stage ready, single-stage for simplicity)
- Base: `eclipse-temurin:25-jre`
- Copies `target/*.jar` → `/app/app.jar`
- Exposes port 8080
- Configurable JVM options via `JAVA_OPTS`

### Kubernetes

**Namespace**: `wcpregnant` (isolated)

**App Deployment**:
- 2 replicas (min), 5 max (via HPA)
- Readiness probe: `GET /api/actuator/health/readiness` (20s initial, 5s interval)
- Liveness probe: `GET /api/actuator/health` (30s initial, 10s interval)
- Resource requests: 256Mi memory, 250m CPU
- Resource limits: 512Mi memory, 500m CPU

**Database Deployment**:
- PostgreSQL 15 Alpine
- 1 replica (single point of failure in basic setup)
- Persistent volume (10Gi)
- Health check: `pg_isready -U wcpregnant`

**Service**:
- Type: ClusterIP (port 80 → 8080)

**Ingress** (nginx, optional):
- Host: `wcpregnant.example.com` (configurable)
- TLS via cert-manager (Let's Encrypt)

**Networking**:
- NetworkPolicy restricts pod-to-pod communication
- App → PostgreSQL on port 5432
- App accepts from frontend/ingress

**HA**:
- PodDisruptionBudget: min 1 pod available
- Autoscaling: scale up at 70% CPU threshold

**Secrets/Config**:
- Secret: database credentials, URLs
- ConfigMap: application properties

## Code Optimization Areas

### Recent Improvements
1. Fixed geolocation distance bug (parameter order)
2. Added null-safety to utility functions
3. Expanded unit test coverage
4. Integrated JaCoCo for coverage tracking

### Future Optimization Opportunities
1. **Database Indexing**: Add indexes on `adress.codePostal`, `position.latitude/longitude` for faster queries
2. **Caching**: Redis cache for frequently accessed toilet locations
3. **Pagination**: Use repository pagination for large result sets
4. **Query Optimization**: Custom JPQL queries instead of stream filtering
5. **Validation**: More robust input validation (JSR-380 annotations)
6. **Logging**: Structured logging for better observability
7. **Monitoring**: Prometheus metrics export
8. **Security**: OAuth2/JWT token validation

## File Structure

```
src/
├── main/
│   ├── java/org/manu/wcpregnant/
│   │   ├── WcpregnantApplication.java (Spring Boot entry point)
│   │   ├── config/
│   │   │   ├── ToiletteServiceConfig.java
│   │   │   └── AppSecurityConfiguration.java
│   │   ├── service/
│   │   │   ├── ToiletteService.java (interface)
│   │   │   ├── CustomerService.java
│   │   │   ├── impl/
│   │   │   │   ├── ToiletteServiceImpl.java
│   │   │   │   └── CustomerServiceImpl.java
│   │   │   ├── utils/
│   │   │   │   ├── GeoPositionUtils.java
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   └── bo/
│   │   │       ├── Person.java
│   │   │       └── UserAccount.java
│   │   ├── web/
│   │   │   ├── ToilettesApiImpl.java (REST controller)
│   │   │   ├── CustomerApiImpl.java
│   │   │   └── dto/ (generated by OpenAPI)
│   │   │       ├── Toilet.java
│   │   │       ├── GeoPosition.java
│   │   │       ├── Adress.java
│   │   │       └── Graduation.java
│   │   └── bdd/
│   │       ├── dao/
│   │       │   ├── ToiletDAO.java (JPA Entity)
│   │       │   ├── AdressDAO.java
│   │       │   ├── GeoPositionDAO.java
│   │       │   ├── GraduationDAO.java
│   │       │   └── ...
│   │       ├── mapper/
│   │       │   ├── ToiletteDAOMapper.java (MapStruct)
│   │       │   ├── AdressDAOMapper.java
│   │       │   └── GraduationDAOMapper.java
│   │       └── repository/
│   │           ├── ToiletteRepository.java
│   │           ├── CustomerRepository.java
│   │           └── UserRepository.java
│   └── resources/
│       ├── application.properties
│       └── api/
│           ├── ToiletAPI.yaml
│           └── CustomerAPI.yaml
├── test/
│   └── java/org/manu/wcpregnant/
│       ├── service/
│       │   ├── ToiletteServiceImplUnitTest.java
│       │   ├── ToiletteServiceImplTest.java (integration)
│       │   ├── ToiletteServiceImplTest.java
│       │   └── CustomerServiceImplTest.java
│       ├── bdd/
│       │   ├── mapper/
│       │   │   ├── AdressDAOMapperTest.java
│       │   │   ├── GraduationDAOMapperTest.java
│       │   │   └── ToiletteDAOMapperTest.java
│       │   └── repository/
│       │       └── ToiletteRepositoryTest.java
│       ├── web/
│       │   ├── ToilettesApiImplTest.java (integration: MockMvc)
│       │   └── SecurityIntegrationTest.java
│       ├── utils/
│       │   └── GeoPositionUtilsTest.java
│       └── utils/
│           └── TestsUtils.java (test data builders)
├── docker/
│   └── Dockerfile
├── k8s/
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── secret.yaml
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── postgres-deployment.yaml
│   ├── postgres-pvc.yaml
│   ├── hpa.yaml
│   ├── ingress.yaml
│   ├── networkpolicy.yaml
│   ├── pdb.yaml
│   ├── deploy.ps1 (Windows deployment script)
│   ├── deploy.sh (Linux/Mac deployment script)
│   └── README.md
├── .github/
│   └── workflows/
│       └── docker-image.yml (CI/CD)
├── pom.xml (Maven dependencies & plugins)
├── docker-compose.yml
├── Dockerfile
├── README.md
├── README-DEPLOY.md
├── SETUP-GUIDE.md
├── ARCHITECTURE.md (this file)
└── wcpregnant.iml (IDE project file)
```

## Performance Considerations

### Current Bottlenecks
1. **Stream filtering**: `getNearestToilettes()` loads all toilets, maps, filters — OK for small datasets, problematic at scale
2. **No indexing**: Database queries without proper indexes
3. **No caching**: Repeated distance calculations

### Recommended Optimizations
1. **Spatial Queries**: PostgreSQL PostGIS extension for geographic queries (instead of client-side filtering)
2. **Lazy Loading**: DTOs only fetch needed fields
3. **Pagination**: Use repository pagination for large result sets
4. **Caching**: Redis for toilet location cache (invalidate on updates)
5. **Connection Pooling**: HikariCP (default in Spring Boot)
6. **Query Limits**: Set database query timeouts

## Security Considerations

### Current Implementation
- Spring Security + JWT (if configured in AppSecurityConfiguration)
- Role-based authorization: `@PreAuthorize` annotations
- CSRF protection (Spring Security default)

### Recommendations
- [ ] Validate all inputs (JSR-380 Bean Validation)
- [ ] HTTPS only (enforced in reverse proxy/K8s Ingress)
- [ ] SQL injection prevention (JPA/Hibernate handles this; avoid raw SQL)
- [ ] XSS prevention (API-only backend, no templates)
- [ ] Rate limiting (nginx-ingress or Spring Cloud)
- [ ] Audit logging (all mutations logged with user/timestamp)
- [ ] Secret rotation (K8s Secret management)

## Monitoring & Observability

### Logs
- Structured logging recommended (JSON lines format)
- Spring Boot logging levels configured in `application.properties`

### Metrics
- Spring Boot Actuator endpoints: `/actuator`, `/actuator/health`, `/actuator/metrics`
- Can export to Prometheus + Grafana

### Tracing
- (Not yet implemented) Spring Cloud Sleuth for distributed tracing

## Deployment Checklist

- [ ] Tests pass locally: `mvn test`
- [ ] JAR builds: `mvn clean package`
- [ ] Docker image builds: `docker build -t wcpregnant:latest .`
- [ ] docker-compose runs: `docker-compose up`
- [ ] K8s manifests validated: `kubectl apply --dry-run=client -f k8s/`
- [ ] Secrets configured securely (no credentials in manifests)
- [ ] Database migrations handled (Flyway or Liquibase recommended)
- [ ] Health checks accessible
- [ ] Logs accessible and structured
- [ ] Monitoring/alerting configured

## Next Steps

1. Run full test suite and review JaCoCo coverage report
2. Set up local K8s cluster (Docker Desktop or Minikube)
3. Deploy locally with docker-compose and K8s scripts
4. Configure CI/CD: GitHub Actions workflow (already present, needs tweaking)
5. Set up staging environment for pre-production validation
6. Performance testing under load (JMeter, Gatling)
7. Security audit and penetration testing

