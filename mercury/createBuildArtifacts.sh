#!/bin/bash
#
#
usage() {
cat <<EOF
 Tar up the build artfiacts
 Usage: $0 TAG
 Where TAG is one of RC, PROD
EOF
}

if [ "$#" -eq 1 ]
then
    tar -cv -f Mercury$1.tar mercury/target/Mercury-*.war mercury/pom.xml mercury/src/main/db/*.* mercury/target/test-classes/*ds.xml mercury/*.sh
else
    usage
    exit 1
fi
