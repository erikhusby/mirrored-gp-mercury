#!/bin/bash -v
#
# Perform a Production release of Mercury
#
# Usage: Release.sh
#
#set -o verbose
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
git commit -m "REL-714 Setting RC version to $RCVERSION" pom.xml
# Create the RCBUILD floating tag (but get rid of previous version first)
git push origin :RCBUILD
git tag -a -m "Current RC " --force RCBUILD $RCBRANCH
git push origin $RCBRANCH
git push origin --tags

# Switch back to the master and update the pom
git checkout master
mvn versions:set -DnewVersion=$NEXTVERSION
# Commit the master
# push the master
git commit -m "REL-714 Setting master version to $NEXTVERSION" pom.xml
git fetch origin +master
git push origin master

# Notify the HipChat room
./notifyHipChat.sh "Mercury Release Candidate branch $RCBRANCH created."
