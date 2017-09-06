#!/bin/bash
#
# Run all the unit tests using each of the several profiles.
#
usage() {
    cat <<EOF
Usage: $0 [-t <test> ] [-b <build>] [-j <jboss> ]

Where:
	-h		Show this message
	-t <test>	Specifies a particular test profile to be run. Defaults to the standard set.
	-b <build>	Specifies a particular build profile to be used. Defaults to BUILD.
	-j <jboss>	Specifies a particular JBoss or Wildfly installation.
	-m <maven>	Specifies additional Maven options.
	-c 		Runs tests with Clover.

The standard set of test profiles includes:
    Tests.ArqSuite.Standard Tests.ArqSuite.Stubby Tests.Multithreaded Tests.DatabaseFree Tests.ExternalIntegration Tests.Alternatives
    The Tests.LongRunning is excluded by default.

Each test profile will be executed separately. The results of each test execution wil be stored in ./surefire-reports-profile where "profile"
is the test profile name. The exit status will be the failure if any test profile fails, otherwise will be success.

The complete build log will be in tests.log

EOF
}
TESTS="Tests.ArqSuite.Standard Tests.ArqSuite.Stubby Tests.Multithreaded Tests.DatabaseFree Tests.ExternalIntegration Tests.Alternatives"
BUILD=
CLOVER=0
ADDITIONAL_OPTIONS=

while getopts "hct:b:j:m:" OPTION; do
    case $OPTION in
	h) usage; exit 1;;
	t) TESTS=$OPTARG;;
	b) BUILD=$OPTARG;;
	j) JBOSS_HOME=$OPTARG;;
	c) CLOVER=1;;
	m) ADDITIONAL_OPTIONS=$OPTARG;;
	[?]) usage; exit 1;;
    esac
done

if [ -e "/broad/tools/scripts/useuse" ]
then
    source /broad/tools/scripts/useuse
    use -v Maven-3.1
    use -v Java-1.7    
fi

if [ ! -e "$JBOSS_HOME" ]
then
    cat <<EOF

    You must define JBOSS_HOME to point to the JBoss or Wildfly installation.

EOF
    exit 1
fi

if [ "x$SSL_OPTS" == "x" ]
then
    KEYSTORE_FILE="../JBossConfig/src/main/resources/keystore/.keystore"
    KEYSTORE_PASSWORD="changeit"
    SSL_OPTS="-DkeystoreFile=$KEYSTORE_FILE -DkeystorePassword=$KEYSTORE_PASSWORD"
fi
MAVEN_OPTS="-Xms4g -XX:MaxPermSize=1g $SSL_OPTS"

BUILD_PROFILE=$BUILD
if [ "x$BUILD_PROFILE" == "x" ]
then
    BUILD_PROFILE="BUILD"
fi


EXIT_STATUS=0

# Setup the build options
if [[ $CLOVER -eq 0 ]]
then
    GOALS="clean test"
else
    GOALS="clean clover:setup verify"
    rm -rf clover/
    mkdir clover
    BUILD_PROFILE="$BUILD_PROFILE,Clover.All -Dmaven.clover.licenseLocation=/prodinfolocal/BambooHome/clover.license -DmercuryCloverDatabase=`pwd`clover/clover.db"
fi

OPTIONS="-PArquillian-JBossAS7-Remote,$BUILD_PROFILE -Djava.awt.headless=true --batch-mode  -Dannotation.outputDiagnostics=false -Dmaven.download.meter=silent $ADDITIONAL_OPTIONS"

for TEST in $TESTS
do
    cat <<EOF
>>>>>

Properties
JBOSS_HOME=$JBOSS_HOME
MAVEN_OPTS=$MAVEN_OPTS
OPTIONS=$OPTIONS

>>>> Executing test profile $TEST

EOF

# Run the current test set
    mvn $OPTIONS -P$TEST $GOALS | tee -a tests-$TEST.log
    if [ ${PIPESTATUS[0]} -ne 0 ]
    then
        EXIT_STATUS=${PIPESTATUS[0]}
    fi

# Save the test results outside of the target directory
    if [ -e "target/surefire-reports" ]
    then
        if [ -e "surefire-reports-$TEST" ]
        then
            rm -rf surefire-reports-$TEST
        fi
        mv target/surefire-reports surefire-reports-$TEST
    fi
    if [ -e "target/clover/surefire-reports" ]
    then
        if [ -e "clover/surefire-reports-$TEST" ]
        then
            rm -rf clover/surefire-reports-$TEST
        fi
        mv target/clover/surefire-reports clover/surefire-reports-$TEST
    fi

done


exit $EXIT_STATUS


