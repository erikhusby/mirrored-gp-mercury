
set "SSL_OPTS=-Djavax.net.ssl.keyStore=/%JBOSS_HOME%/.keystore -Djavax.net.ssl.keyStorePassword=changeit -Djavax.net.ssl.trustStore=%JBOSS_HOME%/.keystore -Djavax.net.ssl.trustStorePassword=changeit "

set "JAVA_OPTS=%JAVA_OPTS% %SSL_OPTS%"