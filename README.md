# Formuvia – Backend

REST API for Formuvia, built with Java 25 and Jersey (JAX-RS), packaged as a WAR and backed by PostgreSQL.

The web client lives in a separate repository: **FormuviaFront** (Angular).

📖 **[User guide](https://natasablagojevic88.github.io/FormuviaFront/)** – how to use Formuvia, in English and Serbian.

## Features

- Login with cookie-based sessions (HTTP-only access token, automatically refreshed)
- Role-based access (`admin`, `user`), with roles assigned per user
- User administration (list with paging, filtering and sorting; create, edit, delete)
- Change own password
- Export of table data to Excel (`.xlsx`)
- Excel entry template for a table defined through the model: translated column labels (required ones in red), per-type format, width and input check, drop-downs for codebooks, and the mapping of columns to fields - all on hidden sheets of the same file
- Import of a filled-in template: every row goes through the same checks as an entry on the form, in one transaction - the first row that cannot be accepted stops the import and the answer says which row it was
- Model: menus and tables defined through the application; a new table is created in the database, with separate roles for viewing, adding, editing and deleting its data
- Data in those tables: listing with paging, filtering and sorting, the form definition for one record, the history of a record, and adding, changing and deleting a record, each guarded by the role the model defines for that action
- Form designer: the fields of a table are defined through the application – data type, position in the entry dialog, default value and list of values (each given as a single `SELECT`, validated before it is saved), and a link to another table used as a codebook, which creates the foreign key in the database; deleting a field drops its column
- Values worked out by the database: a field of a form can carry the name of a database function that takes the record as `json` and returns `json`; when the client reports that the field was changed, the record is handed to that function and the record it returns is sent back, so one field can fill in or correct the others. The functions a field may be given are read from `information_schema.routines` of the current schema
- Procedures on a table: a table of the model can be given procedures of the database, each under a name of its own (`model_procedure`). The client runs one over a single record; the server checks the name against `information_schema.routines`, calls it with the identifier of that record and answers with the record as it stands afterwards. Running one asks for the role that may change the record
- Triggers on a table: a table of the model can be given triggers of the database (`model_trigger`), each one a function returning `trigger`, set to run before or after an insert, an update or a delete, with a `WHEN` condition of its own. Saving the entry creates the trigger in the database and deleting it drops it; the function is checked against `information_schema.routines` beforehand. Only an administrator sets them, and nothing in the API runs them - the database does, on every write of a row
- Reports on a table: a table of the model can be given reports of JasperReports (`model_jasper`), each one a report compiled by Jaspersoft Studio and stored as a file of its own. The uploaded file is read with `JRLoader` before it is accepted and has to be a compiled report that takes a parameter `id` of type `java.util.UUID`, the record the report is made over; deleting the entry deletes the stored file with it. Only an administrator sets them
- File upload: a file of a record is stored on the file system under `files.path`, in a folder of the date it arrived, and the record keeps the identifier it was stored under (column type `FILE`)
- Mail servers: the servers the application sends mail through are written down in the application itself (`smtp`), not in `application.properties` - a name of its own, the address mail is sent from, the server and its port, the protection of the connection (none, STARTTLS or SSL/TLS) and, when the server asks for one, the user it signs in as. The password is stored and never sent back to the client. Only an administrator sets them, and each server can be tried out with a test mail. Every mail the application sends is written down in `mail_log` - the server it went through, the recipient, the subject, the text, whether it left and, when it did not, why
- Multilingual texts: English (`en-US`), Serbian Latin (`sr-Latn-RS`), Serbian Cyrillic (`sr-RS`)
- Database schema is created and updated automatically on startup – no manual migrations
- Scheduled jobs (Quartz): the jobs of the application itself - listening to the database, checking that connection and cleaning up expired sessions - and the jobs a report is given through the application (`job_item`, `job_cron`). A job of a report says what is to be done with it (send it by mail through one of the mail servers, write it into a file on the server as Excel or CSV, or only run it) and when: for every unit of time - minutes, hours, day of the month, month, day of the week, year - either any value, a step (`*/n`) or a list of values, out of which the server builds the cron expression of the trigger. A job is kept in the database of the scheduler (`quartz.database.start`), so it stands there after a restart as well; a job that is switched off is unscheduled without being deleted, a deleted one is taken out of the scheduler together with its row, and the name of a job cannot be changed, because the scheduler holds the job under it. When such a job fires the server does the work itself: it sends the report by mail - in the mail itself as an HTML table, or along with it as a CSV, an Excel file or a Jasper PDF - writes that file into the folder of the job, or only runs the report over the data. Every run is written down (`job_log`) with its outcome and, if it failed, the error, which is also sent to the address for errors the job carries, through the mail server of that job - so a job that carries such an address needs a mail server as well, whatever else it does. The jobs of the scheduler are read back through `/api/job/list` and `/api/job/{id}`, each with the state of its last run, `/api/job/logs/{id}` gives the runs of one job as a paged table, and `/api/job/execute/{id}` runs one of them at once, beside its timetable; a job is shown only to a user holding the role it is tied to, and a job without a role to the admin role alone
- OpenAPI / Swagger UI documentation

## Requirements

| Tool | Version |
|---|---|
| Java (JDK) | 25 |
| Maven | 3.9+ |
| PostgreSQL | 18+ (uses the built-in `uuidv7()` function) |
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
- **Environment variables** – any key can be overridden by a variable of the same name, e.g. `files.path=/data/formuvia`. This is the way to configure the application in a container:

  ```sh
  docker run -e "database.password=secret" -e "files.path=/data/formuvia" ...
  ```

  A name with dots cannot be exported from a shell script (`export database.password=…` is not a valid identifier there), so in `setenv.sh` use a system property instead.
- **JVM system properties** – any key can be overridden with `-D<key>=<value>`, e.g. in Tomcat's `bin/setenv.sh`:

  ```sh
  CATALINA_OPTS="$CATALINA_OPTS -Ddatabase.password=secret -Dadmin.default.password=secret"
  ```

The order is: `application.properties` from the WAR, then the external file, then environment variables, then system properties – so `-D` always wins. Only a key that is already known – from `application.properties` or from the external file – can be overridden this way.

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
| `files.path` | Folder where uploaded files are stored; `${user.home}` is replaced with the home of the user running the server | `${user.home}/.formuvia-files` |
| `test.mail.subject.text` | Subject of the test mail sent from *Administration / Mail servers* | `Formuvia - mail working` |
| `csv.date.format` | How a date is written into a CSV file made by a job | `dd.MM.yyyy` |
| `csv.time.format` | How a time is written into that file | `HH:mm` |
| `csv.date-time.format` | How a date and time together are written into it | `dd.MM.yyyy HH:mm` |
| `quartz.database.start` | Start the scheduler that keeps its jobs in the database; without it the jobs of a report are written down but never scheduled | `true` |
| `database.listen.cron` | How often the changes announced by the database are read | `*/2 * * * * ?` |
| `database.listen.connection.check.cron` | How often that connection is checked | `0 */1 * * * ?` |
| `token.clear.cron` | How often expired sessions are cleaned up | `0 0 */1 * * ?` |

Logs are written to the console and to `~/.logs/formuvia.log`. Change the location with `-Dlog.path=/path/to/formuvia.log`.

Settings of the report engine are read separately, from `resources/jasperreports.properties`, which the build puts at the root of the classpath (`WEB-INF/classes`). `net.sf.jasperreports.default.font.name` there decides which font a report uses when it names none of its own. The family it names has to be registered as a font extension on the classpath, otherwise filling a report fails with `JRFontNotFoundException`; `jasperreports-fonts` brings the DejaVu families. A PDF keeps the Serbian letters only with a font that is embedded in it, which is what a font extension does, so the built-in `SansSerif` is not enough.

### 3. Build

```sh
mvn clean package
```

The result is `target/Formuvia.war`, the API on its own; the web client is then served from somewhere else and let in through `cors.url`.

To put the client inside the same WAR, as one deployment, run Ant instead (`build.xml`, in this folder, with the `FormuviaFront` repository beside it):

```sh
ant
```

It builds the client first (`FormuviaFront/buildForWar.xml`: `npm install` and `npm run build`), then runs `mvn clean package` here, and then adds `FormuviaFront/dist/FormuviaFront/browser/` into `target/Formuvia.war` beside `WEB-INF`. The client then calls the API at the address it was itself served from, so there is no CORS and nothing to configure on either side.

### 4. Deploy

Copy `Formuvia.war` to Tomcat's `webapps/` directory and start Tomcat. The application is available at:

- API: `http://localhost:8080/Formuvia/api/...`
- Swagger UI: `http://localhost:8080/Formuvia/swagger-ui/index.html`
- OpenAPI spec: `http://localhost:8080/Formuvia/api/openapi.json`
- OpenAPI spec of one module: `http://localhost:8080/Formuvia/api/openapi-module.json?module=report`

Both the endpoints and the schemas are named after the module they belong to - the tags read `Report / Reports`, `Model / Columns`, and a schema is `Report.ReportDTO` - and the document is written out sorted, so each module stands together in Swagger UI. The picker in the header of Swagger UI holds the whole application (*All*) and one definition per module; the modules are read from the packages of the controllers, so a new module appears there by itself.

## First login

Log in with username **`admin`** and the password set in `admin.default.password`. Change this password immediately after the first login (in the web client: click your name in the sidebar).

## Production notes

- Serve the application over **HTTPS** (`cookie.secure=true`).
- With `cookie.same.site=Strict` the web client and the API must be on the same site (e.g. `app.example.com` and `api.example.com`, or both behind one reverse proxy). Set `cors.url` to the exact origin of the web client.
- Put secrets in an external properties file or JVM properties, never in the repository.
- Turn on **gzip** for the answers of the API. JSON goes down to roughly a tenth of its size (`openapi.json`: 149 KB -> 17 KB), and a page of a table behaves the same way. It is a setting of the deployment, not of the application, and it belongs wherever the traffic enters:
  - behind an ingress or a reverse proxy, there: `gzip on` in nginx, or a `compress` middleware in Traefik;
  - on a plain Tomcat, on the `<Connector>`: `compression="on" compressionMinSize="1024"` (`application/json` is in Tomcat's own list of compressible types).

  Nothing changes in the code, and the browser unpacks the answer by itself. Leave the Excel export and uploaded files out of the list of compressed media types - they are packed already.

## License

MIT – see [LICENSE](LICENSE).
