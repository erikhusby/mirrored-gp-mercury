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
    RELEASEID="1.0-SNAPSHOT"
fi

HOST="seqlims.broadinstitute.org"
ADMIN_PORT=4848
ADMIN_USER=admin
WAR_FILE="target/SequeL-$RELEASEID.war"
APP_NAME="SequeL"
DOMAIN="domain1"
WATCHERS="mhusby@broadinstitute.org andrew@broadinstitute.org mcovarr@broadinstitute.org epolk@broadinstitute.org breilly@broadinstitute.org sampath@broadinstitute.org thompson@broadinstitute.org "
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

    MAILTXT=/tmp/sequel.txt
    cat >$MAILTXT <<EOF

    Sequel $RELEASEID has been deployed to $HOST/$APP_NAME

EOF
    mail -s "SequeL deployed to $HOST" $WATCHERS < $MAILTXT
else
    echo "Unable to deploy non-existant $WAR_FILE"
fi
