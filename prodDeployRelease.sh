#!/bin/bash
#
# Deploy SequeL to seqlims
#
HOST="seqlims.broadinstitute.org"
ADMIN_PORT=4848
ADMIN_USER=admin
WAR_FILE="target/SequeL-1.0-SNAPSHOT.war"
APP_NAME="SequeL"
DOMAIN="domain1"

#AS_INSTALL="/prodinfolocal/glassfish3"
doRemote() {
    echo "Remote: $@"
    ssh releng@$HOST $@
}

if [ -e $WAR_FILE ] ; then
    OPTIONS="--host $HOST --port $ADMIN_PORT --user $ADMIN_USER --echo=true --passwordfile $AS_INSTALL/seqlims-passwords.txt"

    asadmin $OPTIONS undeploy $APP_NAME
    asadmin $OPTIONS restart-domain
#    doRemote asadmin start-domain $DOMAIN
    asadmin $OPTIONS deploy --name $APP_NAME --upload=true $WAR_FILE
    asadmin $OPTIONS list-applications

else
    echo "Unable to deploy non-existant $WAR_FILE"
fi
