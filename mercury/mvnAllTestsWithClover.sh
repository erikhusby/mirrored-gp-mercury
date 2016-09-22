#!/bin/bash
#
# Run all the unit tests using each of the several profiles.
#
source /broad/tools/scripts/useuse
use Maven-3.1
use Java-1.8

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
if [ "x$BUILD_PROFILE" == "x" ]
then
    BUILD_PROFILE=BUILD
fi

MAVEN_OPTS="-Xms4g -XX:MaxPermSize=1g $SSL_OPTS"
OPTIONS="-PArquillian-WildFly10-Remote,$BUILD_PROFILE,Clover.All -Djava.awt.headless=true --batch-mode -Dmaven.download.meter=silent -Dmaven.clover.licenseLocation=/prodinfolocal/BambooHome/clover.license"
PROFILES="Tests.ArqSuite.Standard Tests.ArqSuite.Stubby Tests.Multithreaded Tests.DatabaseFree Tests.ExternalIntegration Tests.Alternatives"
#PROFILES="Tests.DatabaseFree Tests.Multithreaded"
#PROFILES="Tests.Multithreaded"

EXIT_STATUS=0

if [ -f "tests.log" ]
then
    rm tests.log
fi

mvn clean | tee tests.log

for PROFILE in $PROFILES
do
    cat <<EOF
>>>>>

Properties
JBOSS_HOME=$JBOSS_HOME
MAVEN_OPTS=$MAVEN_OPTS
OPTIONS=$OPTIONS
PROFILES=$PROFILES

>>>> Executing profile $PROFILE

EOF

# Adding the clean to deal with a known Java 1.8 bug when compling from Maven -- see JDK-8067747
    mvn $OPTIONS -P$PROFILE clean clover:setup verify | tee -a tests.log
    if [ ${PIPESTATUS[0]} -ne 0 ]
    then
        EXIT_STATUS=${PIPESTATUS[0]}
    fi

    if [ -e "target/clover/surefire-reports" ]
    then
        mv target/clover/surefire-reports target/clover/surefire-reports-$PROFILE
    fi
    if [ -e "target/surefire-reports" ]
    then
        mv target/surefire-reports target/surefire-reports-$PROFILE
    fi
done

mvn $OPTIONS clover:aggregate clover:clover | tee -a tests.log
if [ ${PIPESTATUS[0]} -ne 0 ]
then
    EXIT_STATUS=${PIPESTATUS[0]}
fi

exit $EXIT_STATUS
