# Formuvia – Backend

REST API for Formuvia, built with Java 21 and Jersey (JAX-RS), packaged as a WAR and backed by PostgreSQL.

The web client lives in a separate repository: **FormuviaFront** (Angular).

## Features

- Login with cookie-based sessions (HTTP-only access token, automatically refreshed)
- Role-based access (`admin`, `user`)
- User administration (list with paging, filtering and sorting; create, edit, delete)
- Change own password
- Multilingual texts: English (`en-US`), Serbian Latin (`sr-Latn-RS`), Serbian Cyrillic (`sr-RS`)
- Database schema is created and updated automatically on startup – no manual migrations
- Scheduled jobs (Quartz), e.g. cleanup of expired sessions
- OpenAPI / Swagger UI documentation

## Requirements

| Tool | Version |
|---|---|
| Java (JDK) | 21 |
| Maven | 3.9+ |
| PostgreSQL | 14+ |
| Servlet container | Jakarta Servlet 6.1 (e.g. Apache Tomcat 11) |

## Installation

### 1. Create the database

```sql
CREATE USER formuvia WITH PASSWORD 'change-me';
CREATE DATABASE formuvia OWNER formuvia;
```

Tables, constraints, indexes and the default admin user are created automatically the first time the application starts.

### 2. Configure

Default settings are in `resources/application.properties`. Do not put real passwords there – override values in one of these ways:

- **External properties file** – set `custom.properties.path` to the path of a file containing only the keys you want to override.
- **JVM system properties** – any key can be overridden with `-D<key>=<value>`, e.g. in Tomcat's `bin/setenv.sh`:

  ```sh
  CATALINA_OPTS="$CATALINA_OPTS -Ddatabase.password=secret -Dadmin.default.password=secret"
  ```

Most important settings:

| Key | Description | Default |
|---|---|---|
| `database.url` | JDBC URL | `jdbc:postgresql://localhost:5432/formuvia` |
| `database.username` / `database.password` | Database credentials | `formuvia` / – |
| `database.num.connections` | Connection pool size | `20` |
| `admin.default.password` | Password of the `admin` user created on first start | – |
| `default.language` | `en-US`, `sr-Latn-RS` or `sr-RS` | `en-US` |
| `cors.url` | Origin of the web client | `http://localhost:4200` |
| `cookie.secure` | Send cookies only over HTTPS | `true` |
| `cookie.same.site` | `Strict`, `Lax` or `None` | `Strict` |
| `cookie.access.token.duration.minutes` | Access token lifetime | `10` |
| `cookie.refresh.token.duration.minutes` | Session lifetime | `10080` (7 days) |
| `forward.ip-adress.header` | Header with the client IP when behind a reverse proxy | `X-Real-IP` |

Logs are written to the console and to `~/.logs/formuvia.log`. Change the location with `-Dlog.path=/path/to/formuvia.log`.

### 3. Build

```sh
mvn clean package
```

The result is `target/Formuvia.war`.

### 4. Deploy

Copy `Formuvia.war` to Tomcat's `webapps/` directory and start Tomcat. The application is available at:

- API: `http://localhost:8080/Formuvia/api/...`
- Swagger UI: `http://localhost:8080/Formuvia/swagger-ui/index.html`
- OpenAPI spec: `http://localhost:8080/Formuvia/api/openapi.json`

## First login

Log in with username **`admin`** and the password set in `admin.default.password`. Change this password immediately after the first login (in the web client: click your name in the sidebar).

## Production notes

- Serve the application over **HTTPS** (`cookie.secure=true`).
- With `cookie.same.site=Strict` the web client and the API must be on the same site (e.g. `app.example.com` and `api.example.com`, or both behind one reverse proxy). Set `cors.url` to the exact origin of the web client.
- Put secrets in an external properties file or JVM properties, never in the repository.

## License

MIT – see [LICENSE](LICENSE).
