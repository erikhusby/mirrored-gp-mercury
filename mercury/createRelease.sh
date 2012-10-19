#!/bin/bash
#
# Perform a Production release of Mercury
#
# Usage: Release.sh
#
HOST="vseqlims.broadinstitute.org"
APP_NAME="Mercury"
WATCHERS="mhusby@broadinstitute.org"
#andrew@broadinstitute.org "

if [ -d "target" ] ; then
    rm -rf target
fi
mkdir target

git clone ssh://git@stash.broadinstitute.org:7999/GPIN/mercury.git target
cd target
git checkout QA
git checkout -b QA_PROD
cd mercury
mvn -P\!DefaultProfile --batch-mode -DdryRun=run release:prepare release:perform
cd target/checkout
mvn -PPROD -DskipTests package
exit 0



if [ -e $WAR_FILE ] ; then
    MAILTXT=/tmp/mecury.txt
    cat >$MAILTXT <<EOF

    Mercury $RELEASEID has been deployed to $HOST/$APP_NAME

EOF
    mail -s "$APP_NAMEL deployed to $HOST" $WATCHERS < $MAILTXT
else
    echo "Unable to deploy non-existant $WAR_FILE"
fi
