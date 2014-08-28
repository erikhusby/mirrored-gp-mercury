#!/bin/bash -l
#
# Perform a Production release of Mercury
#
# Usage: Release.sh
#
unuse Java-1.6-Prodinfo
unuse Maven-2.2
unuse Git-2.0

use Maven-3.1
use Java-1.7
use Git-1.8
which mvn
which java
which git

set -o verbose
if [ -d "release" ] ; then
    rm -rf release
fi
mkdir release

git clone ssh://git@stash.broadinstitute.org:7999/GPIN/mercury.git release
cd release/mercury

#
# Determine current version numbers
VERSION=`groovy -e 'print new XmlParser().parse(new File("pom.xml")).version.text()'`
#
# Split the version into its parts
MAJOR=`expr $VERSION : '\([0-9]*\)'`
MINOR=`expr $VERSION : '.*\.\([0-9]*\)'`

NEXTMINOR=`expr $MINOR + 1`
NEXTVERSION="$MAJOR.$NEXTMINOR-SNAPSHOT"
RCVERSION="$MAJOR.$MINOR-RC"
RCBRANCH="RC-$MAJOR.$MINOR"

cat <<EOF
Creating Release Candidate Branch $RCBRANCH
Setting version to $RCVERSION

Updating master to version $NEXTVERSION

EOF

# Make the RC Branch a remote tracking branch
git checkout  --track -b $RCBRANCH
mvn versions:set -DnewVersion=$RCVERSION
mvn -DincludesList=lims:limsThrift:jar::1.0-SNAPSHOT versions:lock-snapshots versions:resolve-ranges
mvn -f rest-pom.xml versions:set -DnewVersion=$RCVERSION
mvn -f rest-pom.xml versions:resolve-ranges
git commit -m "REL-714 Setting RC version to $RCVERSION" -a
# Create the RCBUILD floating tag (but get rid of previous version first)
git push origin :RCBUILD
git tag -a -m "Current RC " --force RCBUILD $RCBRANCH
git push origin $RCBRANCH
git push origin --tags

# Switch back to the master and update the pom
git checkout master
mvn versions:set -DnewVersion=$NEXTVERSION
mvn -f rest-pom.xml versions:set -DnewVewrsion=$NEXTVERSION
# Commit the master
# push the master
git commit -m "REL-714 Setting master version to $NEXTVERSION" -a 
git fetch origin +master
git push origin master

# Notify the HipChat room
./notifyHipChat.sh "Mercury Release Candidate branch $RCBRANCH created."
