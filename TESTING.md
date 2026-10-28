# Service unit tests

Run from the FarmaGest project directory with JDK 21 and Maven:

```powershell
mvn test
mvn clean compile
```

`mvn test` runs JUnit 5 through Surefire and generates a JaCoCo report at
`target/site/jacoco/index.html`. Surefire XML and text results are in
`target/surefire-reports`. `clean` deletes these generated reports; run
`mvn test` again afterwards to regenerate them.

## Isolation and design

No MySQL server, database environment variables or Docker are required.
`ServiceTestSupport` intercepts DAO construction with Mockito's inline mock maker,
including short-lived DAOs created inside `StockOperations`. Each intercepted DAO
delegates to a named mock, so tests can configure results and verify calls across
multiple DAO instances. The static connection factory returns a mocked JDBC
connection. All mocking scopes are closed after each test.

Actual service methods, stock helpers, password hashing, amount calculations and
transaction handling execute. No service or transaction helper is mocked. This
allows commit/rollback checks without changing the production constructors or
introducing a dependency injection framework. Mockito is configured as a JVM
agent in Surefire; JaCoCo's agent argument is preserved.

The fixtures use real mutable model instances. Successful insert mocks assign IDs
where subsequent service calls need generated keys. Failure tests verify JDBC
rollback calls, not an in-memory database: model mutations are not undone by a
JDBC rollback.

## Functional coverage

| Service | Tested behavior |
| --- | --- |
| AuthService | Successful login, username trimming, hash removal, unknown/inactive user, wrong password, locked-row rechecks, failed last-login writes, authorization using persisted roles |
| UserService | Public reads, hash generation and cleanup, profile update preserving stored hash, deactivation, password change, invalid fields/passwords, missing users, failed writes and rollback |
| ProductService | CRUD, barcode uniqueness, category/manufacturer existence and activation, price/VAT/minimum stock validation, missing records, failed writes, SQL constraint translation |
| CategoryService | CRUD, required/oversized names, invalid IDs, missing records, failed writes, constraint translation and unexpected database failures |
| ManufacturerService | CRUD, required/oversized names, email/phone validation, invalid IDs, missing records, failed writes and database error translation |
| SupplierService | CRUD, required/oversized business names, email/phone validation, invalid IDs, missing records, failed writes and database error translation |
| CustomerService | CRUD, required names, oversized last names, email/phone validation, invalid IDs, missing records, failed writes and database error translation |
| PurchaseService | Order totals, multiple lines and product lock order, invalid quantity/cost/supplier, exact reception split across batches, persisted costs, expired/invalid/mismatched batches, order states, cancellation and DAO failures with rollback |
| SaleService | Persisted prices/VAT, line and order totals, product lock order, allocation across batches in DAO order, stock shortage after partial allocation, customer/product validation, cancellation aggregation, expired-batch restoration, inconsistent allocations, state guards and DAO failures with rollback |
| InventoryService | Read views, positive/negative adjustment, movement contents, shortage/overflow, inactive actors/batches, expired stock restrictions, batch expiry, DAO failure after stock write and rollback |
| AuditService | History queries, standalone/shared transactions, credential-marker rejection, user snapshot prohibition, invalid entries, failed writes and rollback |

Additional tests cover `Checks`, `PasswordHasher` and `Transactions`, including
tax rounding, money precision, duplicate line instances, malformed hashes,
random salts, commit failures, rollback failures and auto-commit restoration.
`ContactValidationTest` adds shared validation cases for the relevant services.
The earlier standalone `ServiceVerification` utility is preserved; JUnit replaces
it as the normal automated verification path.

## Limits

There was no existing automated MySQL integration infrastructure, so no database
integration tests or Testcontainers dependency were added. This suite does not
validate SQL syntax, schema constraints, actual JDBC persistence, row locking or
concurrent transactions. FEFO filtering and ordering belong to ProductBatchDAO;
service tests verify consumption in the order returned by that DAO.

JaCoCo line/branch coverage measures execution, not completeness of all possible
business inputs. The report covers the whole application, but the scope of this
suite is the `com.farmagest.service` package; DAO/model coverage is not its target.
