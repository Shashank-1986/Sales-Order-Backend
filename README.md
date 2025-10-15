# Sales Order Service

This is a Spring Boot application that provides a RESTful API for managing sales orders and a product catalog. It includes features like JWT authentication, role-based access control, optimistic locking for concurrency, and environment-specific configurations.

## Architecture and Design

The application is built using a **Layered Monolithic Architecture**. This is a robust and highly effective pattern that promotes a clean separation of concerns, making the application easier to test, maintain, and scale.

### Key Design Patterns

-   **Layered Architecture**: The code is strictly separated into:
    -   **Controller/Web Layer**: Handles HTTP requests, authentication, and data validation.
    -   **Service/Business Layer**: Contains the core business logic and orchestrates data operations.
    -   **Repository/Data Access Layer**: Manages all communication with the database.
-   **Data Transfer Object (DTO)**: We use DTOs (e.g., `SalesOrderRequestDto`) to decouple the public API contract from the internal database schema. This is a critical security and design pattern that prevents accidental data exposure and allows the database model to evolve independently of the API.
-   **Centralized Exception Handling**: A global `@ControllerAdvice` class intercepts exceptions (e.g., `EntityNotFoundException`, `MethodArgumentNotValidException`) and formats them into consistent, user-friendly JSON error responses with appropriate HTTP status codes.

## Advanced Features

### Security

-   **Authentication**: Implemented using **JSON Web Tokens (JWT)** and Spring Security. The application is stateless, which is ideal for load-balanced and scalable deployments. Users authenticate via a `/api/auth/signin` endpoint to receive a bearer token.
-   **Authorization**: Access to endpoints is controlled by user roles.
    -   `ROLE_USER` (default): Can manage sales orders.
    -   `ROLE_ADMIN`: Can manage the product catalog (`/api/products/**`).

### Concurrency

-   **Optimistic Locking**: To prevent data inconsistencies from simultaneous modifications (race conditions), the `Product` entity uses the `@Version` annotation. If two users attempt to update the same product, the second user's request will be rejected with an **HTTP 409 Conflict** error, preventing lost updates.

### Scalability and Performance

-   **Caching**: The application uses Spring's caching abstraction (`@Cacheable`) on the `ProductService`. This significantly reduces database load and improves response times for frequently accessed, infrequently changed data like the product catalog.
-   **N+1 Query Optimization**: The `SalesOrderRepository` uses an `@EntityGraph` to solve the classic N+1 query problem. This ensures that when a list of sales orders is fetched, their associated items are retrieved in a single, efficient query.

### Monitoring

-   **Implementation**: This project includes **Spring Boot Actuator**, which exposes production-ready monitoring endpoints. Key endpoints like `/actuator/health`, `/actuator/metrics`, and `/actuator/prometheus` are enabled.
-   **Production Strategy**: In a production environment, a monitoring server like **Prometheus** would be configured to periodically "scrape" the `/actuator/prometheus` endpoint. The collected time-series data would then be visualized in **Grafana** dashboards to provide real-time insights into application health, performance (JVM memory, CPU usage, HTTP request latencies), and error rates.

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java Development Kit (JDK) 17** or later.
- **Docker Desktop**: Required for running the integration tests, which use Testcontainers to spin up a database.
- **MySQL Database** (for local development): You need a running MySQL instance for the `development` profile.

## Local Development Setup

1.  **Clone the repository:**
    ```bash
    git clone <your-repository-url>
    cd sales-order
    ```

2.  **Set up the development database:**
    -   Create a MySQL database named `sales_order_dev`.
    -   Create a MySQL user named `sales_order_dev` with a password.
    -   Grant this user full permissions on the `sales_order_dev` database.

3.  **Configure the database password:**
    -   Open the file `src/main/resources/application-development.properties`.
    -   Uncomment the `spring.datasource.password` line and add the password for your `sales_order_dev` user.
    ```properties
    spring.datasource.password=your_local_db_password
    ```

## Compiling and Running

### Database Schema Management

The application uses different strategies for managing the database schema depending on the active Spring profile. This is controlled by the `spring.jpa.hibernate.ddl-auto` property.

#### Schema Generation (Development and Testing)

-   **`development` profile (`ddl-auto: update`)**: When you run the application with the `development` profile, Hibernate automatically compares your JPA entities (e.g., `SalesOrder`, `Product`) with the database schema. If it finds missing tables or columns, it will attempt to add them. This is convenient for local development but is not safe for production.

-   **`testing` profile (`ddl-auto: create-drop`)**: For automated tests using Testcontainers, the schema is created from scratch at the beginning of the test run and the container is destroyed at the end. This ensures that each test run is isolated and starts with a clean database.

#### Generating Migration Scripts (Development Workflow)

When you make changes to your JPA entities (e.g., adding a new field), you need to generate the corresponding SQL `ALTER TABLE` script to be used in a migration tool for production.

