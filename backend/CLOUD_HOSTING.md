# Cloud Hosting Database Setup

This backend already reads its runtime database configuration from environment variables. No Java code change is required for cloud hosting.

## Deployment Mode

Use the prebuilt JAR deployment flow for cloud hosting:

1. Build the admin UI and backend locally:
   - `cd backend`
   - `.\mvnw.cmd "-Dmaven.repo.local=.m2\repository" -DskipTests package`
2. Confirm the packaged JAR exists:
   - `target/backend-0.0.1-SNAPSHOT.jar`
3. Upload the `backend/` directory to cloud hosting, not `front_admin/`.
4. Use [backend/Dockerfile](c:/Users/ASUS/Desktop/wechat/backend/Dockerfile:1) as the image build entry.

Why this works:

- The Maven package step already builds `front_admin` from `../front_admin`
- The admin UI is copied into the Spring Boot JAR under `static/admin-ui`
- [backend/Dockerfile](c:/Users/ASUS/Desktop/wechat/backend/Dockerfile:8) only needs the finished JAR and starts it with `java -jar`

Do not upload `front_admin/` by itself. It is not an independent cloud hosting service and does not need its own Dockerfile.

## Required Environment Variables

Set these variables on the `green-go` cloud hosting service before restarting the service:

```env
PORT=80
DB_URL=jdbc:mysql://<cloud-mysql-private-host>:3306/xjco2913?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
DB_USERNAME=<db-username>
DB_PASSWORD=<db-password>
AMAP_WEB_SERVICE_KEY=<amap-web-service-key>
```

Use the values in [cloud-hosting.env.example](c:/Users/ASUS/Desktop/wechat/backend/cloud-hosting.env.example:1) as the template. Do not leave `DB_URL` on the default `localhost:3306` value when the service runs in cloud hosting.

## Database Initialization

If the cloud MySQL database is empty, import these SQL files in order:

1. [schema.sql](c:/Users/ASUS/Desktop/wechat/backend/src/main/resources/db/schema.sql:1)
2. [scooter_location_migration.sql](c:/Users/ASUS/Desktop/wechat/backend/src/main/resources/db/scooter_location_migration.sql:1)

The database name expected by the application is `xjco2913`.

## Verification

After updating the environment variables and importing the schema if needed:

1. Restart or redeploy the `green-go` service.
2. Trigger `POST /user/login` or `GET /scooter/list` from the mini program.
3. Check the cloud hosting logs.

Successful behavior:

- No more `Connection refused` from `HikariPool`
- No JDBC connection stack traces during login or scooter queries
- Login and scooter list requests return business responses instead of 500 errors

If errors remain:

- `Connection refused` usually means the host or port is wrong, or the MySQL instance is not reachable from cloud hosting.
- `Communications link failure` or timeouts usually means network routing or VPC access is wrong.
- `Access denied` usually means the database username or password is wrong.
- `Unknown database` means the `xjco2913` database has not been created yet.
