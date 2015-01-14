#!/bin/bash 
#
#
usage() {
    cat <<EOF

usage: $0 host user password domainName domainPassword

Sets up the JBoss DS Security domain for Mercury

host is the name of the host running the JBoss server.
user is an administration username.
password is the user's password.

domainName is the domain to be modified. If it already exists, it will be removed first, and then created thusly:

<security-domain name="domainName" cache-type="default">
    <authentication>
        <login-module code="org.picketbox.datasource.security.SecureIdentityLoginModule" flag="required">
            <module-option name="username" value="mercury"/>
            <module-option name="password" value="5dfc52b51bd35553df8592078de921bc"/>
            <module-option name="managedConnectionFactoryName" value="jboss.jca:service=LocalTxCM,name=mercury_pool"/>
        </login-module>
    </authentication>
</security-domain>

EOF
}
#
# Ensure that we have a $JBOSS_HOME
if [ -z "$JBOSS_HOME" ]
then
    echo "Please define JBOSS_HOME to point to the JBoss installation."
    exit 1
fi

if [ $# -eq 5 ]
then
    HOSTIP=$1
    shift
    USER=$1
    shift
    PASSWORD=$1
    shift
    DOMAINNAME=$1
    shift
    DOMAINPASSWORD=$1

    #
    # Encrypt the Domain password
    ENCODEDPASSWORD=`java -cp $JBOSS_HOME/modules/org/picketbox/main/picketbox-4.0.7.Final.jar:$JBOSS_HOME/modules/org/jboss/logging/main/jboss-logging-3.1.0.GA.jar org.picketbox.datasource.security.SecureIdentityLoginModule $DOMAINPASSWORD | cut -d : -f 2 | tr -d ' '`


    #
    # Remove the old version of the security domain if one exists and create the new one.
    #
    $JBOSS_HOME/bin/jboss-cli.sh --connect --controller=$HOSTIP --user=$USER --password=$PASSWORD  <<EOF 
/subsystem=security/security-domain=$DOMAINNAME:remove
/subsystem=security/security-domain=$DOMAINNAME/:add(cache-type=default)
/subsystem=security/security-domain=$DOMAINNAME/authentication=classic:add( \
    login-modules=[ \
        { \
           "code"=>"org.picketbox.datasource.security.SecureIdentityLoginModule", \
           "flag"=>"required", \
           "module-options"=>[ \
              ("username"=>"mercury"), \
              ("password"=>"$ENCODEDPASSWORD"), \
              ("managedConnectionFactoryName"=>"jboss.jca:service=LocalTxCM,name=mercury_pool") \
           ] \
        } \
    ] \
) {allow-resource-service-restart=true}

/subsystem=security/security-domain=$DOMAINNAME:read-resource(recursive=true)
quit

EOF
    
else
    usage
    exit 1
fi
