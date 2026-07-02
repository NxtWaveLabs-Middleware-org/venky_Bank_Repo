# NextGenBank — Phase 1 (Sessions 1–12)

A minimal, real banking web app — login, view balances, transfer funds — built
to be deployed on WebSphere Application Server (Traditional) against a
PostgreSQL backend. This is the Month 1 lab codebase from your training plan.

## What's included

```
nextgenbank/
├── pom.xml                         # Maven WAR build
├── src/main/java/com/nextgenbank/
│   ├── model/                      # Customer, Account, Transaction
│   ├── dao/                        # CustomerDAO (auth), AccountDAO (balances/transfer)
│   ├── servlet/                    # Login, Logout, Dashboard, Transfer, Health
│   └── util/                       # JNDI DataSource lookup, password hashing
├── src/main/webapp/
│   ├── login.jsp
│   ├── dashboard.jsp
│   └── WEB-INF/web.xml             # servlet mappings + resource-ref
├── db/
│   ├── schema.sql                  # PostgreSQL DDL
│   └── seed.sql                    # 2 test customers, 3 accounts
└── wsadmin/
    ├── create_datasource.py        # Jython: creates JDBC provider + DataSource
    └── deploy_app.py               # Jython: installs/starts the WAR
```

## 1. Set up PostgreSQL

```bash
createdb nextgenbank
psql -d nextgenbank -f db/schema.sql
psql -d nextgenbank -f db/seed.sql
```

Test login credentials seeded: **jsmith / Passw0rd!** and **amiller / Passw0rd!**

Create a dedicated app user instead of using postgres superuser, matching what
`create_datasource.py` expects:

```sql
CREATE USER nextgenbank_app WITH PASSWORD 'changeit';
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO nextgenbank_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO nextgenbank_app;
```

## 2. Build the WAR

```bash
mvn clean package
# produces target/nextgenbank.war
```

## 3. Get the PostgreSQL JDBC driver onto the WAS node

Download `postgresql-42.7.x.jar` from https://jdbc.postgresql.org/download/
and copy it to a path on the WAS node, e.g.
`/opt/IBM/WebSphere/jdbcdrivers/postgresql.jar` (this matches `DRIVER_PATH`
in `create_datasource.py` — adjust if yours differs).

## 4. Create the JDBC Provider + DataSource

**Option A — Admin Console (do this once manually so you feel the UI, per Session 7):**
Resources → JDBC → JDBC Providers → New → PostgreSQL → then create a
DataSource under it named `NextGenBankDS`, JNDI name `jdbc/nextgenbankDS`,
and fill in host/port/db/user/password as custom properties. Test the
connection before moving on.

**Option B — wsadmin (do this for Session 28's automation lab):**

```bash
wsadmin.sh -lang jython -f wsadmin/create_datasource.py <nodeName> <serverName>
```

## 5. Deploy the app

**Option A — Admin Console:** Applications → New Application → Install and
walk through the wizard, mapping the module to your server and binding
`jdbc/nextgenbankDS`.

**Option B — wsadmin:**

```bash
wsadmin.sh -lang jython -f wsadmin/deploy_app.py $(pwd)/target/nextgenbank.war <nodeName> <serverName>
```

## 6. Test it

```
http://<host>:<port>/nextgenbank/login.jsp
http://<host>:<port>/nextgenbank/api/health
```

Log in as `jsmith` / `Passw0rd!`, view balances, then transfer money between
account IDs 1 and 2 (John Smith's checking → savings) and confirm the balance
updates and a row appears in `transactions`.

## Where this goes next

- **Session 6:** intentionally add a conflicting JAR to reproduce a
  classloader issue — the app is small enough that the effect is easy to see.
- **Session 9:** break the `jdbc/nextgenbankDS` JNDI binding on purpose and
  practice diagnosing the `NameNotFoundException`.
- **Month 2:** the `AccountDAO.transfer()` local-transaction method gets
  reimplemented as a container-managed EJB method, and the transfer flow
  gets a JMS/MDB-driven async audit trail (Sessions 21–24, 27).
- **Month 3:** this same WAR is what you'll performance-tune, break (GC/heap/
  thread issues), and eventually migrate onto WebSphere Liberty (Session 44).

## Security review — fixed after initial build

A quick self-review caught several real gaps, now fixed:

- **Horizontal privilege escalation**: `TransferServlet` now calls
  `AccountDAO.isOwnedBy()` before allowing a transfer, so a logged-in user
  can no longer move funds out of an account they don't own just by
  guessing an ID.
- **CSRF**: a per-session token is issued at login and required on every
  transfer POST.
- **Session fixation**: the session is invalidated and recreated on
  successful login.
- **Direct JSP access**: `dashboard.jsp` moved to `WEB-INF/views/` (not
  web-accessible) and is only reachable via `DashboardServlet`'s forward.
- **Unhandled exception**: `TransferServlet` now catches
  `IllegalArgumentException` (zero/negative amount) instead of letting it
  surface as an HTTP 500.
- **Cookie hardening**: `HttpOnly` enabled in `web.xml`; flip `secure` to
  `true` once HTTPS is on (Session 15).
- **Generic error page** added so stack traces never reach the browser.

Also added: a **transaction history** view on the dashboard
(`AccountDAO.recentTransactions()`), since a banking app that can't show you
your own history isn't much of one.

Still open, intentionally deferred to later phases: unsalted password
hashing (fine for the lab; swap for bcrypt as a stretch exercise), no
transfer amount/daily limits, no rate limiting on login attempts.

## A note on the transfer logic

`AccountDAO.transfer()` uses `SELECT ... FOR UPDATE` plus a single JDBC
connection's local transaction — it's atomic, but only within Postgres. It's
intentionally *not* yet a "real" distributed transaction. Comparing this
version to the EJB/XA version you'll build in Month 2 is one of the strongest
concrete stories you can tell in a WebSphere interview.
