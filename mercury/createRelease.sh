#!/bin/bash
#
# Perform a Production release of Mercury
#
# Usage: Release.sh
#

if [ -d "release" ] ; then
    rm -rf release
fi
mkdir target

git clone ssh://git@stash.broadinstitute.org:7999/GPIN/mercury.git target
cd target
git checkout QA
git checkout -b QA_PROD
cd mercury
mvn -P\!DefaultProfile --batch-mode release:prepare release:perform
git status
git branch -a
git tag -l
