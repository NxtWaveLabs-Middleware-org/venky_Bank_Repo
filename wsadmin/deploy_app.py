# wsadmin -lang jython -f deploy_app.py <pathToWar> <nodeName> <serverName>
#
# Installs (or reinstalls) the NextGenBank WAR, maps it to the target
# server, binds the jdbc/nextgenbankDS resource-ref, and starts the app.
# This is the Session 28 lab script - extend it with the backupConfig call
# and health-check/rollback logic for Session 43 (CI/CD).

import sys

warPath = sys.argv[0]
nodeName = sys.argv[1]
serverName = sys.argv[2]
appName = "NextGenBank"

target = "WebSphere:cell=%s,node=%s,server=%s" % (AdminControl.getCell(), nodeName, serverName)

installed = AdminApp.list().split('\n')
if appName in installed:
    print("Existing install found, uninstalling first...")
    AdminApp.uninstall(appName)
    AdminConfig.save()

print("Installing " + appName + " from " + warPath)

options = [
    '-appname', appName,
    '-contextroot', '/nextgenbank',
    '-MapModulesToServers', [[appName, appName + '.war,WEB-INF/web.xml', target]],
    '-MapResRefToEJB', [[appName, appName, appName + '.war,WEB-INF/web.xml',
                          'jdbc/nextgenbankDS', 'javax.sql.DataSource', 'jdbc/nextgenbankDS', '', '']]
]

AdminApp.install(warPath, options)
AdminConfig.save()

print("Starting application...")
AdminControl.invoke(AdminControl.completeObjectName('type=Application,name=' + appName + ',*'), 'start')

print("Deployed and started " + appName + " on " + target)
print("Verify: curl http://<host>:<port>/nextgenbank/api/health")
