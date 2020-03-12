#!/bin/bash -l
#
# Fix the versions of limsThrift and bspclient for the release. Assumes we are on the release branch.
#
# This procedure is used in the Bamboo builds that run as user releng where the default
# versions are not correct for the Mercury environment. In addition, the order is important
# because of problems in the dot kit that screws up the PATH after the use Maven-3.1 command.
#
unuse Maven-2.2
unuse Java-1.6-Prodinfo
unuse Git-2.0
export MAVEN2_HOME=/prodinfo/prod3pty/apache-maven-3.6.3
PATH=$MAVEN2_HOME/bin:$PATH
use Java-1.8
use Git-2.5

mvn -DincludesList=lims:limsThrift:jar::1.0-SNAPSHOT versions:lock-snapshots versions:resolve-ranges

#
# Find current release branch which by convention is release/x.xx
# The gitflow process as implemented by the maven-jgitflow plugin insures that there is only one
# release branch in process at a time.
#
BRANCH=`git branch | grep release/`
git branch
git status
git commit -m "REL-714 Fixing versions of limsThrift and bspclient." pom.xml
# BRANCH is prefixed by "* " 
git push origin ${BRANCH:2}
