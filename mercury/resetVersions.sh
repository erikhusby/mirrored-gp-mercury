#!/bin/bash -l
# 
# Reset the versions of limsThrift and bspclient that were fixed in release process.
#
# When the release is finished by jgitflow, the pom.xml is pushed to the develop branch 
# with the fixed versions of limsThrift and bspclient. We need to replaced the fixed
# versions with the shapshot and range versions.
#
unuse Maven-2.2
unuse Java-1.6-Prodinfo
unuse Git-2.0
use Maven-3.1
use Java-1.7
use Git-1.8

# Reset the versions of limsThrift and bspclient on the develop breanch
git branch
git status
git checkout develop
#
mvn versions:unlock-snapshots -DincludesList=lims:limsThrift:jar
#
# Want bspclient to be [3.1,) i.e. open ended.
sed --regexp-extended --in-place=bck  "s/(<bspclient\.version>)([1-9]+\.[1-9]+)\.[1-9]+/\1\[\2,\)/" pom.xml

git commit -m "REL-714 Reseting versions of limsThrift and bspclient." pom.xml
git push origin develop

