#!/bin/bash
#
# Run all the unit tests using each of the several profiles.
#
#mvn clean | tee tests.log
OPTIONS="-PArquillian-JBossAS7-Remote -Djava.awt.headless=true --batch-mode -Dmaven.download.meter=silent"
for PROFILE in Tests.ArqSuite.Standard Tests.ArqSuite.Stubby Tests.DatabaseFree Tests.ExternalIntegration Tests.Alternatives
do
    echo "Using profile $PROFILE"
    mvn $OPTIONS -P$PROFILE test | tee -a tests.log
#    echo -n 1>&2 "Press return to continue."; read CONTINUE
done


