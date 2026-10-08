# Online Auction Bidding System

A small Spring Boot 3 web application for a college Software Engineering assessment.
This is an educational simulation; it does not process real payments.

## Requirements

- Java 17
- Maven

## Run locally

From this directory, run:

```powershell
mvn spring-boot:run
```

Then open [http://localhost:8081/login](http://localhost:8081/login).
The application uses an H2 file database at `./data/auctiondb`; the local database
is intentionally excluded from version control.

## Demo accounts

These are synthetic, public demonstration credentials loaded on first startup only:

| Role | ID | PIN | Account balance/status |
|---|---|---|---|
| Customer | A101 | 1111 | Rs.25,000 / ACTIVE |
| Customer | A102 | 2222 | Rs.8,000 / ACTIVE |
| Customer | A103 | 3333 | Rs.12,000 / INACTIVE |
| Administrator | ADMIN01 | 9999 | — |

Do not use these credentials for any real system.

## Demo workflow

1. Sign in as `A101` with PIN `1111`.
2. Enter the auction item reference and an eligible whole-rupee bid.
3. Validate the bid and review the amount, charge, and total debit.
4. Re-enter the PIN to authorise, then complete the transaction.
5. View the transaction in customer history. An eligible successful transaction
   can be reversed once.
6. Sign in as `ADMIN01` with PIN `9999` to inspect transactions and audit history.

## Rules and limitations

- Bid amount must be a whole rupee value from Rs.100 through Rs.20,000.
- The account must be ACTIVE and have sufficient balance for the total debit.
- The charge is Rs.10 for bids below Rs.1,000 and Rs.0 for bids of Rs.1,000 or more.
- Request references prevent duplicate bid submissions.
- Customer transaction access is restricted to the signed-in customer.
- Login is a simple session-based simulation without Spring Security.
- No real payment gateway, third-party integrations, or live bidding are included.

## Database schema

The H2 schema is generated from the JPA entities on startup. It stores users,
accounts, transactions, and transaction history. The transaction record preserves
the authorised amount, charge, total debit, unique reference, owner, account,
auction item reference, status, and creation time. The history record stores the
reference, actor, old/new status, and event time.
