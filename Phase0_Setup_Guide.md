# Phase 0 — Environment Setup
## Practice Banking Application | WebSphere Admin Track

Complete every step in order. Each step has a verification command — don't move to Phase 1 until all checks pass.

---

## Step 1: Verify / Install JDK

WAS ND runs on its own bundled JDK, but you need a separate JDK on your machine to compile code before packaging.

```bash
java -version
echo $JAVA_HOME
```

**If missing:** Check IBM's supported JDK table for your exact WAS ND version/fix pack, then install that version.

**Pass criteria:** `java -version` returns a version number, and `JAVA_HOME` points to a valid JDK directory.

---

## Step 2: Verify / Install Maven

```bash
mvn -version
```

**If missing:**
```bash
sudo apt install maven      # Debian/Ubuntu
```

**Pass criteria:** `mvn -version` shows Maven version and confirms it's using the JDK from Step 1.

---

## Step 3: Install PostgreSQL

```bash
sudo apt install postgresql postgresql-contrib
sudo -u postgres psql -c "SELECT version();"
```

**Pass criteria:** Version string prints without error.

---

## Step 4: Create Database and App User

```bash
sudo -u postgres psql
```
```sql
CREATE DATABASE bankdb;
CREATE USER bankapp WITH PASSWORD 'ChangeMe123!';
GRANT ALL PRIVILEGES ON DATABASE bankdb TO bankapp;
\q
```

**Pass criteria:**
```bash
psql -U bankapp -d bankdb -h localhost
```
connects without error (you may need to edit `pg_hba.conf` to allow password auth for local connections — set method to `md5` or `scram-sha-256` for the `bankapp` user, then `sudo systemctl restart postgresql`).

---

## Step 5: Load the Schema

Using `schema.sql` from earlier:

```bash
psql -U bankapp -d bankdb -h localhost -f schema.sql
```

**Verify:**
```sql
SELECT a.account_number, a.balance, c.full_name
FROM accounts a JOIN customers c ON a.customer_id = c.customer_id;
```

**Pass criteria:** Returns one row — Ravi Kumar, ₹25,000.

---

## Step 6: Download PostgreSQL JDBC Driver

WAS does not ship this driver — you must supply it.

```bash
mkdir -p /opt/jdbc-drivers
wget -P /opt/jdbc-drivers https://jdbc.postgresql.org/download/postgresql-42.7.3.jar
```

**Pass criteria:** File exists at `/opt/jdbc-drivers/postgresql-42.7.3.jar`. (We'll point WAS's JDBC Provider at this path in Phase 1.)

---

## Step 7: IDE Setup

Recommended: **Eclipse + IBM WebSphere Developer Tools (WDT)** — lets you deploy/debug directly against local WAS ND.
Alternative: IntelliJ Community (write code only; deploy manually via console).

**Pass criteria:** IDE installed, can open a Maven project.

---

## Step 8: Initialize Git Repository

Start this now so Phase 9 (CI/CD) has history to work with, not a retrofit.

```bash
mkdir bankapp && cd bankapp
git init
```

Create `.gitignore`:
```
target/
*.class
*.log
.settings/
.classpath
.project
*.war
*.ear
```

**Critical rule:** Never commit real DB passwords or the seed password hash placeholder as if it were real. Credentials get externalized via WAS J2C aliases starting Phase 1.

**Pass criteria:** `git status` runs clean inside the repo, `.gitignore` committed.

---

## Step 9: WAS ND Profile Sanity Check

Log into the admin console and record these — you'll need them constantly from Phase 1 onward:

| Item | Your Value |
|---|---|
| Dmgr profile name | |
| Dmgr console URL (e.g. `https://localhost:9043/ibm/console`) | |
| Dmgr SOAP port | |
| Custom node 1 — profile name | |
| Custom node 1 — federated? (Y/N) | |
| Custom node 2 — profile name | |
| Custom node 2 — federated? (Y/N) | |
| App HTTP port (per node) | |
| App HTTPS port (per node) | |
| Admin console username | |
| Administrative Security enabled? (Y/N) | |
| WAS ND exact version / fix pack | |
| Namespace: javax.* or jakarta.* | |
| `wsadmin.sh` full path | |

**Why Administrative Security matters now:** if it's **enabled**, every wsadmin script and console login in later phases requires credentials — good to know before Phase 9 automation. If it's **disabled**, note that too; we'll enable it deliberately in Phase 4.

**Pass criteria:** Console loads, you can log in, table above is filled in.

---

## Step 10: Resource Check

```bash
free -h
```

