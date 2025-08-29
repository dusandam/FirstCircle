# FirstCircle Banking Service
## Task Description

Develop a service that simulates basic banking operations. This service manages accounts and allows deposits, withdrawals, and transfers between accounts. The system is designed to reflect real-world banking constraints.

## Requirements

### Core Operations

- Account creation – Users can create an account with an initial deposit.

- Deposit – Users can deposit money into their account.

- Withdrawal – Users can withdraw money from their account; overdrafts are not allowed.

- Transfer – Users can transfer funds between accounts.

- Account balance – Users can check the balance of their account.

### Data Storage

In-memory storage is sufficient; a full database is optional.

Note: The term service here refers to a software component/module, not a deployable API

## Implementation Highlights

### Language & Framework: Kotlin + Spring.

### Concurrency & Thread-Safety:

- Transfers use per-account locking to prevent race conditions.

- Audit log is synchronized to ensure correct ordering.

### Domain Design:

Domain-driven design (DDD) principles applied:

- Account and Money as core domain objects.

- BankingService handles use cases.

### Idempotency:

- Each operation accepts a UUID key to prevent duplicate execution.

### Tamper-evident Audit Log:

- All state-changing operations are recorded with a hash chain.

## Running the Tests
Tests are implemented using Kotest. They cover:

- Account creation, deposit, withdrawal, and transfers.

- Idempotency: retrying operations does not duplicate effects.

- Audit logging: all operations are logged in order.

- Edge cases: overdraft prevention, self-transfer prevention.

### System Requirements:
- JDK 17+
- Maven 3.8+
- Kotlin 2.0.21
- Spring Boot 3.5.5
## Testing
- JUnit 5 (via Spring Boot Test starter)
- Kotest 5.9.1 (JUnit5 runner)
  
##Quickstart:

mvn test
