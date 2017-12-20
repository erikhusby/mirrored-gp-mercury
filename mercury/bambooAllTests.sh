#!/bin/bash
usage() {
    cat <<EOF

     Usage: $0 [-j jboss_server] [-w wildfly_server] [-m MAVENOPTS]+  [-c] [-b PROFILE]

     Runs all the Mercury unit tests for a Bamboo build.

     Where:
        -h  Show this message
        -w  Specifies the particular Wildfly installation, for example on mercuryfb it would be /local/prodinfolocal/wildfly
        -j  Specifies the particular JBoss installation, for example: seq01-arqullian
        -c  Runs the tests with Clover.
       	-m <maven>	Specifies additional Maven options. Can be mentioned more than once, they accumulate.
       	-b  Specifies a particular build profile to be used. Defaults to BUILD,Arquillian-JBossAS7-Remote.
       	-u <java>  Specifies version of Java to use, defaults to Java-1.7

Either the JBoss or Wildfly server must be specified.

EOF
}


TESTS_ARQUILLIAN="Tests.ArqSuite.Standard Tests.ArqSuite.Stubby Tests.ExternalIntegration Tests.Alternatives"
TESTS_NONARQUILLIAN="Tests.Multithreaded Tests.DatabaseFree "
BUILD="BUILD"
CLOVER=""
JBOSS_SERVER=
WILDFLY_SERVER=
ADDITIONAL_OPTIONS=
JAVA_USE="Java-1.7"
let A=0

while getopts "hcb:j:w:m:u:" OPTION; do
    case $OPTION in
	h) usage
	    exit 1
	    ;;
	b) BUILD=$OPTARG
	    ;;
	j) JBOSS_SERVER=$OPTARG
	    SERVER_PROFILE="Arquillian-JBossAS7-Remote"
	    (( A += 1 ))
	    ;;
	w) WILDFLY_SERVER=$OPTARG
	    SERVER_PROFILE="Arquillian-WildFly10-Remote"
	    (( A += 1 ))
	    ;;
	c) CLOVER="-c"
	    ;;
	m) ADDITIONAL_OPTIONS="$ADDITIONAL_OPTIONS -m $OPTARG "
	    ;;
	u) JAVA_USE=$OPTARG
	    ;;
	[?]) usage
	    exit 1
	    ;;
    esac
done

BUILD="$BUILD,$SERVER_PROFILE"
cat <<EOF
BUILD=$BUILD
JBOSS_SERVER=$JBOSS_SERVER
WILDFLY_SERVER=$WILDFLY_SERVER
CLOVER=$CLOVER
ADDITIONAL_OPTIONS=$ADDITIONAL_OPTIONS
JAVA_USE=$JAVA_USE
EOF

exit

if [ -e "/broad/tools/scripts/useuse" ]
then
    source /broad/tools/scripts/useuse
    use -v Maven-3.1
    use -v $JAVA_USE
fi

# Only one of JBoss or Wildfly can be specified.
if (( A != 1 ))
then
    usage
    exit 1
fi

# Remove existing test logs and old surefire reports
rm -v tests-*.log
rm -vrf surefire-reports*

# Run the NonArquillian tests first
for TEST in $TESTS_NONARQUILLIAN
do
    ./mvnAllTests.sh -t $TEST -b $BUILD -u $JAVA_USE $CLOVER $ADDITIONAL_OPTIONS
done

server() {
    if [[ "x$JBOSS_SERVER" != "x" ]]
    then
        /prodinfo/prodapps/jboss/jboss.sh $JBOSS_SERVER $1
    fi
    if [[ "x$WILDFLY_SERVER" != "x" ]]
    then
        $WILDFLY_SERVER/wildfly.sh $1
    fi
}

# Run the Arquillian tests
for TEST in $TESTS_ARQUILLIAN
do
    server start
    ./mvnAllTests.sh -t $TEST -b $BUILD -u $JAVA_USE $CLOVER $ADDITIONAL_OPTIONS
    server stop
done

# Aggregate the clover results if necessary
if [[ "$CLOVER" == "-c" ]]
then
    mvn $OPTIONS clover:aggregate clover:clover | tee tests-clover.log
    if [ ${PIPESTATUS[0]} -ne 0 ]
    then
	EXIT_STATUS=${PIPESTATUS[0]}
    fi
fi
