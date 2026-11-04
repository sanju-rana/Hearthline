# Hearthline

Hearthline is a direct-ordering website for a fictional restaurant chain, with a separate kitchen display and an admin console. A customer chooses one branch, builds a cart, pays through a clearly labeled demo checkout, and follows the order while the kitchen moves it through a fixed status workflow.

PostgreSQL is the source of truth. WebSocket messages are sent only after the order transaction commits. If the socket drops, the kitchen board and the tracking page load the order again through the REST API.

## Features

- Customer registration, login, and logout with a BCrypt password hash and an HTTP-only JWT cookie.
- Roles: customer, kitchen staff, and restaurant admin. Checks happen on the server.
- Multiple branches. A cart and an order belong to one branch.
- Menu search, category filter, price sort, availability, and popular dishes, loaded from PostgreSQL.
- Server-side cart, tax, delivery fee, and order total. The browser cannot submit a price.
- Idempotency key so a double submit does not create two orders.
- Demo payment with success, decline, and pending results. No card data is stored.
- Kitchen board for one branch, with legal status moves only.
- Live updates over STOMP, plus a REST fallback.
- Admin overview counted from the database, plus branch, menu, and order management.

## Tech stack

- Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, Flyway, WebSocket STOMP, PostgreSQL
- React 18, TypeScript, Vite, Tailwind CSS, React Router, Axios, React Hook Form, Zod
- Docker Compose for PostgreSQL, the API, and the web app

## Architecture

```text
React (customer, kitchen, admin)
        |  REST, cookie
        v
Spring Boot controllers
        |
        v
Services (pricing, status rules, demo payment)
        |
        v
PostgreSQL  <---- Flyway migrations
        |
        after commit
        v
STOMP /topic/kitchen/{branchId}
STOMP /topic/orders/{orderId}
```

The important classes are explained in `TECHNICAL_DESIGN_DOCUMENT.md`.

## Folder structure

```text
backend/     Spring Boot API
frontend/    React application
docker-compose.yml
deployment.md
TECHNICAL_DESIGN_DOCUMENT.md
```

## Run it locally

Install JDK 21, Maven, Node.js 22, and Docker. Full commands, environment variable names, and the demo logins are in [deployment.md](deployment.md).

Short version with Docker, after Docker Desktop is running:

```bash
docker compose up --build
```

Open http://localhost:8088.

## Demo logins

These accounts are created only when the `dev` profile is active. They are fictional.

| Role | Email | Password |
| --- | --- | --- |
| Customer | customer@hearthline.test | Customer@123 |
| Kitchen, Indiranagar | kitchen.indiranagar@hearthline.test | Kitchen@123 |
| Kitchen, Bandra | kitchen.bandra@hearthline.test | Kitchen@123 |
| Kitchen, Delhi | kitchen.delhi@hearthline.test | Kitchen@123 |
| Admin | admin@hearthline.test | Admin@123 |

The Delhi branch is active but not accepting orders, so checkout is blocked there. Indiranagar does not sell Double Patty. Bandra does not sell Coastal Fish Curry or Mushroom Truffle.

## Tests

```bash
cd backend
mvn test

cd ../frontend
npm test
npm run build
```

## API

Public catalog routes live under `/api/restaurants`, `/api/branches`, and `/api/menu`. Customers use `/api/cart` and `/api/orders`. Kitchen staff use `/api/kitchen/orders`. Admins use `/api/admin`. Every JSON body uses `success`, `data`, `message`, and `timestamp`. The endpoint list is in the technical design document.

## Deployment

Local Docker Compose is the supported run path. A public live URL has not been published. The GitHub repository URL should be added here after the project is pushed.
