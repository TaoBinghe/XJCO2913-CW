# Green Go

Green Go is an e-scooter rental system built for the XJCO2913 Software Engineering project. It supports customer-facing rental workflows, an administrative dashboard, and a Spring Boot backend that manages users, scooters, stores, bookings, payments, feedback, revenue, and external service integrations.

The project is organized as a three-part application:

- `backend`: Spring Boot REST API, MySQL persistence, authentication, business rules, and packaged admin UI hosting.
- `front`: uni-app/Vue 3 customer application for H5 and WeChat Mini Program targets.
- `front_admin`: Vue 3/Vite/Element Plus admin dashboard.

## Team Members

- Binghe Tao - backend engineer
- Guangkai Shang - scrum master
- Zhengyu Hong - test engineer
- Letian Lin - frontend engineer
- Chenyang Jin - documentation engineer

## Screenshots

> Add final screenshots before submission.

- Customer home / scooter rental screen
- Store reservation screen
- Order detail / payment screen
- Admin dashboard
- Scooter or store management screen
- Revenue report screen

## Demo Link

> Add the deployed application URL, demo video, or presentation link here.

- Demo:
- Video:
- Admin UI:

## Features

### Customer Application

- User registration, login, logout, and JWT-based session handling.
- Store pickup booking flow with rental period selection, pickup deadline, scooter pickup, lock/unlock, return, renewal, cancellation, and settlement.
- Scan ride flow for nearby scooters, including start, lock/unlock, return, and location-aware return information.
- Scooter list and route lookup through map/location services.
- Personal order list and detailed order lifecycle view.
- Wallet balance, bank card binding, recharge, and payment support.
- Discounts based on customer type and payment rules.
- Feedback issue submission and personal feedback history.
- AI-assisted fault report chat for scooter or ride issues.

### Admin Dashboard

- Admin login for users with the `MANAGER` role.
- Dashboard summary for stores, fleet, pricing, guest bookings, feedback, and revenue.
- Scooter management, including list, create, update, delete, status, lock status, and location resolution.
- Store management with CRUD operations and map coordinates.
- User management and role/status visibility.
- Pricing plan management for rental periods and prices.
- Guest/unregistered customer booking creation.
- Feedback issue triage, high-priority issue view, status updates, and resolution notes.
- Weekly and daily revenue reporting.

### Backend

- REST APIs for customer and admin workflows.
- MySQL schema and seed data for stores, scooters, and pricing plans.
- MyBatis-Plus mapper layer and service-layer business logic.
- JWT token generation plus in-memory session invalidation.
- Request authentication interceptor and role checks for admin-only APIs.
- Wallet, payment, card, booking, pricing, feedback, and revenue services.
- Amap integration for route planning and address/location resolution.
- DashScope-compatible AI integration for the fault report assistant.
- Mail integration for booking confirmation messages.
- Dockerfile and cloud-hosting environment template for deployment.

## Tech Stack

### Backend

- Java 17
- Spring Boot 3.3.8
- Spring Web
- Spring Validation
- Spring Mail
- Spring Security Crypto
- MyBatis-Plus 3.5.15
- MySQL Connector/J
- Java JWT
- Lombok
- Maven Wrapper

### Customer Frontend

- uni-app
- Vue 3
- Vite
- WeChat Mini Program target
- H5 target
- Amap H5 map configuration

### Admin Frontend

- Vue 3
- Vite
- TypeScript
- Element Plus
- Vue Router
- Axios

## Project Structure

```text
Green-go/
├── backend/
│   ├── src/main/java/com/greengo/
│   │   ├── controller/        # REST controllers
│   │   ├── service/           # Business service interfaces
│   │   ├── service/impl/      # Business service implementations
│   │   ├── mapper/            # MyBatis-Plus mappers
│   │   ├── domain/            # Request/response/domain models
│   │   ├── config/            # Web, CORS, interceptor, and time config
│   │   └── utils/             # Auth, JWT, pricing, locking, and helper utilities
│   ├── src/main/resources/
│   │   ├── application.yml    # Runtime configuration with environment overrides
│   │   └── db/                # Schema and migration SQL files
│   ├── src/test/java/         # Backend unit and controller tests
│   ├── Dockerfile
│   └── pom.xml
├── front/
│   ├── src/pages/             # Customer app pages
│   ├── src/api/               # Customer API wrappers
│   ├── src/utils/             # Auth, request, booking, location, payment helpers
│   ├── src/pages.json         # uni-app route and tab configuration
│   └── package.json
├── front_admin/
│   ├── src/views/             # Admin dashboard views
│   ├── src/api/               # Admin API wrappers
│   ├── src/router/            # Admin routes and auth guard
│   ├── src/layout/            # Admin layout
│   └── package.json
└── postman/                   # API testing assets, if maintained by the team
```

