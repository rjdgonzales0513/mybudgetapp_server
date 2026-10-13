# My Budget App — Server

REST API for a household budgeting app. It tracks **accounts** (bank balances), **budget categories**, and **transactions** (income and expenses), and produces **monthly reports** as JSON or a downloadable PDF statement.

Built with Spring Boot 3.5, Java 21, Spring Data JPA, PostgreSQL, Flyway and PDFBox. The React/Vite frontend lives in a separate repository.

## Quick start

### Prerequisites

- Java 21
- PostgreSQL (or Docker)

### 1. Start PostgreSQL

Using Docker:

```bash
docker run -d --name mybudget-db -p 5432:5432 \
  -e POSTGRES_DB=mybudgetapp -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=Password@123 \
  postgres:17
```

Or create a `mybudgetapp` database in an existing local Postgres and update the credentials in `src/main/resources/application.properties`.

### 2. Run the server

```bash
./mvnw spring-boot:run        # macOS / Linux / Git Bash
.\mvnw.cmd spring-boot:run    # Windows PowerShell
```

The API is served at `http://localhost:8081/api`. On first start Flyway creates the tables and seeds four default categories (Needs, Investments, Emergency Fund, Leisure).

### 3. Try it

```bash
curl http://localhost:8081/api/categories
```

## Running the full stack with Docker Compose

`docker-compose.yml` runs Postgres, this backend and the frontend together. Build the images first (the frontend image comes from the frontend repo):

```bash
docker build -t mybudgetappserver .
docker compose up
```

| Service  | URL                     |
| -------- | ----------------------- |
| Frontend | http://localhost:5173   |
| Backend  | http://localhost:8081   |
| Postgres | localhost:5432          |

## Configuration

Settings live in `src/main/resources/application.properties`. Each can be overridden with an environment variable:

| Property                     | Env var                      | Default                                       |
| ---------------------------- | ---------------------------- | --------------------------------------------- |
| `server.port`                | `SERVER_PORT`                | `8081`                                        |
| `spring.datasource.url`      | `SPRING_DATASOURCE_URL`      | `jdbc:postgresql://localhost:5432/mybudgetapp` |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | `postgres`                                    |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | `Password@123`                                |

CORS currently allows only `http://localhost:5173`. To allow another frontend origin, add it in `security/SecurityConfig.java`.

## Common commands

```bash
./mvnw clean package                   # build target/mybudgetappserver-0.0.1-SNAPSHOT.jar
./mvnw test                            # run tests
./mvnw test -Dtest=ClassName#method    # run one test
```

## Documentation

- [API reference](docs/API.md): every endpoint, with request and response examples
- [Architecture](docs/ARCHITECTURE.md): code layout, data model, business rules, known limitations
- [Deployment](docs/DEPLOYMENT.md): Docker, GitLab CI/CD and Kubernetes

## API at a glance

| Method | Path                                   | Description                         |
| ------ | -------------------------------------- | ----------------------------------- |
| GET    | `/api/accounts`                        | List accounts                       |
| POST   | `/api/accounts`                        | Create an account                   |
| GET    | `/api/accounts/{accountId}`            | Get one account                     |
| GET    | `/api/categories`                      | List categories                     |
| POST   | `/api/categories`                      | Create a category                   |
| PUT    | `/api/categories/{categoryId}`         | Update a category's budget          |
| DELETE | `/api/categories/{categoryId}`         | Delete a category                   |
| GET    | `/api/transactions`                    | List all transactions               |
| POST   | `/api/transactions`                    | Record an income or expense         |
| GET    | `/api/transactions/{accountId}`        | List an account's transactions      |
| GET    | `/api/reports/months`                  | Months that have transactions       |
| GET    | `/api/reports/monthly?month=YYYY-MM`   | Monthly report (JSON)               |
| GET    | `/api/reports/monthly/pdf?month=YYYY-MM` | Monthly statement (PDF download)  |
