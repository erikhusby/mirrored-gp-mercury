#!/bin/bash -l
#
# Run all the unit tests using each of the several profiles.
#
use Maven-3.0
#mvn clean | tee tests.log
JBOSS_HOME=/prodinfolocal/jboss-as-7.1.1.Final/
MAVEN_OPTS="-Xms4g -XX:MaxPermSize=1g"
OPTIONS="-PArquillian-JBossAS7-Remote,BUILD -Djava.awt.headless=true --batch-mode -Dmaven.download.meter=silent"
PROFILES="Tests.ArqSuite.Standard Tests.ArqSuite.Stubby Tests.DatabaseFree Tests.ExternalIntegration Tests.Alternatives"
#PROFILES="Tests.ArqSuite.Standard Tests.ArqSuite.Stubby"

rm tests.log
for PROFILE in $PROFILES
do
    echo "Using profile $PROFILE"
    mvn $OPTIONS -P$PROFILE test | tee -a tests.log
#    echo -n 1>&2 "Press return to continue."; read CONTINUE
    mv target/surefire-reports target/surefire-reports-$PROFILE
done


