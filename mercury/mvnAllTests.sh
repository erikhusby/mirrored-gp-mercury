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
	-m <maven>	Specifies additional Maven options. Can be mentioned more than once, they accumulate
	-c 		    Runs tests with Clover.
	-u <java>   Specifies the Java version to use. Default is Java-1.7
	-w          Running under Wildfly, default is JBOSS 7

The standard set of test profiles includes:
    Tests.ArqSuite.Standard Tests.ArqSuite.Stubby Tests.Multithreaded Tests.DatabaseFree Tests.ExternalIntegration Tests.Alternatives
    The Tests.LongRunning is excluded by default.

Each test profile will be executed separately. The results of each test execution wil be stored in ./surefire-reports-profile where "profile"
is the test profile name. The exit status will be the failure if any test profile fails, otherwise will be success.

The complete build logs will be in tests-$TEST.log

EOF
}
TESTS="Tests.ArqSuite.Standard Tests.ArqSuite.Stubby Tests.Multithreaded Tests.DatabaseFree Tests.ExternalIntegration Tests.Alternatives"
BUILD_PROFILE="BUILD"
ARQUILLIAN_PROFILE=Arquillian-JBossAS7-Remote
CLOVER=0
ADDITIONAL_OPTIONS=
JAVA_USE="Java-1.7"

while getopts "hcwt:b:j:m:u:" OPTION; do
    case $OPTION in
	h)
	    usage
	    exit 1
	    ;;
	t) TESTS=$OPTARG;;
	b) BUILD_PROFILE=$OPTARG;;
	j) JBOSS_HOME=$OPTARG;;
	c) CLOVER=1;;
	m) ADDITIONAL_OPTIONS="$ADDITIONAL_OPTIONS $OPTARG";;
	u) JAVA_USE=$OPTARG;;
	w) ARQUILLIAN_PROFILE="Arquillian-WildFly10-Remote";;
	[?])
	    usage
	    exit 1
	    ;;
    esac
done

cat <<EOF
TESTS=$TESTS
BUILD_PROFILE=$BUILD_PROFILE
ARQUILLIAN_PROFILE=$ARQUILLIAN_PROFILE
CLOVER=$CLOVER
ADDITIONAL_OPTIONS=$ADDITIONAL_OPTIONS
JAVA_USE=$JAVA_USE
JBOSS_HOME=$JBOSS_HOME
EOF

if [ -e "/broad/tools/scripts/useuse" ]
then
    source /broad/tools/scripts/useuse
    export MAVEN2_HOME=/prodinfo/prod3pty/apache-maven-3.6.3
    PATH=$MAVEN2_HOME/bin:$PATH
    use $JAVA_USE
else
    echo "Unable to set $JAVA_USE"
    ls -l /broad/tools/scripts/useuse
    exit 1
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
export MAVEN_OPTS="-Xms4g -XX:MaxPermSize=1g $SSL_OPTS"


EXIT_STATUS=0

# Setup the build options
if [[ $CLOVER -eq 0 ]]
then
    GOALS="clean test"
else
    GOALS="clean clover:setup verify"
    BUILD_PROFILE="$BUILD_PROFILE,Clover.All -Dmaven.clover.licenseLocation=/prodinfolocal/BambooHome/clover.license -DmercuryCloverDatabase=`pwd`/cloverdb/clover.db"
fi
BUILD_PROFILE=$ARQUILLIAN_PROFILE,$BUILD_PROFILE

OPTIONS="-P$BUILD_PROFILE -Djava.awt.headless=true --batch-mode  -Dannotation.outputDiagnostics=false -Dmaven.download.meter=silent $ADDITIONAL_OPTIONS"

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

    cat <<EOF
<<<<
    Finished test profile $TEST
<<<<

EOF


# Save the test results outside of the target directory
    if [ -e "target/surefire-reports" ]
    then
        if [ -e "surefire-reports-$TEST" ]
        then
            rm -rvf surefire-reports-$TEST
        fi
        mv -v target/surefire-reports surefire-reports-$TEST
    else
        echo "<<<< No target/surefire-reports directory found!"
    fi
    if [ -e "target/clover/surefire-reports" ]
    then
        if [ -e "clover/surefire-reports-$TEST" ]
        then
            rm -rvf clover/surefire-reports-$TEST
        fi
        mv -v target/clover/surefire-reports clover/surefire-reports-$TEST
    else
        echo "<<<< No target/clover/surfire-reports directory found"
    fi
done

exit $EXIT_STATUS


