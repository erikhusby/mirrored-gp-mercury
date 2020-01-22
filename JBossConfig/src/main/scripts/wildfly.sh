#!/bin/bash
#
#
usage() {
    cat <<EOF

    Usage: $0 start|stop|restart|status|parameters [hostname]

    Controls a Wildlfy server.

    start -- starts the server in the background using the standalone-full.xml configuration file. Stores the pid of the server in
        JBOSS_HOME/wildfly-standalone.pid. If hostname is specified, then binds the management and public interfaces to hostname.
        Otherwise it will bind the interfaces to the current host's ip addresses. Specify localhost if you wish to bind
        the interfaces to 127.0.0.1.
    stop -- Attempts to stop a running server.
    restart -- Stops and then starts the server.
    status -- Reports on the state of the server.
    parameters -- lists environment variables used.

    If JBOSS_HOME is not set, it is set to the directory of this procedure.
    
EOF
}

if [ -e "/broad/software/scripts/useuse" ]
then
    source /broad/software/scripts/useuse
    use Java-1.8
fi
#
# Setup the options
#
setOptions() {

    if [ "${JBOSS_HOME}x" == "x" ]
    then
        JBOSS_HOME=`dirname ${BASH_SOURCE}`
    fi

    # JAVA_OPTS - from standalone.conf
    SERVER_OPTS="--server-config=standalone-full.xml -b=$HOSTIP -bmanagement=$HOSTIP"
    JAVA="$JAVA_HOME/bin/java"
    SERVER_HOME="$JBOSS_HOME/standalone"
    SERVER_PIDFILE="$SERVER_HOME/wildfly-standalone.pid"
    SERVER_LOG_DIR="$SERVER_HOME/log"
    SERVER_CONSOLE_LOG="$SERVER_HOME/log/jboss-as/console.log"
    SERVER_CONFIG_DIR="$SERVER_HOME/configuration"
    JBOSS_MODULEPATH="$JBOSS_HOME/modules"
}

#
# Start the server
#
start() {
    isRunning
    if [ $? -eq 0 ]
    then
	mkdir -p $(dirname $SERVER_CONSOLE_LOG)
	cat /dev/null > $SERVER_CONSOLE_LOG
	mkdir -p $(dirname $SERVER_PIDFILE)

	#    $JBOSS_HOME/bin/standalone.sh -c standalone-modeshape.xml
	if [ -e "$JBOSS_HOME/bin/standalone.conf" ]
	then
            echo "Loading $JBOSS_HOME/bin/standalone.conf"
            . $JBOSS_HOME/bin/standalone.conf
	fi
	if [ -e "$SERVER_HOME/standalone.conf" ]
	then
            echo "Loading $SERVER_HOME/standalone.conf"
            . $SERVER_HOME/standalone.conf
	fi

	parameters
	echo "Cleaning out tmp directory "
	rm -rf $SERVER_HOME/tmp
	echo -n "Starting WildFly: "
	RUN_CMD="$JAVA -D\"[Standalone]\" $JAVA_OPTS -Dorg.jboss.boot.log.file=$SERVER_LOG_DIR/server.log -Dlogging.configuration=file:$SERVER_CONFIG_DIR/logging.properties -jar $JBOSS_HOME/jboss-modules.jar -mp ${JBOSS_MODULEPATH} org.jboss.as.standalone -Djboss.home.dir=$JBOSS_HOME  -Djboss.server.base.dir=$SERVER_HOME $SERVER_OPTS"

	echo "Starting Wildfly"
	sh -c "nohup sh -c \" cd $BASEDIR && exec $RUN_CMD 2>&1 \" >>$SERVER_CONSOLE_LOG & umask 0000 ; echo \$! > $SERVER_PIDFILE "

	echo -n " (pid `cat $SERVER_PIDFILE`) "
	echo " "
    else
        echo "Wildfly is running, pid=`cat $SERVER_PIDFILE`"
   fi
}
#
# Stop the server if it is running
#
stop() {
    isRunning
    if [ $? -eq 0 ]
    then
        echo "Wildfly does not appear to be running."
    else
        echo "Wildfly is running, pid=`cat $SERVER_PIDFILE`, sending shutdown"
        $JBOSS_HOME/bin/jboss-cli.sh --connect --controller="$HOSTIP" command=:shutdown
    fi
    sleep 10
    isRunning
    if [ $? -eq 0 ]
    then
	rm -f $SERVER_PIDFILE
    else
	echo "Wildfly did not stop, pid=`cat $SERVER_PIDFILE`"
    fi
}

#
# Show the various parameters
#
parameters() {
    cat <<EOF
HOSTIP=$HOSTIP
JAVA_HOME="$JAVA_HOME"
JAVA_OPTS="$JAVA_OPTS"
JBOSS_HOME="$JBOSS_HOME"
SERVER_HOME="$SERVER_HOME"
SERVER_PIDFILE="$SERVER_PIDFILE"
SERVER_CONSOLE_LOG="$SERVER_CONSOLE_LOG"
SERVER_LOG_DIR="$SERVER_LOG_DIR"
SERVER_CONFIG_DIR="$SERVER_CONFIG_DIR"
SERVER_OPTS="$SERVER_OPTS"
JBOSS_MODULEPATH="$JBOSS_MODULEPATH"

EOF
}

#
# Check if server is running
#
isRunning() {
    if [ -f "$SERVER_PIDFILE" ] 
    then
        read ppid < $SERVER_PIDFILE
        if [ `ps -p $ppid 2> /dev/null | grep -c $ppid 2> /dev/null` -eq '1' ]
        then
            IS_RUNNING=$ppid
        else
            IS_RUNNING=0
	    rm $SERVER_PIDFILE
        fi
    else
        IS_RUNNING=0
    fi
    return $IS_RUNNING
}
#
# Check the server status
#
status() {
    isRunning
    if [ $? -eq 0 ]
    then
        echo "Wildfly does not appear to be running."
    else
        echo "Wildfly is running, pid=`cat $SERVER_PIDFILE`"
        read ppid < $SERVER_PIDFILE
        ps -f $ppid
    fi
}

#
#
if [ $# -eq 0 ] 
then
    usage
    exit 1
else 
    OPERATION=$1
    shift

    if [ $# -gt 0 ]
    then
	    HOSTIP=$1
    else
	    if [ "Darwin" == `uname` ]
	    then
	        HOSTIP=`ifconfig | grep "inet " | grep -v 127.0.0.1 | awk 'NR==1 {print $2}'`
	    else
	        HOSTIP=`hostname -I`
	    fi
    fi
fi


setOptions

# Perform the command
#
case "$OPERATION" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        stop
        start
        ;;
    status)
        status
        ;;
    parameters)
	    parameters
	    ;;
    *)
        usage
        exit 1
        ;;
esac

exit 0
