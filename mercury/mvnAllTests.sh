#!/bin/bash
#
# Run all the unit tests using each of the several profiles.
#
if [ -e "/broad/tools/scripts/useuse" ]
then
    source /broad/tools/scripts/useuse
    use Maven-3.1
    use Java-1.8
fi

if [ "x$JBOSS_HOME" == "x" ]
then
    cat <<EOF

    You must define JBOSS_HOME to point to the Wildfly installation.

EOF
    exit 1
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

MAVEN_OPTS="-Xms4g -XX:MaxMetaspaceSize=1g $SSL_OPTS"
OPTIONS="-PArquillian-WildFly10-Remote,$BUILD_PROFILE -Djava.awt.headless=true --batch-mode -Dmaven.download.meter=silent "
PROFILES="Tests.ArqSuite.Standard Tests.ArqSuite.Stubby Tests.Multithreaded Tests.DatabaseFree Tests.ExternalIntegration Tests.Alternatives"
#PROFILES="Tests.ExternalIntegration"
#PROFILES="Tests.DatabaseFree"
#PROFILES="Tests.Multithreaded"
#PROFILES="Tests.Alternatives"

EXIT_STATUS=0

if [ -f "tests.log" ]
then
    rm tests.log
fi

for PROFILE in $PROFILES
do
    cat <<EOF
>>>>>

Properties
JBOSS_HOME=$JBOSS_HOME
MAVEN_OPTS=$MAVEN_OPTS
OPTIONS=$OPTIONS
PROFILES=$PROFILES
JAVA_HOME=$JAVA_HOME

>>>> Executing profile $PROFILE

EOF
# Adding the clean to deal with a known Java 1.8 bug when compling from Maven -- see JDK-8067747
    mvn $OPTIONS -P$PROFILE clean test | tee -a tests.log
    if [ ${PIPESTATUS[0]} -ne 0 ]
    then
        EXIT_STATUS=${PIPESTATUS[0]}
    fi

#    echo -n 1>&2 "Press return to continue."; read CONTINUE
    if [ -e "target/surefire-reports" ]
    then
        if [ -e "surefire-reports-$PROFILE" ]
        then
            rm -rf surefire-reports-$PROFILE
        fi
        mv target/surefire-reports surefire-reports-$PROFILE
    fi
done

exit $EXIT_STATUS


