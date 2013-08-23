#!/bin/bash
#
# Extract the Mercury version number from the pom.xml
# Determine current version numbers
VERSION=`groovy -e 'print new XmlParser().parse(new File("pom.xml")).version.text()'`

# Target is either DEV, RC or Production depending on the version.
CLASSIFIER=`expr $VERSION : '[0-9]*\.[0-9]*-\(.*\)'`

if [ "$CLASSIFIER" = "RC" ] 
then
    TARGET="MercuryRC"
elif [ "$CLASSIFIER" = "SNAPSHOT" ]
then
    TARGET="MercuryDEV"
else
    TARGET="Production"
fi
#
# Craft the HipChat message
./notifyHipChat.sh "Mercury $VERSION deployed to $TARGET."