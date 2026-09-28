# DailyCred

DailyCred is a Spring Boot REST API for managing short-term lending workflows between borrowers and lenders. It covers borrower and lender onboarding, authentication, loan plans, loan applications, repayments, wallet transactions, KYC document processing, analytics, notifications, reports, and admin oversight actions.

## Tech Stack

- Java 17
- Spring Boot 4.0.5
- Spring Web MVC
- Spring Data JPA
- Spring Security with JWT authentication
- PostgreSQL
- Lombok
- Tess4J for OCR
- OpenPDF for PDF report generation
- Maven Wrapper

## Project Structure

```text
src/main/java/com/unqiuehire/kashflow
|-- config          # CORS, security, password, and scheduler configuration
|-- constant        # Enums and application constants
|-- controller      # REST API controllers
|-- dto             # Request and response DTOs
|-- entity          # JPA entities
|-- exception       # Application exceptions and global exception handling
|-- repository      # Spring Data repositories
|-- scheduler       # Scheduled loan monitoring jobs
|-- security        # JWT filter and token utilities
|-- service         # Service interfaces
`-- serviceimpl     # Service implementations
```

## Main Features

- JWT-based login and secured API access
- Borrower, lender, and admin account management
- Loan plan creation and lender-specific plan management
- Loan application submission, lookup, and decision handling
- Loan creation, borrower/lender loan lookup, and loan closure
- Repayment tracking by loan, borrower, and loan application
- Wallet balance, top-up, withdrawal, freeze, and transaction history
- KYC document upload, OCR extraction, and KYC summaries
- Borrower and lender analytics dashboards
- Cash collection confirmation workflow
- Reward and penalty event tracking
- PDF report generation
- Admin oversight for freeze, blacklist, fraud flag, and borrower policy overrides
- Scheduled loan monitoring

## Prerequisites

- JDK 17
- PostgreSQL running locally or accessible over the network
- Maven is optional because the project includes `mvnw` and `mvnw.cmd`
- Tesseract OCR if OCR is enabled

## Configuration

Application configuration is stored in:

```text
src/main/resources/application.properties
```

Current defaults:

```properties
server.port=8080
spring.datasource.url=jdbc:postgresql://localhost:5432/loanapplication_db
spring.datasource.username=postgres
spring.datasource.password=raju
spring.jpa.hibernate.ddl-auto=update

ocr.tesseract.enabled=true
ocr.tesseract.datapath=C:/Program Files/Tesseract-OCR/tessdata
ocr.tesseract.language=eng

scheduler.loan-monitor.enabled=true
scheduler.loan-monitor.cron=0 5 0 * * *

app.jwt.secret=my-super-secret-key-for-jwt-token-12345678901234567890
app.jwt.expiration-ms=36000000
```

For local development, create the PostgreSQL database before starting the app:

```sql
CREATE DATABASE loanapplication_db;
```

For production or shared environments, move database credentials and `app.jwt.secret` out of `application.properties` and provide them through environment-specific configuration.

## Run Locally

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS or Linux:

```bash
./mvnw spring-boot:run
```

The API starts on:

```text
http://localhost:8080
```

## Build and Test

Build the project:

```powershell
.\mvnw.cmd clean package
```

Run tests:

```powershell
.\mvnw.cmd test
```

## Authentication

Most endpoints require a JWT bearer token.

Public endpoints:

- `POST /api/auth/login`
- `POST /api/borrowers`
- `POST /api/lenders`
- `POST /api/admin/accounts`

Admin endpoints under `/api/admin/**` require the `ADMIN` role.

Authenticated requests should include:

```http
Authorization: Bearer <jwt-token>
```

## API Overview

| Area | Base path | Description |
| --- | --- | --- |
| Authentication | `/api/auth` | Login and token generation |
| Borrowers | `/api/borrowers` | Borrower creation, lookup, update, and delete |
| Lenders | `/api/lenders` | Lender creation, lookup, update, and delete |
| Admin accounts | `/api/admin/accounts` | Admin account creation and lookup |
| Admin oversight | `/api/admin/oversight` | Freeze, blacklist, fraud flag, and policy override actions |
| Loan plans | `/loan-plans` | Lender loan plan creation, lookup, and update |
| Loan applications | `/api/loan-application` | Apply for loans and manage application decisions |
| Loans | `/api/loans` | Create, view, list, and close loans |
| Repayments | `/api/repayments` | Record and query repayments |
| Wallets | `/api/wallets` | Wallet balance, transactions, top-up, withdrawal, freeze, and unfreeze |
| KYC | `/api/kyc` | Upload KYC documents, run OCR, and view KYC summaries |
| Cash collections | `/api/cash-collections` | Lender initiated cash collection and borrower confirmation |
| Notifications | `/api/notifications` | Create, list, and mark notifications as read |
| Location | `/api/location` | Update locations and find nearby lenders or borrower location |
| Borrower analytics | `/api/borrower-analytics` | Borrower analytics summary |
| Lender analytics | `/api/lender-analytics` | Dashboard, collections, plan performance, and borrower risk |
| Reports | `/api/reports` | PDF reports for lender dashboard and borrower analytics |
| Reward events | `/api/reward-events` | Reward event lookup |
| Penalty events | `/api/penalty-events` | Penalty event lookup |

## Notes

- Package names currently use `com.unqiuehire.kashflow`.
- Hibernate is configured with `ddl-auto=update`, so schema changes are applied automatically during development.
- OCR expects Tesseract trained data at the configured `ocr.tesseract.datapath`.
- Scheduled loan monitoring runs daily at 12:05 AM by default.