A common workflow is to use Hibernate's schema generation tools:

1.  **Make your changes** to the entity classes in your IDE.
2.  **Temporarily modify** your `application-development.properties` to generate the script instead of updating the database directly. Change `ddl-auto` to `none` and add the following properties:
    ```properties
    # Temporarily add these lines to generate the update script
    spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=update
    spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=src/main/resources/db/migration/V__generated.sql
    spring.jpa.hibernate.ddl-auto=none
    ```
3.  **Run the application.** Hibernate will not touch the database but will instead generate a file named `V__generated.sql` containing the necessary `ALTER TABLE` statements.
4.  **Create a versioned migration file:** Copy the contents of the generated script into a new, properly versioned file (e.g., `V2__add_new_column_to_product.sql`) and review it for correctness.
5.  **Revert the changes** in `application-development.properties` back to `spring.jpa.hibernate.ddl-auto=update`.

#### Schema Migration (Recommended for Production)

-   **`production` profile (`ddl-auto: validate`)**: In production, the application is configured to `validate` the schema on startup. It will fail to start if the JPA entities do not perfectly match the database schema. This is a crucial safety feature to prevent accidental data loss or corruption.

For a production environment, changes to the database schema should be managed explicitly and controllably using a dedicated database migration tool.

**Recommended Tool: Flyway or Liquibase**

While not currently implemented, the recommended approach for managing production schema changes is to integrate a tool like Flyway or Liquibase.

These tools work by maintaining a series of versioned SQL scripts in your source code (e.g., `V1__Create_tables.sql`, `V2__Add_product_version_column.sql`). When the application starts, the tool checks which scripts have already been applied to the database and executes only the new ones.

**Benefits of a Migration Tool:**
-   **Version Control:** Your database schema changes are versioned and tracked in Git, just like your application code.
-   **Reliability:** Provides a reliable, repeatable process for updating the database schema across all environments (dev, staging, prod).
-   **Rollbacks:** Supports rolling back changes if something goes wrong.

To implement this, you would add the tool's dependency to your `pom.xml` and place your SQL migration scripts in `src/main/resources/db/migration`.

---

## Compiling and Running

This project uses the Maven Wrapper (`mvnw`), so you do not need to have Maven installed globally.

### Compiling the Application

To compile the source code, run the following command from the project root:

```bash
# For macOS/Linux
./mvnw compile

# For Windows
mvnw.cmd compile
```

### Running the Application (Development Profile)

The application is configured to use the `development` profile by default. To run the application:

```bash
# For macOS/Linux
./mvnw spring-boot:run

# For Windows
mvnw.cmd spring-boot:run
```

The application will start on `http://localhost:8080`.

### Running Tests

The project includes both unit tests and integration tests.

-   **Unit Tests**: These are fast and test individual components in isolation.
-   **Integration Tests**: These test the full application stack and require **Docker** to be running, as they will automatically start a MySQL container.

To run all tests:

```bash
# For macOS/Linux
./mvnw test

# For Windows
mvnw.cmd test
```

---

## Deployment and CI/CD

### CI/CD Pipeline

A typical Continuous Integration/Continuous Deployment (CI/CD) pipeline for this project (e.g., using GitHub Actions) would follow these steps:

1.  **Trigger**: The pipeline is automatically triggered on a push to the `main` or `develop` branch.
2.  **Set Up Environment**: The CI runner checks out the code and sets up JDK 17 and a Docker environment.
3.  **Run Tests**: The pipeline executes `./mvnw test`. This command runs all unit tests and the integration tests, which automatically start a MySQL database using Testcontainers. This step is critical to ensure code quality and prevent regressions.
4.  **Build Artifact**: If all tests pass, the application is packaged into an executable JAR file using `./mvnw clean package`.
5.  **Automated Deployment**:
    -   The resulting JAR file is deployed to the target environment (e.g., a cloud-hosted virtual machine).
    -   The `SPRING_PROFILES_ACTIVE` environment variable is set to `production` (or `testing`).
    -   Secure environment variables (like `PROD_DB_PASSWORD`) are injected into the environment.
    -   The application is started using the `java -jar` command.

### Packaging and Running for Production

To create a self-contained, executable JAR file for production, run:

```bash
./mvnw clean package
```

This command will run all tests and, if they pass, create a JAR file in the `target/` directory (e.g., `target/sales-order-0.0.1-SNAPSHOT.jar`).

### Running the Packaged Application

To run the application using the `production` profile, you must activate it via a system property or environment variable. This ensures that production-safe settings are used (e.g., connecting to the production database).

You will also need to provide the production database password as an environment variable (`PROD_DB_PASSWORD`), as it is not hardcoded in the configuration files.

```bash
export PROD_DB_PASSWORD="your_production_db_password"
java -jar -Dspring.profiles.active=production target/sales-order-0.0.1-SNAPSHOT.jar
```
