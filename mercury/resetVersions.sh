#!/bin/bash
#
usage() {
    cat <<EOF
Usgae: $0 [-n]

 Reset the versions of limsThrift and bspclient that were fixed in release process.

OPTIONS:
	-h show this message.
	-n means no command and push to the repo.

Notes:

 When the release is finished by jgitflow, the pom.xml is pushed to the develop branch 
 with the fixed versions of limsThrift and bspclient. We need to replaced the fixed
 versions with the shapshot and range versions.

 This procedure is used in the Bamboo builds that run as user releng where the default
 versions are not correct for the Mercury environment. In addition, the order is important
 because of problems in the dot kit that screws up the PATH after the use Maven-3.1 command.


EOF
}

source /broad/tools/scripts/useuse
unuse Maven-2.2
unuse Java-1.6-Prodinfo
unuse Git-2.0
use Maven-3.1
use Java-1.7
use Git-1.8
NOCOMMIT=0

while getopts "hn" OPTION
do
    case $OPTION in
	h) usage
	   exit 1
	   ;;
	n) NOCOMMIT=1
	   ;;
	[?]) usage
	     exit 1
	     ;;
    esac
done

# Reset the versions of limsThrift and bspclient on the develop branch
# Verify that we are on the develop branch.
bn=$(git symbolic-ref HEAD)
bn=${bn##refs/heads/}
if [ "$bn" != "develop" ]
then
    echo "*** Must be on the develop branch to reset the versions in the pom.xml"
    exit 1
fi
if [ ! -e "pom.xml" ]
then
    echo "*** No pom.xml in current directory"
    exit 1
fi
#
# Transform the timestamped version of limsThrift-1.0-20121210.154738-35.jar to a snapshot version like limsThrift-1.0-SNAPSHOT.jar
mvn versions:unlock-snapshots -DincludesList=lims:limsThrift:jar
#
# The production release bspclient versions are in the form 1.3.60
# For development we want bspclient version to be [1.3,) i.e. open ended.
# So look for <bspclient.version>1.3.60</bspclient.version> and transform it to <bspclient.version>[1.3,)</bspclient.version>
#
SEDOPTS="--regexp-extended --in-place=bck"

if [ "`uname`" == "Darwin" ]
then
    SEDOPTS="-E -i bck"
fi
       
sed $SEDOPTS "s/(<bspclient\.version>)([0-9]+\.[0-9]+)\.[0-9]+/\1\[\2,\)/" pom.xml

if [[ $NOCOMMIT -eq 0 ]]
then
    git commit -m "REL-714 Reseting versions of limsThrift and bspclient." pom.xml
    git push origin develop
fi

