#!/bin/bash
#
usage() {
    cat <<EOF

Usage: $0 parentDirectory
Wildfly will be installed in a directory under parentDirectory.
EOF
}

TARGET=$1
if [ -z $TARGET ]
then
   usage
   exit 1
fi

WILDFLY_VERSION='17.0.1.Final'

# Uses the pom, crowd properties, and src files found in the current directory.
JBOSSCONFIG=`pwd`
if [ ! -e pom.xml ]; then
    echo Cannot find pom.xml
    exit 1;
fi

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

echo "*** Updating WildFly modules using JBossConfig install"
cp -r ${JBOSSCONFIG} JBossConfig
pushd JBossConfig
mvn install
popd

echo "*** Done setting up environment for Mercury Wildfly $WILDFLY_VERSION"
echo "*** JBOSS_HOME = $JBOSS_HOME"

echo "*** Cleaning up"
rm -v wildfly-$WILDFLY_VERSION.zip
rm -rf JBossConfig

popd
