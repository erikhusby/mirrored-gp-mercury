#!/bin/bash
#
# Usage: tagBuild.sh TAG ${bamboo.repository.revision.number}
if [ $# -eq 2 ] ; then
    TAG=$1
    BRRN=$2
    if [ -d "target" ] ; then
        if [ -d "target/checkout" ] ; then
            rm -rf target/checkout
        fi
    else
        mkdir target
    fi
    mkdir target/checkout
    git clone ssh://git@stash.broadinstitute.org:7999/GPIN/mercury.git target/checkout
    cd target/checkout
    git tag -a -m "Successful build" --force $TAG $BRRN
    git push origin $TAG
else
    echo "Usage: $0 TAG bamboo.repository.revision.number"
    echo "You must specify the tag name and the revision number associated with the build"
    exit 1
fi