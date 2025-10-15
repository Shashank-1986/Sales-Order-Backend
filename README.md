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


Project Q&A
This section answers common questions about the project's design, scalability, security, and operational practices.

1. What architecture and design patterns did you use and why?
We chose a Layered Monolithic Architecture for its simplicity and effectiveness in creating a clean, maintainable codebase. This architecture strictly separates the application into three distinct layers:

Controller/Web Layer: This is the entry point for all incoming HTTP requests. Its only job is to handle web-related tasks like validating requests and converting data into a format the application can use.
Service/Business Layer: This is the core of the application. It contains all the business logic (e.g., how to create an order, calculate totals, or cancel an order) and coordinates the flow of data.
Repository/Data Access Layer: This layer is responsible for all communication with the database, abstracting away the complexities of data persistence.
This separation was chosen because it makes the application much easier to develop, test, and maintain. For example, we can test the business logic in the service layer without needing to start a web server.

We also used the Data Transfer Object (DTO) pattern. Instead of exposing our internal database models directly through the API, we use DTOs (like SalesOrderRequestDto). This is a crucial design choice for two reasons:

Security: It prevents sensitive or internal data from being accidentally exposed to the outside world.
Flexibility: It allows us to change our database structure without breaking the public API contract that our clients depend on.
2. How would you ensure the scalability and availability of the solution?
The application was designed with scalability and high availability in mind.

Scalability:

Stateless Design: The application is stateless, using JSON Web Tokens (JWT) for authentication. This means any instance of the application can handle any user's request, allowing us to easily scale horizontally. We can run multiple instances of the application behind a load balancer to handle increased traffic.
Efficient Data Handling: We've implemented caching for frequently accessed, rarely changed data (like the product catalog) to reduce database load. We also use techniques like @EntityGraph to prevent the "N+1 query problem," ensuring our database interactions are highly efficient.
Availability:

Redundancy: By running multiple application instances, we ensure that if one instance fails, the load balancer will automatically redirect traffic to the healthy ones, preventing downtime.
Health Checks: The application includes Spring Boot Actuator, which exposes a /actuator/health endpoint. The load balancer uses this endpoint to monitor the health of each instance and ensure it only sends traffic to fully functional ones.
Database High Availability: In a production environment, we would use a managed database service (like AWS RDS) configured for multi-availability zone deployment. This creates a standby replica of the database in a different physical location, which can take over automatically if the primary database fails.
3. What security mechanisms did you implement and how would you validate them?
We implemented several layers of security to protect the application and its data.

Authentication: We use JSON Web Tokens (JWT). When a user signs in, they receive a digitally signed token. For every subsequent request to a protected endpoint, they must present this token in the Authorization header. The application validates the token's signature and expiration to confirm the user's identity.
Authorization: We use Role-Based Access Control (RBAC), managed by Spring Security. Access to different API endpoints is restricted based on user roles. For example, only users with the ROLE_ADMIN can manage the product catalog, while regular users with ROLE_USER can only manage their sales orders.
Input Validation: We use Jakarta Bean Validation (@Valid) on our DTOs in the controller layer. This ensures that all incoming data is well-formed and meets our business rules before it is processed, protecting against invalid data and potential injection attacks.
Validation Strategy:

Automated Integration Tests: Our test suite includes tests that verify our security rules. For example, we have tests that confirm an unauthenticated user receives a 401 Unauthorized error, and tests that ensure a user with ROLE_USER receives a 403 Forbidden error when trying to access an admin-only endpoint. We use @WithMockUser to simulate requests from users with different roles.
Vulnerability Scanning: In a CI/CD pipeline, we would integrate automated security scanning tools (like OWASP ZAP or Snyk) to scan the application and its dependencies for known vulnerabilities.
4. How would you ensure data integrity in concurrent scenarios?
To handle scenarios where multiple users might try to modify the same piece of data at the same time (a "race condition"), we implemented Optimistic Locking.

How it Works: The Product entity has a @Version field. When a user reads a product to update it, they also get its current version number. When they submit their update, the application checks if the version number in the database is still the same.
If it is, the update proceeds, and the version number is incremented.
If it's not (meaning another user updated it in the meantime), the update is rejected with an HTTP 409 Conflict error. This prevents the first user's changes from being silently overwritten.
This approach is highly effective for web applications where data conflicts are relatively infrequent, as it avoids the performance overhead of pessimistic locking (locking the database row).

5. How would you automate deployment and database migrations?
Automation is key for reliable and frequent releases. Our strategy involves a CI/CD pipeline and a database migration tool.

Automated Deployment (CI/CD):

Trigger: The pipeline (e.g., using GitHub Actions) is automatically triggered on every push to the main branch.
Test: It checks out the code and runs all unit and integration tests (./mvnw test). This is a critical quality gate.
Build: If tests pass, it packages the application into a runnable JAR file (./mvnw clean package).
Deploy: The pipeline then securely deploys this JAR file to the production environment, sets the necessary environment variables (like SPRING_PROFILES_ACTIVE=production and database credentials), and starts the application.
Automated Database Migrations:

Tool: While not yet implemented, the recommended approach is to use a tool like Flyway or Liquibase.
Process: Database changes are written as versioned SQL scripts (e.g., V1__create_tables.sql, V2__add_product_version.sql) and stored in the project's source code. When the application starts, Flyway automatically checks the database to see which scripts have already been applied and runs only the new ones. This ensures the database schema is always in sync with the application code, across all environments, in a reliable and repeatable way. For production, ddl-auto is set to validate to prevent Hibernate from making any unexpected changes.
6. What tools would you use for monitoring and logging?
Effective monitoring and logging are essential for maintaining a healthy application in production.

Monitoring:

Metrics Collection: We use Spring Boot Actuator to expose detailed application metrics (like JVM memory, CPU usage, and HTTP request latencies) via a /actuator/prometheus endpoint.
Metrics Storage & Visualization: We would set up a Prometheus server to periodically "scrape" (collect) these metrics. The data would then be visualized in Grafana dashboards, giving us real-time insight into the application's performance and health. We would also configure alerts in Grafana to notify the team of any issues (e.g., high error rates or low memory).
Logging:

Log Generation: The application uses SLF4J for structured logging, which is the standard for modern Java applications.
Centralized Logging: In a distributed environment with multiple application instances, it's impractical to check log files on individual servers. We would configure the application to send its logs to a centralized logging platform like the ELK Stack (Elasticsearch, Logstash, Kibana) or Splunk. This allows us to search, analyze, and visualize logs from all instances in one place, making it much easier to troubleshoot issues.
7. What challenges did you encounter and how did you solve them?
One of the key challenges was ensuring efficient data retrieval and avoiding performance bottlenecks, particularly the N+1 query problem.

The Problem: When fetching a list of sales orders, a naive implementation would first run one query to get the orders, and then N additional queries to get the associated items for each of the N orders. This is extremely inefficient and scales very poorly.
The Solution: We solved this by using a JPA @EntityGraph in our SalesOrderRepository. This feature allows us to tell Spring Data JPA exactly which related entities (in this case, the orderItems) we want to load along with the parent SalesOrder. By defining this entity graph, we ensure that all the required data is fetched in a single, optimized SQL query with a JOIN, completely eliminating the N+1 problem and significantly improving performance.
