# API Reference

Base URL: `http://localhost:8081/api`

All requests and responses use JSON unless noted. No authentication is required at the moment.

**Errors** come back in Spring Boot's default error format with the matching HTTP status:

```json
{
  "timestamp": "2026-10-13T10:15:30.000+00:00",
  "status": 404,
  "error": "Not Found",
  "message": "No account with id 'unknown' does not exist",
  "path": "/api/accounts/unknown"
}
```

> Amounts are decimals with two places (`NUMERIC(12,2)`). Dates use ISO format (`YYYY-MM-DD`), and months use `YYYY-MM`.

---

## Accounts

An account is a bank or cash account with a running balance. Recording a transaction updates the balance automatically.

### Account object

```json
{
  "id": "bpi-savings",
  "name": "Savings",
  "bank": "BPI",
  "icon": "Wallet",
  "color": "#6B8CAE",
  "balance": 25000.00
}
```

### `GET /accounts`

Returns every account.

**200 OK**: `Account[]`

### `GET /accounts/{accountId}`

Returns one account.

| Status | When                     |
| ------ | ------------------------ |
| 200    | Found                    |
| 404    | No account with that id  |

### `POST /accounts`

Creates an account. The client chooses the `id`.

```json
{
  "id": "bpi-savings",
  "name": "Savings",
  "bank": "BPI",
  "icon": "Wallet",
  "color": "#6B8CAE",
  "balance": 25000.00
}
```

| Field     | Type    | Rules                                          |
| --------- | ------- | ---------------------------------------------- |
| `id`      | string  | Required. Lowercase letters, digits, hyphens   |
| `name`    | string  | Required                                       |
| `bank`    | string  | Required                                       |
| `icon`    | string  | Required (icon name used by the frontend)      |
| `color`   | string  | Required (hex color)                           |
| `balance` | decimal | Required, 0 or more                            |

| Status | When                                              |
| ------ | ------------------------------------------------- |
| 202    | Created. The body is the saved account            |
| 400    | A field fails validation                          |
| 409    | An account with that `id` already exists          |

---

## Categories

A category is a budget bucket, such as Needs or Leisure, with a monthly budget amount. Four are seeded on first start: `needs`, `invest`, `emergency` and `leisure`.

### Category object

```json
{
  "id": "needs",
  "name": "Needs",
  "icon": "Home",
  "color": "#6B8CAE",
  "budget": 1800.00
}
```

### `GET /categories`

Returns every category.

**200 OK**: `Category[]`

### `POST /categories`

Creates a category. The client chooses the `id`.

```json
{
  "id": "groceries",
  "name": "Groceries",
  "icon": "ShoppingCart",
  "color": "#7A9B76",
  "budget": 500.00
}
```

| Field    | Type    | Rules                                        |
| -------- | ------- | -------------------------------------------- |
| `id`     | string  | Required. Lowercase letters, digits, hyphens |
| `name`   | string  | Required                                     |
| `icon`   | string  | Required                                     |
| `color`  | string  | Required                                     |
| `budget` | decimal | Required, at least 0.01                      |

| Status | When                                       |
| ------ | ------------------------------------------ |
| 202    | Created. The body is the saved category    |
| 400    | A field fails validation                   |
| 409    | A category with that `id` already exists   |

### `PUT /categories/{categoryId}`

Updates only the category's budget.

```json
{ "budget": 2000.00 }
```

| Status | When                        |
| ------ | --------------------------- |
| 202    | Updated. The body is the category |
| 404    | No category with that id    |

### `DELETE /categories/{categoryId}`

| Status | When                                         |
| ------ | -------------------------------------------- |
| 204    | Deleted                                      |
| 404    | No category with that id                     |
| 409    | The category still has transactions          |

---

## Transactions

### Transaction object

The linked `category` and `account` come back as full nested objects. `category` is `null` for income.

```json
{
  "id": 42,
  "date": "2026-10-05",
  "description": "Weekly groceries",
  "type": "EXPENSE",
  "category": { "id": "needs", "name": "Needs", "icon": "Home", "color": "#6B8CAE", "budget": 1800.00 },
  "account":  { "id": "bpi-savings", "name": "Savings", "bank": "BPI", "icon": "Wallet", "color": "#6B8CAE", "balance": 24850.00 },
  "amount": 150.00
}
```

### `GET /transactions`

Returns every transaction, newest first.

### `GET /transactions/{accountId}`

Returns one account's transactions, newest first.

| Status | When                     |
| ------ | ------------------------ |
| 200    | OK                       |
| 404    | No account with that id  |

### `POST /transactions`

Records an income or an expense and updates the account balance.

```json
{
  "date": "2026-10-05",
  "description": "Weekly groceries",
  "type": "EXPENSE",
  "categoryId": "needs",
  "accountId": "bpi-savings",
  "amount": 150.00
}
```

| Field         | Type    | Rules                                          |
| ------------- | ------- | ---------------------------------------------- |
| `date`        | date    | Required                                       |
| `description` | string  | Required                                       |
| `type`        | string  | Required. `EXPENSE` or `INCOME` (any case)     |
| `categoryId`  | string  | Required for `EXPENSE`, ignored for `INCOME`   |
| `accountId`   | string  | Required                                       |
| `amount`      | decimal | Required, at least 0.01                        |

What happens:

- **EXPENSE**: the amount must not exceed the account balance. The balance goes down by `amount`.
- **INCOME**: the balance goes up by `amount`. No category is stored.

| Status | When                                                              |
| ------ | ----------------------------------------------------------------- |
| 201    | Recorded. The body is the saved transaction                       |
| 400    | A field fails validation, `type` isn't `EXPENSE`/`INCOME`, an expense has no `categoryId`, or the expense is larger than the balance |
| 404    | The account or the expense category doesn't exist                 |

If the request fails, the account balance is left unchanged.

---

## Reports

### `GET /reports/months`

Lists the months that have at least one transaction, newest first.

```json
["2026-10", "2026-09", "2026-08"]
```

### `GET /reports/monthly?month=YYYY-MM`

Returns a summary of one month.

```json
{
  "month": "2026-10",
  "totalSpent": 1240.50,
  "transactionCount": 18,
  "byCategory": [
    { "categoryId": "needs", "name": "Needs", "color": "#6B8CAE", "total": 900.00 },
    { "categoryId": "leisure", "name": "Leisure", "color": "#C97B84", "total": 340.50 }
  ],
  "transactions": [ /* Transaction objects, newest first */ ]
}
```

- `totalSpent` adds up **expenses only**.
- `transactionCount` counts income and expenses.
- `byCategory` is sorted by total, highest first. Income has no category, so it doesn't appear here.

A badly formatted `month` returns 400.

### `GET /reports/monthly/pdf?month=YYYY-MM`

Returns the same report as an A4 PDF statement (`Content-Type: application/pdf`), downloaded as `household-ledger-YYYY-MM.pdf`. The statement contains a summary, the category breakdown and the full transaction list.

```bash
curl -o statement.pdf "http://localhost:8081/api/reports/monthly/pdf?month=2026-10"
```
