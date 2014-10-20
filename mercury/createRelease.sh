#!/bin/bash -l
#
# Perform a Production release of Mercury
#
# Usage: createRelease.sh
#
unuse Java-1.6-Prodinfo
unuse Maven-2.2
unuse Git-2.0
use Maven-3.1
use Java-1.7
use Git-1.8

if [ -d "release" ] ; then
    rm -rf release
fi
mkdir release

git clone ssh://git@stash.broadinstitute.org:7999/GPIN/mercury.git release
cd release/mercury

git checkout RCBUILD
# Determine current version numbers
VERSION=`groovy -e 'print new XmlParser().parse(new File("pom.xml")).version.text()'`
#
# Split the version into its parts
MAJOR=`expr $VERSION : '\([0-9]*\)'`
MINOR=`expr $VERSION : '[0-9]*\.\([0-9]*\)'`
REV=`expr $VERSION : '[0-9]*\.[0-9]*\(.*\)'`
REV=${REV#\.}
REV=${REV%-RC}
if [ "$REV" == "" ]
then
    REV="0"
fi
REV=`expr $REV + 1`

RCBRANCH="RC-$MAJOR.$MINOR"
git checkout $RCBRANCH
PRODVERSION=${VERSION%-RC}

git checkout --track -b $PRODVERSION
mvn versions:set -DnewVersion="$PRODVERSION"
git commit -m "REL-714 Setting Production Release version $PRODVERSION" pom.xml
git push origin :PROD
git tag -a -m "Current Production" --force PROD HEAD
git push origin $PRODVERSION
git push origin --tags

git checkout $RCBRANCH

NEXTRCVERSION="$MAJOR.$MINOR.$REV-RC"
mvn versions:set -DnewVersion="$NEXTRCVERSION"
git commit -m "REL-714 Setting RC Version $NEXTRCVERSION" pom.xml
git push origin :RCBUILD
git tag -a -m "Current RC" --force RCBUILD $RCBRANCH
git push origin $RCBRANCH
git push origin --tags

#
# Notify the HipChat room
./notifyHipChat.sh "Mercury Production branch $PRODVERSION created." 
