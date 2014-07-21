#!/bin/bash -l
#
# Run all the unit tests using each of the several profiles.
#
use Maven-3.0
JBOSS_HOME=/prodinfolocal/jboss-as-7.1.1.Final/
MAVEN_OPTS="-Xms4g -XX:MaxPermSize=1g"
#mvn clean | tee tests.log
OPTIONS="-PArquillian-JBossAS7-Remote,BUILD,Clover.All -Djava.awt.headless=true --batch-mode -Dmaven.download.meter=silent -Dmaven.clover.licenseLocation=/prodinfolocal/BambooHome/clover.license"
PROFILES="Tests.ArqSuite.Standard Tests.ArqSuite.Stubby Tests.DatabaseFree Tests.ExternalIntegration Tests.Alternatives"
#PROFILES="Tests.ArqSuite.Standard"
for PROFILE in $PROFILES
do
    echo "Using profile $PROFILE"
    mvn $OPTIONS -P$PROFILE clover2:setup verify | tee -a tests.log
    mv target/clover/surefire-reports target/clover/surefire-reports-$PROFILE
done

mvn $OPTIONS clover2:aggregate clover2:clover | tee -a tests.log
