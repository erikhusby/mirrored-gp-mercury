#!/bin/bash
#
# Run all the unit tests using each of the several profiles.
#
if [ -e "/broad/tools/scripts/useuse" ]
then
    source /broad/tools/scripts/useuse
    export MAVEN2_HOME=/prodinfo/prod3pty/apache-maven-3.6.3
    PATH=$MAVEN2_HOME/bin:$PATH
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
OPTIONS="-PArquillian-WildFly10-Remote,$BUILD_PROFILE,Clover.All -Djava.awt.headless=true --batch-mode -Dmaven.download.meter=silent -Dmaven.clover.licenseLocation=/prodinfolocal/BambooHome/clover.license"
PROFILES="Tests.ArqSuite.Standard Tests.ArqSuite.Stubby Tests.Multithreaded Tests.DatabaseFree Tests.ExternalIntegration Tests.Alternatives"
#PROFILES="Tests.DatabaseFree Tests.Multithreaded"
#PROFILES="Tests.Multithreaded"

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
        if [ -e "surefire-reports-clover-$PROFILE" ]
        then
            rm -rf surefire-reports-clover-$PROFILE
        fi
        mv target/clover/surefire-reports surefire-reports-clover-$PROFILE
    fi

    if [ -e "target/surefire-reports" ]
    then
        if [ -e "surefire-reports-$PROFILE" ]
        then
            rm -rf surefire-reports-$PROFILE
        fi
        mv target/surefire-reports surefire-reports-$PROFILE
    fi

done

# Move the clover reports back under target/clover
mv -v surefire-reports-clover-* target/clover

mvn $OPTIONS clover:aggregate clover:clover | tee -a tests.log
if [ ${PIPESTATUS[0]} -ne 0 ]
then
    EXIT_STATUS=${PIPESTATUS[0]}
fi

exit $EXIT_STATUS
