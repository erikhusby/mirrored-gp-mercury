#!/bin/bash -l
# 
# Reset the versions of limsThrift and bspclient that were fixed in release process.
#
# When the release is finished by jgitflow, the pom.xml is pushed to the develop branch 
# with the fixed versions of limsThrift and bspclient. We need to replaced the fixed
# versions with the shapshot and range versions.
#
# This procedure is used in the Bamboo builds that run as user releng where the default
# versions are not correct for the Mercury environment. In addition, the order is important
# because of problems in the dot kit that screws up the PATH after the use Maven-3.1 command.
#
unuse Maven-2.2
unuse Java-1.6-Prodinfo
unuse Git-2.0
use Maven-3.1
use Java-1.7
use Git-1.8

# Reset the versions of limsThrift and bspclient on the develop branch
git branch
git status
git checkout develop
#
# Transform the timestamped version of limsThrift-1.0-20121210.154738-35.jar to a snapshot version like limsThrift-1.0-SNAPSHOT.jar
mvn versions:unlock-snapshots -DincludesList=lims:limsThrift:jar
#
# The production release bspclient versions are in the form 1.3.60
# For development we want bspclient version to be [1.3,) i.e. open ended.
# So look for <bspclient.version>1.3.60</bspclient.version> and transform it to <bspclient.version>[1.3,)</bspclient.version>
#
sed --regexp-extended --in-place=bck  "s/(<bspclient\.version>)([0-9]+\.[0-9]+)\.[0-9]+/\1\[\2,\)/" pom.xml

git commit -m "REL-714 Reseting versions of limsThrift and bspclient." pom.xml
git push origin develop

