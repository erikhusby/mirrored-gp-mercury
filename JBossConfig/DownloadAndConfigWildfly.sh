#!/bin/bash
#
usage() {
    cat <<EOF

Usage: $0 parentDirectory
Wildfly is installed in a directory under the parentDirectory.
Mercury will be temporarily cloned under the parentDirectory.
EOF
}

TARGET=$1
if [ -z $TARGET ]
then
   usage
   exit 1
fi

WILDFLY_VERSION='17.0.1.Final'
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

$CMD https://download.jboss.org/wildfly/$WILDFLY_VERSION/wildfly-$WILDFLY_VERSION.zip 
unzip wildfly-$WILDFLY_VERSION.zip

pushd wildfly-$WILDFLY_VERSION
export JBOSS_HOME=`pwd`

popd

echo "*** Getting Mercury WildFly configuration..."
if [ ! -d "mercury/JBossConfig" ]
then
    git clone ssh://git@stash.broadinstitute.org:7999/gpin/mercury.git
fi

pushd mercury/JBossConfig
echo "*** Building and deploying WildFly configuration..."
echo "*********************************"
echo "********** git checkout develop after go-live **********"
echo "*********************************"
git checkout GPLIM-6742_wildfly17
mvn install
popd

echo "*** Done setting up environment for Mercury Wildfly $WILDFLY_VERSION"
echo "*** JBOSS_HOME = $JBOSS_HOME"

echo "*** Cleaning up"
rm -v wildfly-$WILDFLY_VERSION.zip
rm -rf mercury

popd
