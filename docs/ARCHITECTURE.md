# Architecture

## Tech stack

| Concern        | Choice                                     |
| -------------- | ------------------------------------------ |
| Runtime        | Java 21, Spring Boot 3.5                   |
| Web            | Spring Web MVC (REST controllers)          |
| Persistence    | Spring Data JPA (Hibernate) + PostgreSQL   |
| Schema         | Flyway migrations                          |
| Security       | Spring Security (all API routes open for now) |
| PDF            | Apache PDFBox 3                            |
| Boilerplate    | Lombok                                     |

## Code layout

```
src/main/java/com/rjdg/mybudget/
├── MyBudgetAppServer.java      # Spring Boot entry point
├── controller/                 # REST endpoints under /api/**
├── service/                    # business rules + PDF generation
├── repository/                 # Spring Data JPA interfaces
├── model/                      # JPA entities
├── dto/                        # request/response records
└── security/SecurityConfig.java
src/main/resources/
├── application.properties
└── db/migration/               # Flyway SQL migrations
```

Requests go through these layers:

```
HTTP ─▶ Controller ─▶ Service ─▶ Repository ─▶ PostgreSQL
                         │
                         └─▶ PdfStatementGenerator (reports only)
```

| Controller              | Service(s)                                 |
| ----------------------- | ------------------------------------------ |
| `AccountController`     | `AccountService`                           |
| `CategoryController`    | `BudgetService`                            |
| `TransactionController` | `TransactionService`                       |
| `ReportController`      | `BudgetService`, `PdfStatementGenerator`   |

## Data model

```
┌─────────────┐        ┌──────────────────┐        ┌─────────────┐
│  category   │ 1    * │   transaction    │ *    1 │   account   │
│─────────────│◀───────│──────────────────│───────▶│─────────────│
│ id (PK, str)│        │ id (PK, bigserial)│        │ id (PK, str)│
│ name        │        │ date             │        │ name        │
│ icon        │        │ description      │        │ bank        │
│ color       │        │ type             │        │ icon        │
│ budget      │        │ amount           │        │ color       │
└─────────────┘        │ category_id (FK) │        │ balance     │
                       │ account_id (FK)  │        └─────────────┘
                       └──────────────────┘
```

- `category.id` and `account.id` are slugs chosen by the client, such as `needs` or `bpi-savings`.
- `transaction.type` is a string, either `EXPENSE` or `INCOME`.
- `transaction.category_id` is nullable, because income has no category.
- Money columns are `NUMERIC(12,2)`, mapped to `BigDecimal`.

### Migrations

Hibernate never changes the schema (`spring.jpa.hibernate.ddl-auto=none`); Flyway manages it all. To change it:

1. Add `src/main/resources/db/migration/V<n>__<description>.sql`, for example `V2__add_notes_to_transaction.sql`.
2. Update the matching entity in `model/`.
3. Restart. Flyway applies the new migration on startup.

Don't edit a migration that has already run (`V1__initial_table.sql` included). Flyway checks each migration's checksum and refuses to start if one has changed.

## Business rules

### Recording a transaction (`TransactionService.addTransaction`)

The method is `@Transactional`, so the balance change and the new transaction are saved together or not at all.

1. Look up the account. If it's missing, return 404.
2. If `type` is `EXPENSE`:
   - require a `categoryId` (400) and look the category up (404 if it's missing)
   - require `amount <= balance` (400 "Insufficient balance")
   - subtract the amount from the account balance and save the transaction with its category
3. If `type` is `INCOME`, add the amount to the balance and save the transaction with no category.
4. Any other `type` returns 400.

### Deleting a category (`BudgetService.deleteCategory`)

A category that still has transactions can't be deleted (409). Delete or move its transactions first.

### Monthly report (`BudgetService.getMonthlyReport`)

The report loads every transaction between the first and last day of the month and builds the summary in memory:

- `totalSpent` is the sum of `EXPENSE` amounts.
- `byCategory` totals amounts per category and is sorted from highest to lowest.

`PdfStatementGenerator` lays out the same `MonthlyReport` with PDFBox: a title, the summary, the category table and the transaction list, adding pages as needed.

## Security & CORS

`SecurityConfig`:

- `/api/**` is open to everyone (`permitAll`). Any other path requires authentication, but there are no other endpoints.
- CSRF protection is turned off.
- `/error` is also open. Spring forwards failed requests there, and blocking it would turn every error into a 403.
- CORS allows `GET, POST, PUT, DELETE, OPTIONS` from `http://localhost:5173` only.

The `jjwt` libraries are already dependencies in preparation for JWT login, but nothing uses them yet.

## Known limitations

- **Create endpoints return 202 Accepted.** Accounts and categories return 202 where 201 Created would be the right status.
- **`getAvailableMonths` loads every transaction** into memory to find the distinct months.
- **No tests yet.**
