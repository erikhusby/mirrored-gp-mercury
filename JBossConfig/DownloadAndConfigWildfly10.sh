#!/bin/bash
#
usage() {
    cat <<EOF

Usage: $0 wildflydirectory
Where wildflydirectory is where wildfly 10 should be installed.
EOF
}

TARGET=$1
if [ -z $TARGET ]
then
   usage
   exit 1
fi

WILDFLY_VERSION='10.1.0.Final'
WELD_CDI_PATCH_VERSION='2.4.1.Final'
#
echo "*** Checking that Java 1.8 is available."
if [ -e "/broad/software/scripts" ]
then
    source /broad/software/scripts/useuse
    use Java-1.8
else
    java -version 2>&1 | grep -q "1.8"
    if [ $? -ne 0 ]
    then
       echo "Java 1.8 is required, please ensure that Java 1.8 is in your path."
       exit 1
    fi
fi

echo "*** Preparing to install Wildfly $WILDFLY_VERSION under $TARGET"
mkdir $TARGET
pushd $TARGET

echo "*** Getting Wildfly $WILDFLY_VERSION"
if type wget &>/dev/null
then
    CMD="wget"
else
    CMD="curl --remote-name"
fi

$CMD http://download.jboss.org/wildfly/$WILDFLY_VERSION/wildfly-$WILDFLY_VERSION.zip 

unzip wildfly-$WILDFLY_VERSION.zip

pushd wildfly-$WILDFLY_VERSION
export JBOSS_HOME=`pwd`

popd

echo "*** Getting WELD CDI Patch"
$CMD http://download.jboss.org/weld/$WELD_CDI_PATCH_VERSION/wildfly-$WILDFLY_VERSION-weld-$WELD_CDI_PATCH_VERSION-patch.zip

$JBOSS_HOME/bin/jboss-cli.sh --command="patch apply wildfly-$WILDFLY_VERSION-weld-$WELD_CDI_PATCH_VERSION-patch.zip"

echo "*** Getting Mercury WildFly configuration..."
if [ ! -d "wildflyconfig" ]
then
    git clone ssh://git@stash.broadinstitute.org:7999/gpin/mercury.git
fi

pushd mercury/JBossConfig
echo "*** Building and deploying WildFly configuration..."
echo "*********************************"
echo "********** git checkout develop instead of GPLIM-3937_Java8 after go-live **********"
echo "*********************************"
git checkout GPLIM-3937_Java8
mvn install
popd

echo "*** Done setting up environment for Mercury Wildfly $WILDFLY_VERSION!"
echo "*** JBOSS_HOME = $JBOSS_HOME"

echo "*** Cleaning up"
rm -v wildfly-$WILDFLY_VERSION.zip
rm -v wildfly-$WILDFLY_VERSION-weld-$WELD_CDI_PATCH_VERSION-patch.zip

popd
