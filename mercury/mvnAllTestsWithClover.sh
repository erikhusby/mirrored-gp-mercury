#!/bin/bash -l
#
# Run all the unit tests using each of the several profiles.
#
use Maven-3.0
#mvn clean | tee tests.log
OPTIONS="-PArquillian-JBossAS7-Remote,BUILD,Clover.All -Djava.awt.headless=true --batch-mode -Dmaven.download.meter=silent -Dmaven.clover.licenseLocation=/prodinfolocal/BambooHome/clover.license"
PROFILES="Tests.ArqSuite.Standard Tests.ArqSuite.Stubby Tests.DatabaseFree Tests.ExternalIntegration Tests.Alternatives"
#PROFILES="Tests.ArqSuite.Standard Tests.ArqSuite.Stubby"
for PROFILE in $PROFILES
do
    echo "Using profile $PROFILE"
    mvn $OPTIONS -P$PROFILE test | tee -a tests.log
#    echo -n 1>&2 "Press return to continue."; read CONTINUE
    mv target/surefire-reports target/surefire-reports-$PROFILE
done

mvn $OPTIONS clover2:aggregate clover2:clover | tee -a tests.log
