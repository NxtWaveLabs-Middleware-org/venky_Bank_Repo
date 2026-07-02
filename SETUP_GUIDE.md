# NextGenBank — Phase 1 Setup Guide

End-to-end, step-by-step instructions to get NextGenBank running on
WebSphere Application Server (Traditional) against PostgreSQL, from a bare
machine to a working login/transfer flow. Written for someone doing this for
the first time — every command is copy-pasteable.

---

## 0. What you need before you start

| Tool | Version | Why |
|---|---|---|
| JDK | 8 (matches WAS 9.0's supported Java) | compile the app |
| Apache Maven | 3.6+ | build the WAR |
| PostgreSQL | 12+ | the database backend |
| IBM WebSphere Application Server ND | 9.0.5.x | the app server |
| IBM Installation Manager | latest | installs WAS |
| PostgreSQL JDBC driver | 42.7.x | lets WAS talk to Postgres |

If you don't have a WAS entitlement, IBM offers a free **WAS Liberty**
download and time-limited **WAS ND trial** — either is fine for this lab;
these instructions assume WAS ND (Traditional), which is what your training
plan's Month 1–3 sessions are built around.

---

## 1. Install and verify prerequisites

```bash
java -version        # expect 1.8.x
mvn -version          # expect 3.6+
psql --version         # expect 12+
```

If PostgreSQL isn't installed:

```bash
# Ubuntu/Debian
sudo apt update && sudo apt install -y postgresql postgresql-contrib
sudo systemctl enable --now postgresql
```

---

## 2. Install WebSphere Application Server ND

1. Download **IBM Installation Manager** and **WAS ND 9.0.5** installation
   files from IBM Fix Central / Passport Advantage.
2. Launch Installation Manager, point it at the WAS repository, and install
   WAS ND to (default) `/opt/IBM/WebSphere/AppServer`.
3. Create the two profiles you'll need — a Deployment Manager and an
   Application Server profile — using the Profile Management Tool or CLI:

```bash
cd /opt/IBM/WebSphere/AppServer/bin

./manageprofiles.sh -create \
  -profileName Dmgr01 \
  -profilePath /opt/IBM/WebSphere/AppServer/profiles/Dmgr01 \
  -templatePath /opt/IBM/WebSphere/AppServer/profileTemplates/management \
  -serverType DEPLOYMENT_MANAGER

./manageprofiles.sh -create \
  -profileName AppSrv01 \
  -profilePath /opt/IBM/WebSphere/AppServer/profiles/AppSrv01 \
  -templatePath /opt/IBM/WebSphere/AppServer/profileTemplates/default
```

4. Start the Dmgr, then federate the AppSrv node to it:

```bash
/opt/IBM/WebSphere/AppServer/profiles/Dmgr01/bin/startManager.sh

/opt/IBM/WebSphere/AppServer/profiles/AppSrv01/bin/addNode.sh <dmgr-host> <dmgr-SOAP-port>
# default SOAP port is 8879 unless you changed it
```

5. Start the node agent and the application server:

```bash
/opt/IBM/WebSphere/AppServer/profiles/AppSrv01/bin/startNode.sh
/opt/IBM/WebSphere/AppServer/profiles/AppSrv01/bin/startServer.sh server1
```

6. Confirm the Admin Console loads: `https://<host>:9043/ibm/console`
   (default secure admin port — check `SystemOut.log` under
   `Dmgr01/logs/dmgr/` if it doesn't come up).

---

## 3. Create the PostgreSQL database

```bash
sudo -u postgres psql
```

Inside the `psql` prompt:

```sql
CREATE DATABASE nextgenbank;
CREATE USER nextgenbank_app WITH PASSWORD 'changeit';
\c nextgenbank
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO nextgenbank_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO nextgenbank_app;
\q
```

Load the schema and seed data (from the project's `db/` folder):

```bash
psql -U postgres -d nextgenbank -f db/schema.sql
psql -U postgres -d nextgenbank -f db/seed.sql
```

Sanity check:

```bash
psql -U nextgenbank_app -d nextgenbank -h localhost -c "SELECT username, full_name FROM customers;"
```

You should see `jsmith` and `amiller`. Seeded login for both:
**password = `Passw0rd!`**

> If PostgreSQL rejects password auth, edit `pg_hba.conf` (commonly
> `/etc/postgresql/<version>/main/pg_hba.conf`) to use `md5` or `scram-sha-256`
> for local connections, then `sudo systemctl restart postgresql`.

---

## 4. Get the PostgreSQL JDBC driver onto the WAS node

```bash
mkdir -p /opt/IBM/WebSphere/jdbcdrivers
cd /opt/IBM/WebSphere/jdbcdrivers
curl -O https://jdbc.postgresql.org/download/postgresql-42.7.4.jar
mv postgresql-42.7.4.jar postgresql.jar
```

This path (`/opt/IBM/WebSphere/jdbcdrivers/postgresql.jar`) matches
`DRIVER_PATH` in `wsadmin/create_datasource.py`. If you put it elsewhere,
edit that variable before running the script.

---

## 5. Build the WAR

From the `nextgenbank/` project root:

```bash
mvn clean package
ls target/nextgenbank.war
```

If Maven fails on the `javax.servlet-api` dependency, double check your
`~/.m2` has internet access, or point it at IBM's Maven-compatible repo if
you're behind a proxy with restricted external access.

---

## 6. Create the JDBC Provider and DataSource

### Option A — Admin Console (do this manually the first time)

1. Admin Console → **Resources → JDBC → JDBC Providers** → scope to your
   server → **New**.
2. Database type: **User-defined**. Implementation class:
   `org.postgresql.ds.PGSimpleDataSource`. Classpath: the full path to
   `postgresql.jar` from Step 4.
3. Under that provider, **New Data Source**:
   - Name: `NextGenBankDS`
   - JNDI name: `jdbc/nextgenbankDS`
4. On the "Custom properties" page (or next screen), set:
   - `serverName` = `localhost` (or your DB host)
   - `portNumber` = `5432`
   - `databaseName` = `nextgenbank`
   - `user` = `nextgenbank_app`
   - `password` = `changeit`
5. Save, then click into the DataSource → **Test connection**. You should
   see a success message. If not, see Troubleshooting below.

### Option B — wsadmin (scripted, repeatable)

```bash
cd /opt/IBM/WebSphere/AppServer/profiles/Dmgr01/bin
./wsadmin.sh -lang jython \
  -f /path/to/nextgenbank/wsadmin/create_datasource.py AppSrv01 server1
```

(Edit the `DB_HOST`/`DB_USER`/`DB_PASSWORD` variables at the top of
`create_datasource.py` first if they differ from Step 3.)

---

## 7. Deploy the application

### Option A — Admin Console

1. **Applications → New Application → New Enterprise Application**.
2. Browse to `target/nextgenbank.war`, choose **Fast Path**.
3. Set context root: `/nextgenbank`.
4. On the resource-reference mapping step, map `jdbc/nextgenbankDS` to the
   DataSource you created in Step 6.
5. Finish, then **Save** to the master configuration.
6. **Applications → All Applications → NextGenBank → Start**.

### Option B — wsadmin

```bash
cd /opt/IBM/WebSphere/AppServer/profiles/Dmgr01/bin
./wsadmin.sh -lang jython \
  -f /path/to/nextgenbank/wsadmin/deploy_app.py \
  /path/to/nextgenbank/target/nextgenbank.war AppSrv01 server1
```

---

## 8. Test it end to end

```bash
curl -s http://<host>:9080/nextgenbank/api/health
# expect: {"status":"UP","db":"UP"}
```

Then in a browser:

```
http://<host>:9080/nextgenbank/login.jsp
```

- Log in as `jsmith` / `Passw0rd!`
- Confirm you see two accounts (Checking $5230.55, Savings $18500.00)
- Transfer $50 from the Checking account ID to the Savings account ID
- Confirm the balances update and the transfer shows up under "Recent
  Transactions"
- Confirm a row was written to `audit_log`:

```bash
psql -U nextgenbank_app -d nextgenbank -h localhost -c "SELECT * FROM audit_log ORDER BY logged_at DESC LIMIT 5;"
```

---

## 9. Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `Test connection` fails with "connection refused" | Postgres not listening on the port/host WAS is using | Check `postgresql.conf` `listen_addresses`, confirm `psql -h <host> -p 5432` works from the WAS host itself |
| `Test connection` fails with "password authentication failed" | `pg_hba.conf` auth method or wrong password | Confirm the user/password in the DataSource custom properties match Step 3; check `pg_hba.conf` |
| App shows "Partial Start" after deploy | resource-ref not bound to the DataSource, or a classloader conflict | Recheck the resource-ref mapping step in Step 7; check `SystemOut.log` for the real exception |
| `NameNotFoundException` for `jdbc/nextgenbankDS` | resource-ref binding mismatch between `web.xml` and the deployment mapping | Confirm the JNDI name matches exactly in both places (Session 9 material) |
| Login always fails even with correct password | seed data hash mismatch, or DB not reachable | Re-run `db/seed.sql`; verify with a direct `psql` query that `password_hash` matches the value in `seed.sql` |
| `/api/health` returns `{"status":"DOWN"}` | DataSource unreachable at runtime even though "Test connection" passed in Admin Console | Restart the app server after any DataSource config change; check connection pool `maxConnections` isn't exhausted |
| Blank page or 500 on `/dashboard` | Check `SystemOut.log` — most likely an unhandled exception in `DashboardServlet` | The app should now redirect to a generic error page (Phase 1 fix) instead of a raw stack trace — if you still see a stack trace, redeploy the latest WAR |

**Where to look when something breaks:**

```
<AppSrv01>/logs/server1/SystemOut.log     # application + WAS runtime messages
<AppSrv01>/logs/server1/SystemErr.log     # uncaught exceptions
<AppSrv01>/logs/server1/startServer.log   # startup failures
<AppSrv01>/logs/ffdc/                     # First Failure Data Capture — full stack traces
```

---

## 10. Quick reference — default ports

| Purpose | Default port |
|---|---|
| Admin Console (secure) | 9043 |
| Admin Console (unsecure) | 9060 |
| Application HTTP | 9080 |
| Application HTTPS | 9443 |
| Dmgr SOAP connector | 8879 |
| PostgreSQL | 5432 |

Ports can differ from these defaults if multiple profiles share a host —
check **Servers → Server Types → WebSphere application servers → server1 →
Ports** in the Admin Console to confirm yours.

---

## Next step

Once you can log in, transfer funds, and see the transaction land in both
`transactions` and `audit_log`, Phase 1 is done. From here, Month 2 of the
training plan replaces the local-transaction `AccountDAO.transfer()` with a
container-managed EJB, adds a SIBus/MDB-driven async audit path, and layers
in LDAP-backed authentication.