## Local Setup

### Prerequisites

- Java 17
- MySQL 8.x or compatible MySQL server
- Node.js 20.19.0 or newer compatible version
- npm
- WeChat Developer Tools, if running the Mini Program target

### 1. Clone the Repository

```bash
git clone <repository-url>
cd Green-go
```

### 2. Create the Database

Create a MySQL database named `xjco2913`:

```sql
CREATE DATABASE xjco2913
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

Import the fresh schema:

```bash
mysql -u root -p xjco2913 < backend/src/main/resources/db/schema.sql
```

The `schema.sql` file creates the core tables and inserts initial stores, scooters, and pricing plans. The other SQL files under `backend/src/main/resources/db/` are migration scripts for upgrading an existing database.

### 3. Configure Environment Variables

The backend reads runtime configuration from environment variables. For local development, the defaults in `application.yml` can connect to:

- MySQL URL: `jdbc:mysql://127.0.0.1:3306/xjco2913`
- MySQL username: `root`
- MySQL password: `root`
- Backend port: `9090`

For a safer and more portable setup, configure these variables in your shell or IDE run configuration:

```env
PORT=9090
DB_URL=jdbc:mysql://127.0.0.1:3306/xjco2913?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8
DB_USERNAME=root
DB_PASSWORD=<your-local-password>
AMAP_WEB_SERVICE_KEY=<your-amap-web-service-key>
DASHSCOPE_API_KEY=<your-dashscope-api-key>
DASHSCOPE_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
DASHSCOPE_MODEL=qwen-plus
MAIL_HOST=<smtp-host>
MAIL_PORT=587
MAIL_USERNAME=<smtp-username>
MAIL_PASSWORD=<smtp-password>
MAIL_FROM=<from-address>
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
```

Do not commit real database passwords, map service keys, AI API keys, or mail credentials.

### 4. Run the Backend

From the `backend` directory:

```bash
cd backend
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The backend starts on `http://localhost:9090` by default.

Health check:

```bash
curl http://localhost:9090/healthz
```

### 5. Run the Customer Frontend

From the `front` directory:

```bash
cd front
npm install
npm run dev:h5
```

For WeChat Mini Program development:

```bash
cd front
npm install
npm run dev:mp-weixin
```

The customer app request layer supports two modes:

- WeChat Mini Program cloud container calls by default.
- HTTP requests when `VITE_MP_WEIXIN_REQUEST_MODE=http` and `VITE_API_BASE_URL` are configured.

Useful customer frontend environment variables:

```env
VITE_API_BASE_URL=http://localhost:9090
VITE_MP_WEIXIN_REQUEST_MODE=http
VITE_WX_CLOUD_ENV_ID=<wechat-cloud-env-id>
VITE_WX_CLOUD_SERVICE=green-go
```

### 6. Run the Admin Frontend

From the `front_admin` directory:

```bash
cd front_admin
npm install
npm run dev
```

The admin Vite dev server uses port `3000` and proxies `/admin/**` API requests to `http://localhost:9090`.

Open:

```text
http://localhost:3000
```

For production packaging, the admin frontend is built and copied into the backend JAR under `/admin-ui`.

## Common Run Commands

### Backend

```bash
cd backend
./mvnw test
./mvnw package
./mvnw -DskipTests package
./mvnw spring-boot:run
```

