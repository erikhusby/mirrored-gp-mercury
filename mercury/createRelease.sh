#!/bin/bash
#
# Perform a Production release of Mercury
#
# Usage: Release.sh
#

if [ -d "release" ] ; then
    rm -rf release
fi
mkdir release

git clone ssh://git@stash.broadinstitute.org:7999/GPIN/mercury.git release
cd release
git checkout QA
git checkout -b QA_PROD
cd mercury
mvn -P\!DefaultProfile --batch-mode release:prepare release:perform
if [ $? -eq 0 ] ; then
    git status
    git branch -a
    git tag -l
    pushd target/checkout
    git tag -a -m "Current Production" --force PROD HEAD
    git push origin PROD
    popd
    git checkout master
    git merge QA_PROD -m "REL-000 Update pom.xml with new version"
    git branch -d QA_PROD
    git push origin :QA_PROD
    git fetch origin +master
    git push origin master
else
    echo "Release failed"
    exit 1
fi

