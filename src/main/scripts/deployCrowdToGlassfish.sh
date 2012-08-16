#!/bin/bash -x
#
# Deploy SequeL Crowd to Glassfish
#
usage() {
    echo " $0 [-QA] [domain] "
    echo "Where -QA says configure the DevTestLoginModule as well as CrowdGlassfishLoginModule."
    echo "Where domain is the name of the Glassfish domain. Defaults to domain1"
    echo "Requires that AS_INSTALL be defined as the Glassfish installation directory"
    exit 1
}

[ -z "$AS_INSTALL" ] && usage
[ "$1" = "-help" ] && usage

if [ "$1" = "-QA" ] ; then
    QACONF=true
    shift
else
    QACONF=false
fi

if [ $# -eq 0 ] ; then
    DOMAIN="domain1"
else
    DOMAIN=$1
fi
#
$AS_INSTALL/bin/asadmin stop-domain $DOMAIN

#
# Fetch the necessary jars from the repository
curl http://prodinfosvn.broadinstitute.org:8000/m2-repository/com/atlassian/crowd/application/CrowdJaasLoginModule/1.3.1/CrowdJaasLoginModule-1.3.1.jar -o $AS_INSTALL/glassfish/lib/CrowdJassLoginModule-1.3.1.jar
curl http://prodinfosvn.broadinstitute.org:8000/m2-repository/org/broadinstitute/customRealm/1.0-SNAPSHOT/customRealm-1.0-SNAPSHOT.jar -o $AS_INSTALL/glassfish/lib/customRealm-1.0-SNAPSHOT.jar
#
#
if grep -q crowdCustomRealm  $AS_INSTALL/glassfish/domains/$DOMAIN/config/login.conf
then
    echo " $AS_INSTALL/glassfish/domains/$DOMAIN/config/login.conf already contains the crowdCustomRealm"
else
    if [ $QACONF ] ; then
	cat >> $AS_INSTALL/glassfish/domains/$DOMAIN/config/login.conf <<EOF
crowdCustomRealm{
        org.broadinstitute.custom.authentication.DevTestLoginModule sufficient;
        org.broadinstitute.custom.authentication.CrowdGlassfishLoginModule required;
};

EOF
    else
	cat >> $AS_INSTALL/glassfish/domains/$DOMAIN/config/login.conf <<EOF
crowdCustomRealm{
        org.broadinstitute.custom.authentication.CrowdGlassfishLoginModule required;
};


EOF
    fi
fi

$AS_INSTALL/bin/asadmin start-domain $DOMAIN
#
$AS_INSTALL/bin/asadmin create-auth-realm \
    --property jaas-context=crowdCustomRealm:crowd.server.url="https\://crowd.broadinstitute.org\:8443/crowd":application.name=pmbridge:application.password=7OdTlnQF:flatRoles=true \
    --classname org.broadinstitute.custom.authentication.GlassfishCustomRealm \
    crowd_custom_realm
#
#
