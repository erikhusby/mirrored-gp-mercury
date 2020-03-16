#!/bin/bash
usage() {
    cat <<EOF

     Usage: $0 [-j jboss_server] [-w wildfly_server] [-m MAVENOPTS]+  [-c] [-b PROFILE]

     Runs all the Mercury unit tests for a Bamboo build.

     Where:
        -h  Show this message
        -w  Use Wildfly, default is JBoss
        -s  Specifies the server home. Example, for JBoss, seq01-arquillian. For Wildfly, /local/prodinfolocal/wildfly
        -c  Runs the tests with Clover.
       	-m <maven>	Specifies additional Maven options. Can be mentioned more than once, they accumulate.
       	-b  Specifies a particular build profile to be used. Defaults to BUILD,Arquillian-JBossAS7-Remote.
       	-u <java>  Specifies version of Java to use, defaults to Java-1.7
	-t <test> Specifies a particular test profile to be used, defaults to all but Tests.LongRunning

Either the JBoss or Wildfly server must be specified.

EOF
}


TESTS_ARQUILLIAN="Tests.ArqSuite.Standard Tests.ArqSuite.Stubby Tests.ExternalIntegration Tests.Alternatives"
#TESTS_ARQUILLIAN="Tests.ArqSuite.Standard Tests.ArqSuite.Stubby Tests.ExternalIntegration"
TESTS_NONARQUILLIAN="Tests.Multithreaded Tests.DatabaseFree "
TESTS_PROFILE=
BUILD="BUILD"
CLOVER=""
SERVER_HOME=
USE_WILDFLY=""
ADDITIONAL_OPTIONS=
JAVA_USE="Java-1.7"

while getopts "hcwb:s:w:m:t:u:" OPTION; do
    case $OPTION in
	h) usage
	    exit 1
	    ;;
	b) BUILD=$OPTARG
	    ;;
	s) SERVER_HOME=$OPTARG
	    ;;
	w) USE_WILDFLY="-w"
	    ;;
	c) CLOVER="-c"
	    ;;
	m) ADDITIONAL_OPTIONS="$ADDITIONAL_OPTIONS -m $OPTARG "
	    ;;
	u) JAVA_USE=$OPTARG
	   ;;
	t) TEST_PROFILE=$OPTARG
	   ;;
	[?]) usage
	    exit 1
	    ;;
    esac
done

cat <<EOF
BUILD=$BUILD
SERVER_HOME=$SERVER_HOME
USE_WILDFLY=$USE_WILDFLY
CLOVER=$CLOVER
ADDITIONAL_OPTIONS=$ADDITIONAL_OPTIONS
JAVA_USE=$JAVA_USE
TEST_PROFILE=$TEST_PROFILE
EOF

if [ -e "/broad/tools/scripts/useuse" ]
then
    source /broad/tools/scripts/useuse
    use -v Maven-3.1
    use -v $JAVA_USE
fi


# Remove existing test logs and old surefire reports
rm tests-*.log
rm -rf surefire-reports*
rm -rf clover
mkdir clover
rm -rf cloverdb
mkdir cloverdb

# Define several useful functions.
# Execute an NonArqillian Test
nonArquillianTest() {
    ./mvnAllTests.sh -t $1 -b $BUILD $USE_WILDFLY -u $JAVA_USE $CLOVER $ADDITIONAL_OPTIONS
}

# Execute an Arquillian Test
arquillianTest() {
    server start
    ./mvnAllTests.sh -t $1 -b $BUILD $USE_WILDFLY -u $JAVA_USE $CLOVER $ADDITIONAL_OPTIONS
    server stop
}

# Execute a server start or stop operation
server() {
    if [ "$USE_WILDFLY" == "-w" ]
    then
        $SERVER_HOME/scripts/$1.sh
    else
        /prodinfo/prodapps/jboss/jboss.sh $SERVER_HOME $1
    fi
}

# if a test profile was not specified then  execute the Non-Arquillian tests first, then the Arquillian tests.
if [ "x$TEST_PROFILE" == "x" ]
then
    # Run the NonArquillian tests first
    for TEST in $TESTS_NONARQUILLIAN
    do
        nonArquillianTest $TEST
    done

    # Run the Arquillian tests
    for TEST in $TESTS_ARQUILLIAN
    do
        arquillianTest $TEST
    done
else
    # Otherwise, determine if the test relies on Arquillian support or not. Default to an Arquillian test if not
    # in the list of NonArquillian
    if [[ "$TESTS_NONARQUILLIAN" =~ "$TEST_PROFILE" ]]
    then
        nonArquillianTest $TEST_PROFILE
    else
        arquillianTest $TEST_PROFILE
    fi
fi

# Generate the clover report if necessary
if [ "$CLOVER" == "-c" ]
then
    mvn --batch-mode \
	-Djava.awt.headless=true \
	-Dmaven.clover.cloverDatabase=`pwd`/cloverdb/clover.db \
	-Dmaven.clover.licenseLocation=/prodinfo/releng/clover.license \
	clover:clover | tee tests-clover.log
    if [ ${PIPESTATUS[0]} -ne 0 ]
    then
	EXIT_STATUS=${PIPESTATUS[0]}
    fi
fi
