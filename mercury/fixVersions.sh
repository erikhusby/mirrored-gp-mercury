#!/bin/bash -l
#
# Fix the versions of limsThrift and bspclient for the release. Assumes we are on the release branch.
#
unuse Maven-2.2
unuse Java-1.6-Prodinfo
unuse Git-2.0
use Maven-3.1
use Java-1.7
use Git-1.8

mvn -DincludesList=lims:limsThrift:jar::1.0-SNAPSHOT versions:lock-snapshots versions:resolve-ranges

BRANCH=`git branch | grep release`
git branch
git status
git commit -m "REL-714 Fixing versions of limsThrift and bspclient." pom.xml
git push origin ${BRANCH:2}