Windows PowerShell equivalents:

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd package
.\mvnw.cmd -DskipTests package
.\mvnw.cmd spring-boot:run
```

If you only want to build the backend without rebuilding the admin frontend through Maven:

```bash
./mvnw -Dskip.frontend=true package
```

### Customer Frontend

```bash
cd front
npm install
npm run dev:h5
npm run dev:mp-weixin
npm run build:h5
npm run build:mp-weixin
```

### Admin Frontend

```bash
cd front_admin
npm install
npm run dev
npm run build
npm run preview
```

## Main API Areas

Customer-facing APIs:

- `POST /user/register`
- `POST /user/login`
- `POST /user/logout`
- `GET /user/my-orders`
- `GET /store/list`
- `GET /store/{storeId}`
- `GET /scooter/list`
- `GET /scooter/route`
- `GET /booking`
- `POST /booking`
- `POST /booking/scan/start`
- `POST /booking/{bookingId}/pickup`
- `POST /booking/{bookingId}/lock`
- `POST /booking/{bookingId}/unlock`
- `POST /booking/{bookingId}/return`
- `POST /payment`
- `GET /wallet`
- `POST /wallet/cards`
- `POST /wallet/recharge`
- `POST /feedback/issues`
- `GET /feedback/issues/my`
- `POST /fault-report/chat`

Admin APIs:

- `POST /admin/login`
- `POST /admin/logout`
- `GET /admin/user/list`
- `GET /admin/scooter/list`
- `POST /admin/scooter/add`
- `POST /admin/scooter/update`
- `DELETE /admin/scooter/delete`
- `GET /admin/stores`
- `POST /admin/stores`
- `PUT /admin/stores/{id}`
- `DELETE /admin/stores/{id}`
- `GET /admin/pricing-plans`
- `POST /admin/pricing-plans`
- `PUT /admin/pricing-plans/{id}`
- `DELETE /admin/pricing-plans/{id}`
- `POST /admin/bookings/unregistered`
- `GET /admin/feedback/issues`
- `GET /admin/feedback/issues/high-priority`
- `PUT /admin/feedback/issues/{id}`
- `GET /admin/revenue/weekly`
- `GET /admin/revenue/daily`

Most protected APIs require an `Authorization` header containing the token returned by login.

## Testing

Recommended checks before submission:

```powershell
cd backend
.\mvnw.cmd test
```

```powershell
cd front_admin
npm run build
```

```powershell
cd front
npm run build:h5
```

Backend tests cover major service and controller behavior, including booking, scooter, payment, wallet/card security, feedback, revenue, route planning, email confirmation, and login interceptor behavior.

## Deployment Notes

The backend is the main deployable service. In the normal deployment flow, Maven builds `front_admin` and copies the built admin UI into the Spring Boot JAR under:

```text
static/admin-ui
```

After deployment, the admin UI is served from:

```text
/admin-ui
```

Build the packaged backend:

```powershell
cd backend
.\mvnw.cmd -DskipTests package
```

Expected JAR:

```text
backend/target/backend-0.0.1-SNAPSHOT.jar
```

The Docker image uses `backend/Dockerfile`, which runs the packaged JAR with Java 17. The container defaults to `PORT=80`, while local development defaults to backend port `9090`.

Cloud or container deployments should set at least:

```env
PORT=80
DB_URL=<cloud-mysql-jdbc-url>
DB_USERNAME=<db-username>
DB_PASSWORD=<db-password>
AMAP_WEB_SERVICE_KEY=<amap-web-service-key>
DASHSCOPE_API_KEY=<dashscope-api-key>
MAIL_HOST=<smtp-host>
MAIL_PORT=587
MAIL_USERNAME=<smtp-username>
MAIL_PASSWORD=<smtp-password>
MAIL_FROM=<from-address>
```

More detailed cloud database notes are available in `backend/CLOUD_HOSTING.md`.

## Security Notes

- Keep all real secrets in environment variables or deployment secret storage.
- Rotate any API key or password that has been committed, shared, or exposed.
- Admin APIs are intended for `MANAGER` users only.
- User and admin sessions use JWT tokens plus server-side session validation.
- Password and card password handling uses modern hashing support, with compatibility for older stored hashes where implemented.
- Do not expose production database ports or cloud MySQL instances without network allowlists or equivalent access controls.
