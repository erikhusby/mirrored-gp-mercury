#!/bin/bash -l
#
# Run all the unit tests using each of the several profiles.
#
use Maven-3.0
#mvn clean | tee tests.log
if [ "x$JBOSS_HOME" == "x" ]
then
    JBOSS_HOME=/prodinfolocal/jboss-as-7.1.1.Final/
fi
if [ "x$SSL_OPTS" == "x" ]
then
    KEYSTORE_FILE="../JBossConfig/src/main/resources/keystore/.keystore"
    KEYSTORE_PASSWORD="changeit"
    SSL_OPTS="-DkeystoreFile=$KEYSTORE_FILE -DkeystorePassword=$KEYSTORE_PASSWORD"
fi
MAVEN_OPTS="-Xms4g -XX:MaxPermSize=1g $SSL_OPTS"
OPTIONS="-PArquillian-JBossAS7-Remote,BUILD -Djava.awt.headless=true --batch-mode -Dmaven.download.meter=silent "
PROFILES="Tests.ArqSuite.Standard Tests.ArqSuite.Stubby Tests.DatabaseFree Tests.ExternalIntegration Tests.Alternatives"
#PROFILES="Tests.ExternalIntegration"

cat <<EOF
Using:
JBOSS_HOME=$JBOSS_HOME
MAVEN_OPTS=$MAVEN_OPTS
OPTIONS=$OPTIONS
PROFIES=$PROFILES

EOF
rm tests.log
for PROFILE in $PROFILES
do
    echo "Using profile $PROFILE"
    mvn $OPTIONS -P$PROFILE test | tee -a tests.log
#    echo -n 1>&2 "Press return to continue."; read CONTINUE
    if [ -e "target/surefire-reports" ]
    then
        mv target/surefire-reports target/surefire-reports-$PROFILE
    fi
done


