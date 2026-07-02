# wsadmin -lang jython -f create_datasource.py <nodeName> <serverName>
#
# Creates a JDBC Provider + DataSource for PostgreSQL, scoped to the given
# server, and binds it to jdbc/nextgenbankDS (matching web.xml's resource-ref
# and DataSourceProvider.java's JNDI lookup).
#
# Prereqs:
#   - Download the PostgreSQL JDBC driver (postgresql-42.x.x.jar) and copy it
#     to a shared path on the WAS node, e.g. /opt/IBM/WebSphere/jdbcdrivers/postgresql.jar
#   - Adjust DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASSWORD below.

import sys

nodeName = sys.argv[0]
serverName = sys.argv[1]

DRIVER_PATH = "/opt/IBM/WebSphere/jdbcdrivers/postgresql.jar"
DB_HOST = "localhost"
DB_PORT = "5432"
DB_NAME = "nextgenbank"
DB_USER = "nextgenbank_app"
DB_PASSWORD = "changeit"   # for the lab; use a J2C alias + vault in real environments

scope = "/Node:%s/Server:%s/" % (nodeName, serverName)

print("Creating JDBC Provider under scope: " + scope)

provider = AdminConfig.create('JDBCProvider', AdminConfig.getid(scope), [
    ['name', 'NextGenBankPostgreSQLProvider'],
    ['description', 'PostgreSQL JDBC Provider for NextGenBank'],
    ['implementationClassName', 'org.postgresql.ds.PGSimpleDataSource'],
    ['classpath', [DRIVER_PATH]]
])

print("Creating DataSource...")

ds = AdminConfig.create('DataSource', provider, [
    ['name', 'NextGenBankDS'],
    ['jndiName', 'jdbc/nextgenbankDS'],
    ['authDataAlias', ''],
    ['statementCacheSize', 20]
])

propertySet = AdminConfig.showAttribute(ds, 'propertySet')
props = AdminConfig.showAttribute(propertySet, 'resourceProperties')

def setProp(name, value):
    for p in props.split(' '):
        if not p:
            continue
        pName = AdminConfig.showAttribute(p, 'name')
        if pName == name:
            AdminConfig.modify(p, [['value', value]])
            return
    AdminConfig.create('J2EEResourceProperty', propertySet, [['name', name], ['value', value], ['type', 'java.lang.String']])

setProp('serverName', DB_HOST)
setProp('portNumber', DB_PORT)
setProp('databaseName', DB_NAME)
setProp('user', DB_USER)
setProp('password', DB_PASSWORD)

# Reasonable starting pool sizing for a lab; revisit in the Session 7/31 labs
connPool = AdminConfig.showAttribute(ds, 'connectionPool')
if connPool:
    AdminConfig.modify(connPool, [['minConnections', 5], ['maxConnections', 20], ['reapTime', 180], ['unusedTimeout', 1800]])

AdminConfig.save()

print("Done. DataSource jdbc/nextgenbankDS created under " + scope)
print("Test it from Admin Console: Resources > JDBC > Data sources > NextGenBankDS > Test connection")
