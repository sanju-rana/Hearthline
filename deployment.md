# Deployment

This guide runs Hearthline on a developer machine. Demo passwords below are fictional and exist only in the `dev` profile.

## 1. Prerequisites

- JDK 21 (Temurin or another build). Java 8 is not enough.
- Maven 3.9 or newer.
- Node.js 22 and npm 10.
- PostgreSQL 16, or Docker Desktop with the Compose plugin.
- Git.

Confirm the JDK:

```bash
java -version
```

The output must say version 21. If `java -version` prints 1.8, set `JAVA_HOME` to the JDK 21 directory and put `%JAVA_HOME%\bin` ahead of the Java 8 path.

## 2. Clone

```bash
git clone <your-public-repository-url>
cd Isocode
```

## 3. Environment

Copy the example file and change the JWT secret before any shared deployment:

```bash
copy .env.example .env
```

Variable names:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET` (at least 32 characters)
- `FRONTEND_URL`
- `APP_CORS_ALLOWED_ORIGINS`
- `SPRING_PROFILES_ACTIVE`

There is no payment-provider key. Checkout uses the in-process demo gateway.

The `dev` profile has local defaults so the API can start against a database named `hearthline` without a filled `.env`. The `prod` profile requires the datasource variables and `JWT_SECRET`, and it does not load demo users.

## 4. Database

With Docker:

```bash
docker compose up -d postgres
```

Without Docker, create the database yourself:

```sql
CREATE USER hearthline WITH PASSWORD 'hearthline';
CREATE DATABASE hearthline OWNER hearthline;
```

Flyway applies `backend/src/main/resources/db/migration` when the API starts. Do not rely on Hibernate to create tables. `spring.jpa.hibernate.ddl-auto` is `validate`.

Demo dishes, branches, and users are inserted by `DevDataSeeder` only when the active profile is `dev` and the admin user is missing. The `test` and `prod` profiles do not run that seeder.

## 5. Backend

```bash
cd backend
mvn test
mvn spring-boot:run
```

The API listens on http://localhost:8080. Health: http://localhost:8080/actuator/health.

`mvn spring-boot:run` uses the default `dev` profile from `application.yml`.

## 6. Frontend

In another terminal:

```bash
cd frontend
npm install
npm test
npm run dev
```

Open http://localhost:5173. Vite proxies `/api` and `/ws` to port 8080, so the browser uses one origin and the HTTP-only cookie is stored.

Production build:

```bash
npm run build
```

The static files are written to `frontend/dist`.

## 7. Docker Compose

Start Docker Desktop first. Then, from the project root:

```bash
docker compose up --build
```

- PostgreSQL: localhost:5432
- API: http://localhost:8080
- Website, including proxied API and WebSocket: http://localhost:8088

Compose waits until Postgres accepts connections, then waits until the API health check passes, then starts the website. The first backend start can take a minute while Maven builds the image and Flyway migrates.

Stop it:

```bash
docker compose down
```

`docker compose down -v` also deletes the database volume.

## 8. Production notes

1. Set `SPRING_PROFILES_ACTIVE=prod`.
2. Provide `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `JWT_SECRET` from the host environment. Do not commit those values.
3. Serve the frontend and the API on one HTTPS origin, or keep the nginx proxy from `frontend/nginx.conf`. The cookie is `Secure` in the prod profile.
4. Do not run the `dev` profile on a public server. It creates known demo passwords.
5. Put a real `PaymentGateway` implementation behind the existing interface before taking real money. The current gateway is demo-only.

## 9. Migrations

Flyway runs on startup. To see what it will do without starting the web server:

```bash
cd backend
mvn -Dflyway.url=jdbc:postgresql://localhost:5432/hearthline -Dflyway.user=hearthline -Dflyway.password=hearthline flyway:info
```

The Flyway Maven plugin is not added on purpose. The application runs the migration itself. The command above works only if you add the plugin. The normal path is simply starting the API.

## 10. Demo logins

| Role | Email | Password | What to open |
| --- | --- | --- | --- |
| Customer | customer@hearthline.test | Customer@123 | http://localhost:5173 |
| Kitchen, Indiranagar | kitchen.indiranagar@hearthline.test | Kitchen@123 | http://localhost:5173/kitchen |
| Kitchen, Bandra | kitchen.bandra@hearthline.test | Kitchen@123 | http://localhost:5173/kitchen |
| Kitchen, Delhi | kitchen.delhi@hearthline.test | Kitchen@123 | http://localhost:5173/kitchen |
| Admin | admin@hearthline.test | Admin@123 | http://localhost:5173/admin |

## 11. Customer ordering test

1. Register a new customer, or sign in as `customer@hearthline.test`.
2. Open Restaurants and choose Hearthline Indiranagar.
3. Search or filter the menu and open a dish.
4. Add it to the cart. Change the quantity. Confirm the tax line.
5. Open checkout, choose pickup or delivery, and press **Pay with demo checkout**.
6. The confirmation page shows `HL-...`, payment provider `DEMO`, and status `PLACED`.
7. Optional: press **Simulate declined payment** and confirm no new order appears in My Orders.

## 12. Kitchen test

1. Sign in as `kitchen.indiranagar@hearthline.test` in a second browser profile.
2. Leave the kitchen board open on New.
3. Place an Indiranagar order as the customer.
4. The ticket should appear without a manual refresh. If the socket drops, the page says it is reconnecting and reloads from the API.
5. Accept, start preparing, mark ready, then complete.
6. Try to jump from Placed straight to Completed. The API rejects it.
7. Sign in as the Bandra kitchen and confirm the Indiranagar ticket is not listed.
8. On the customer tracking page, the status history matches the kitchen moves.

## 13. Common problems

| Symptom | What to check |
| --- | --- |
| `java` version is 1.8 | `JAVA_HOME` must point at JDK 21, and that `bin` directory must come first on `PATH`. |
| Port 5432 already in use | Stop the other Postgres, or change the host port in `docker-compose.yml` and the JDBC URL. |
| API exits on schema validation | The database was created by an older experiment. Drop the `hearthline` database, or run `docker compose down -v`, and start again. |
| Cookie login works in Postman but not the browser | Use the Vite URL (5173) or the Compose URL (8088). Do not open the React app from a different origin without the proxy. |
| Kitchen board stays empty | The signed-in kitchen user is assigned to a different branch from the order. |
| `docker` cannot find the engine | Start Docker Desktop and wait until it reports it is running. |
| Demo users are missing | The API is on the `prod` or `test` profile. Use `dev`. |
