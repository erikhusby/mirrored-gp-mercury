#!/bin/bash 
#
# Usage: tagBuild.sh TAG ${bamboo.repository.revision.number} [branch]
if [ $# -ge 2 ] ; then
    TAG=$1
    BRRN=$2
    if [ -d "target" ] ; then
        if [ -d "target/checkout" ] ; then
            rm -rf target/checkout
        fi
    else
        mkdir -v target
    fi

    if [ $# -eq 3 ] ; then
        BRANCHNAME=$3
    else
        BRANCHNAME="master"
    fi
    mkdir -v target/checkout
    git clone ssh://git@stash.broadinstitute.org:7999/GPIN/mercury.git target/checkout
    cd target/checkout
    git checkout $BRANCHNAME
    git push origin :$TAG
    git tag -a -m "Successful build" --force $TAG $BRRN
    git push origin --tags
else
    echo "Usage: $0  TAG bamboo.repository.revision.number [branch]"
    echo "You must specify tag name and the revision number associated with the build"
    echo "the branch is optional and defaults to master."
    exit 1
fi