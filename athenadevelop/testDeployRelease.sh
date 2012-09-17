#!/bin/bash
#
# Deploy SequeL to seqlims
#
# Usage: prodDeployRelease.sh RELEASE_ID
#
if [ $# -eq 1 ]
then
    RELEASEID=$1
else
    RELEASEID="2.0.0-SNAPSHOT"
fi

HOST="pmbridgedev.broadinstitute.org"
ADMIN_PORT=4848
ADMIN_USER=admin
WAR_FILE="target/pmbridge-$RELEASEID.war"
APP_NAME="PMBridge"
DOMAIN="domain1"
WATCHERS="mhusby@broadinstitute.org mccrory@broadinstitute.org pshapiro@broadinstitute.org "
#AS_INSTALL="/prodinfolocal/glassfish3"

if [ -e $WAR_FILE ] ; then
    OPTIONS="--host $HOST --port $ADMIN_PORT --user $ADMIN_USER --echo=true "

    asadmin $OPTIONS undeploy $APP_NAME
    asadmin $OPTIONS restart-domain
    asadmin $OPTIONS deploy --name $APP_NAME --upload=true $WAR_FILE
    asadmin $OPTIONS list-applications

#    MAILTXT=/tmp/sequel.txt
#    cat >$MAILTXT <<EOF
#
#    Sequel $RELEASEID has been deployed to $HOST/$APP_NAME
#
#EOF
#    mail -s "SequeL deployed to $HOST" $WATCHERS < $MAILTXT
else
    echo "Unable to deploy non-existant $WAR_FILE"
fi