**Pass criteria:** At least 4GB free RAM for a single AppServer JVM (you'll want 8GB+ free before Phase 5 clustering).

---

## Step 11: Confirm WAS ND Version → Java EE/Jakarta EE Alignment

This is the step most beginners skip and it breaks the build in Phase 1.

```bash
./versionInfo.sh    # from <WAS_HOME>/bin  (versionInfo.bat on Windows)
```

Depending on your exact WAS ND version:
- **WAS ND 9.x traditional** → supports up to **Java EE 8**, uses `javax.servlet.*`, `javax.ejb.*`, `javax.persistence.*` package names
- **Newer WAS ND with Jakarta EE support** → uses the `jakarta.*` namespace instead

These are **not interchangeable**. If your Maven `pom.xml` pulls a `jakarta.servlet-api` dependency but your WAS ND only supports `javax.servlet`, the WAR will fail to deploy or fail at runtime with `ClassNotFoundException`/`NoClassDefFoundError`.

**Pass criteria:** You know your exact WAS ND version and fix pack, and have confirmed whether it's `javax.*` or `jakarta.*` based, so Phase 1 dependency versions are picked correctly the first time.

---

## Step 12: Confirm Disk Space (not just RAM)

WAS profiles — especially a Dmgr plus multiple node profiles for Phase 5 clustering — consume disk quickly via logs, deployed binaries, and `wstemp`/`tranlog` directories.

```bash
df -h
```

**Pass criteria:** 20GB+ free if you plan to eventually run a Dmgr + 2 cluster members on this same machine.

---

## Step 13: Trust the Admin Console's SSL Certificate

The console (`https://localhost:9043/ibm/console`) uses a self-signed certificate by default.

- Accept/trust it in your browser now.
- If you'll later script against it (curl, wsadmin over SOAP/RMI in Phase 9), note that some tools require explicit handling, e.g. `-Dcom.ibm.SSL.ConfigURL` or an insecure/trust-all flag for local dev use only.

**Pass criteria:** Console loads in browser without certificate errors blocking you.

---

## Step 14: Locate `wsadmin` and Note Its Path

You'll use this constantly starting Phase 9 for scripted deployments.

```bash
find / -name "wsadmin.sh" 2>/dev/null
```

**Pass criteria:** Path recorded somewhere you'll have it handy (e.g. add to the profile table in Step 9, or your shell `PATH`).

---

## Step 15: Decide Your Phase 5 Clustering Layout Now

Clustering (Phase 5) needs a Deployment Manager profile + 2 or more Application Server (node) profiles.

Decide now, not later:
- **Option A:** Simulate everything on this one machine (fine, but resource-heavy — revisit Step 12's disk number and Step 10's RAM number with this in mind).
- **Option B:** Plan a second VM/machine now so node profiles are properly separated.

**Pass criteria:** A decision made and noted — no action needed yet, just avoids a mid-Phase-5 scramble.

**Confirmed topology for this project:**
- 1 Deployment Manager (Dmgr) profile
- 2 Custom (node) profiles, federated to the Dmgr cell
- All work from Phase 1 onward goes through the **Dmgr console**, not a standalone AppServer console

### Step 15a: Verify Node Federation

Since you're running custom profiles, they must be federated to the Dmgr cell before any deployment work in Phase 1. Check now:

```bash
# On Dmgr — confirm it's running
<DMGR_PROFILE>/bin/serverStatus.sh -all

# On each node — confirm federation status
<NODE_PROFILE>/bin/serverStatus.sh -all
```

In the Dmgr console (**System administration → Nodes**), confirm both node names show status **"Node Agent running"** or synced, not "Unavailable"/"Not federated".

If a node isn't federated yet:
```bash
<NODE_PROFILE>/bin/addNode.sh <dmgr_host> <dmgr_SOAP_port>
```

**Pass criteria:** Both custom nodes appear in the Dmgr console under Nodes, status shows synced/running.

---

## Step 16: Note License/Trial Expiry (if applicable)

If WAS ND is a trial license, record the expiry date somewhere visible.

**Pass criteria:** Expiry date noted, or confirmed this is a full license with no expiry concern.

---

## Phase 0 Exit Checklist

- [ ] `java -version` and `JAVA_HOME` confirmed
- [ ] `mvn -version` confirmed
- [ ] PostgreSQL installed and running
- [ ] `bankdb` database + `bankapp` user created, password auth working
- [ ] Schema loaded, seed query returns Ravi Kumar's account
- [ ] PostgreSQL JDBC jar downloaded to a known path
- [ ] IDE installed and able to open a Maven project
- [ ] Git repo initialized with `.gitignore`
- [ ] WAS ND console reachable, login confirmed, profile table filled in
- [ ] 4GB+ free RAM confirmed
- [ ] WAS ND version + fix pack confirmed, `javax.*` vs `jakarta.*` alignment known
- [ ] 20GB+ free disk space confirmed
- [ ] Admin console SSL cert trusted in browser
- [ ] `wsadmin.sh` path located and noted
- [ ] Phase 5 clustering layout decided → confirmed: 1 Dmgr + 2 custom nodes
- [ ] Both custom nodes federated and showing synced/running in Dmgr console
- [ ] License/trial expiry — confirmed: no expiry concern

Once every box is checked, you're ready for **Phase 1**: Maven WAR project structure, login Servlet, and creating your first JDBC Provider + DataSource in the WAS console.

---

### Note on jBCrypt (coming in Phase 1, no action needed yet)

The seed `password_hash` value in `app_users` is a placeholder. Phase 1 code will use **jBCrypt** to generate a real hash when the first user is created — flagging it now so it's not a surprise when it shows up in the `pom.xml` dependencies.
