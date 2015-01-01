#!/bin/bash -l
#
# Run all the unit tests using each of the several profiles.
#
use Maven-3.0
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
OPTIONS="--offline -PArquillian-JBossAS7-Remote,BUILD,Clover.All -DtestFailureIgnore=true -Djava.awt.headless=true --batch-mode -Dmaven.download.meter=silent -Dmaven.clover.licenseLocation=/prodinfolocal/BambooHome/clover.license"
PROFILES="Tests.ArqSuite.Standard Tests.ArqSuite.Stubby Tests.DatabaseFree Tests.ExternalIntegration Tests.Alternatives"
#PROFILES="Tests.ArqSuite.Standard"

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
    mvn $OPTIONS -P$PROFILE clover2:setup verify | tee -a tests.log
    if [ -e "target/clover/surefire-reports" ]
    then
        mv target/clover/surefire-reports target/clover/surefire-reports-$PROFILE
    fi
    if [ -e "target/surefire-reports" ]
    then
        mv target/surefire-reports target/surefire-reports-$PROFILE
    fi
done

mvn $OPTIONS clover2:aggregate clover2:clover | tee -a tests.log
