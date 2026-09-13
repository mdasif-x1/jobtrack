# JobTrack

JobTrack is a full-stack-ready job application management system built with Java and Spring Boot. It helps users track companies, job applications, application status, follow-up dates, search results, filters, and dashboard statistics through a REST API.

## Problem

Job applications are often tracked across spreadsheets, browser bookmarks, notes, and emails. That makes follow-ups, status tracking, duplicate prevention, and company management difficult.

JobTrack provides one API-driven system to manage this workflow in a structured way.

## Features

- Company CRUD
- Job application CRUD
- Application status update through PATCH
- Company-to-application relationship
- Unique job-link protection
- Request validation and date validation
- Consistent JSON error responses
- Pagination and safe sorting
- Search by job role or company name
- Filter by status, job type, and application date range
- Upcoming action list
- Status statistics for dashboard cards
- Swagger/OpenAPI documentation
- Postman collection
- Unit tests using JUnit 5 and Mockito

## Exclusions

The current project scope intentionally does not include:

- Authentication or authorization
- JWT or Spring Security
- User accounts
- File uploads
- Notifications
- Email integration
- React frontend
- Microservices

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Build tool | Maven Wrapper |
| Database | MySQL |
| Persistence | Spring Data JPA / Hibernate |
| Validation | Jakarta Bean Validation |
| API documentation | springdoc-openapi 3.1.1 |
| Testing | JUnit 5, Mockito, AssertJ |
| API testing | Postman |

## Architecture

```text
Controller → Service → Repository → MySQL
                ↓
             Mapper
                ↓
          Request / Response DTOs
```

- **Controllers** handle HTTP requests and responses.
- **Services** contain business rules and transactions.
- **Repositories** communicate with the database.
- **Mappers** convert entities to response DTOs and request DTOs to entities.
- **Entities** represent database tables and are not returned directly from the API.

## Database Relationship

```text
Company (1) ──────── (*) JobApplication
```

A company can have many job applications.  
Each job application belongs to exactly one company through `company_id`.

## Package Structure

```text
com.asif.jobtrack
├── config
├── controller
├── dto
│   ├── request
│   └── response
├── entity
├── enums
├── exception
├── mapper
├── repository
│   └── projection
├── service
│   └── impl
└── JobTrackApplication
```

## Prerequisites

Install the following before running locally:

- Java 21
- MySQL 8+
- IntelliJ IDEA or another Java IDE
- Git
- Postman (optional, for API testing)

Verify Java:

```powershell
java -version
```

Verify Maven Wrapper:

```powershell
.\mvnw.cmd -version
```

## Local Setup

### 1. Clone the repository

```bash
git clone https://github.com/mdasif-x1/jobtrack.git
cd jobtrack
```

### 2. Create the MySQL database

Run this in MySQL Workbench:

```sql
CREATE DATABASE jobtrack_db;
```

### 3. Configure environment variables

The application reads database settings from environment variables.

| Variable | Required | Default |
|---|---:|---|
| `DB_URL` | No | `jdbc:mysql://localhost:3306/jobtrack_db?serverTimezone=UTC` |
| `DB_USERNAME` | No | `root` |
| `DB_PASSWORD` | Yes | None |
| `DDL_AUTO` | No | `update` |
| `PORT` | No | `8080` |

In IntelliJ, add this to the application Run Configuration environment variables:

```text
DB_PASSWORD=your_mysql_password
```

Do not hard-code or commit the database password.

### 4. Run the application

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The API starts at:

```text
http://localhost:8080
```

## API Endpoints

### Companies

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/companies` | Create company |
| GET | `/api/companies` | Get all companies |
| GET | `/api/companies/{id}` | Get company by ID |
| PUT | `/api/companies/{id}` | Update company |
| DELETE | `/api/companies/{id}` | Delete company |

### Job Applications

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/applications` | Create application |
| GET | `/api/applications` | Get paginated applications |
| GET | `/api/applications/{id}` | Get application by ID |
| PUT | `/api/applications/{id}` | Full update |
| PATCH | `/api/applications/{id}/status` | Update only status |
| DELETE | `/api/applications/{id}` | Delete application |

### Search, Filters, and Dashboard

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/applications/search?keyword=java` | Search role or company |
| GET | `/api/applications/filter/status?status=APPLIED` | Filter by status |
| GET | `/api/applications/filter/job-type?jobType=INTERNSHIP` | Filter by job type |
| GET | `/api/applications/filter/date?startDate=2026-09-01&endDate=2026-09-30` | Filter by date range |
| GET | `/api/applications/upcoming` | Get today/future actions nearest first |
| GET | `/api/applications/statistics/status` | Get count by application status |

List, search, and filter endpoints accept:

```text
page=0
size=10
sort=applicationDate,desc
```

API page numbers are zero-based. The frontend displays page numbers beginning at 1.

Allowed sort fields:

```text
applicationDate
nextActionDate
jobRole
status
createdAt
```

## Sample Requests

### Create a company

```json
{
  "name": "Atlassian",
  "website": "https://www.atlassian.com",
  "location": "Bengaluru"
}
```

### Create a job application

```json
{
  "jobRole": "Java Backend Intern",
  "jobType": "INTERNSHIP",
  "status": "APPLIED",
  "applicationDate": "2026-09-12",
  "jobLink": "https://example.com/jobs/java-backend-intern",
  "location": "Bengaluru",
  "notes": "Applied through the careers page",
  "nextActionDate": "2026-09-18",
  "companyId": 1
}
```

### Update only the status

```json
{
  "status": "INTERVIEW"
}
```

## Error Response Format

The API returns a consistent error structure:

```json
{
  "timestamp": "2026-09-13T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/applications",
  "fieldErrors": {
    "jobRole": "Job role is required"
  }
}
```

Common status codes:

| Status | Meaning |
|---:|---|
| 400 | Invalid request, validation, invalid enum, pagination, or date range |
| 404 | Company or application not found |
| 409 | Duplicate job link or company still in use |
| 500 | Unexpected server error |

## Testing

Run all unit tests:

```powershell
.\mvnw.cmd clean test
```

The service-layer tests use Mockito, so they run without starting Spring Boot or connecting to MySQL.

## Swagger / OpenAPI

Start the application, then open:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

## Postman

Import this collection into Postman:

```text
postman/JobTrack.postman_collection.json
```

The collection uses one variable:

```text
baseUrl = http://localhost:8080
```

After deployment, update only `baseUrl` with the live backend URL.

## Docker

Docker support will be added in the next implementation hour.

## Deployment

Deployment configuration will be added after Docker setup.

## Live URL

To be added after deployment.

## Author

Md Asif  
GitHub: [mdasif-x1](https://github.com/mdasif-x1)