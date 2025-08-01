# CCD Data Store API - AI Agent Guidelines

## Project Overview
The Core Case Data (CCD) Data Store API is a Spring Boot application that provides storage, search, and workbasket functionality for case data in the HMCTS platform. It serves as the backend for case management in various judicial services.

## Architecture & Components

### Key Components
- **Domain Layer**: Contains the core business logic in `domain/service`
  - Case data access and manipulation
  - Case search functionality
  - Authorization and security checks
  - Event handling (create/start events)
  - Case linking and relationships

- **Data Layer**: Handles database interactions in `data/`
  - Case details repository (`casedetails/`)
  - User authorization (`user/`)
  - Drafts storage (`draft/`)
  - Case linking and reference data (`caselinking/`)

- **API Endpoints**: Organized by consumer type
  - UI endpoints (`endpoint/ui/`)
  - Standard API (`endpoint/std/`)
  - Exception handlers (`endpoint/exceptions/`)

### External Dependencies
- **Identity & Access Management (IDAM)**: For user authentication
- **Service Authorization (S2S)**: For service-to-service authentication
- **Definition Store**: Provides case type definitions and UI configuration
- **User Profile Service**: For user data and preferences
- **Document API**: For case document management

## Build & Run Workflow

### Local Development
```bash
# Build the project
./gradlew clean build

# Start application with dependencies using Docker
docker-compose up

# Run tests
./gradlew test                  # Unit tests
./gradlew functional            # All functional tests
./gradlew functional -P tags="@F-1023"  # Specific functional test
```

### Environment Configuration
Key environment variables required in `src/main/resources/application.yaml`:
- `DATA_STORE_DB_*` for database connection
- `IDAM_*` for IDAM integration 
- `DEFINITION_STORE_HOST` for Definition Store API endpoint
- `DATA_STORE_S2S_AUTHORISED_SERVICES` for S2S integration

## Testing Patterns

### Test Structure
- Unit tests in `src/test/` mirror the main package structure
- Functional tests in `src/aat/` using BEFTA Framework (BDD)
- Use TestContainers for integration tests with database

### Test Helpers & Fixtures
- Common test fixtures in `src/test/resources/`
- BEFTA test data in `.xlsx` format

## Security Model

### Authentication & Authorization
- Two-level security: IDAM user authentication + S2S service authentication
- User authorization based on IDAM roles (`caseworker-{jurisdiction}`)
- Role-based access control for case data

### Request Flow
1. Authenticate user with IDAM (JWT token in `Authorization` header)
2. Verify service token (in `ServiceAuthorization` header)
3. Verify user has appropriate role for jurisdiction
4. Check user has access to the specific case (if applicable)

## Common Patterns & Conventions

### DTO Pattern
- Case data stored in JSON format with field-type specific serialization
- Case field types defined in Definition Store
- Case events and field validation rules from Definition Store

### Service Layer Decorators
- Services frequently use decorator pattern for cross-cutting concerns
- Example: `AuthorisedGetCaseOperation` wraps basic operations with access control

## Integration Points
- Definition Store API for case type schema
- User Profile API for user data
- IDAM for authentication
- Document API for document management
- Callback endpoints for workflow integration with other services

## Common Issues & Solutions
- JWT token issues: Check IDAM configuration and roles
- Case data validation: Case fields must match Definition Store schema
- Performance: Use pagination for large result sets in search operations
- Security: Always apply proper authorization checks at service level

## Application Insights & Cost Optimization

### Telemetry Configuration
- App Insights configuration is in `/lib/applicationinsights.json`
- Custom telemetry processing in `AppInsightsCostOptimizationProcessor`
- Main telemetry client in `AppInsightsConfiguration`

### Sampling & Filtering Strategy
- Health endpoints are sampled at minimum rate (1%)
- High-volume endpoints like document operations are sampled at lower rate (15%)
- Dependency calls are sampled at 25%
- General requests are sampled at 50%
- All expected exceptions are filtered (ResourceNotFoundException, AccessDeniedException)
- OPTIONS requests are filtered out completely
- Database health checks are filtered out

### Best Practices for New Features
- Avoid excessive logging that might generate large volumes of telemetry
- For high-volume operations, consider adding custom filtering in `AppInsightsCostOptimizationProcessor`
- Use appropriate severity levels for exceptions
- Consider using custom events sparingly and only for important business metrics
