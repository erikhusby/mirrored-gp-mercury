package org.broadinstitute.gpinformatics.infrastructure.jmx;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * JMX beans that subclass this class will be automatically
 * registered with the container.  They will be singletons
 * in the VM, and they will be automagically instantiated
 * and at app deployment time.
 *
 * In Jconsole, look for beans under the category {@link #APP_NAME}
 */
abstract class AbstractJMXRegister {

    private Log log = LogFactory.getLog(this.getClass());

    private MBeanServer platformMBeanServer;

    private ObjectName objectName;

    private static final String APP_NAME = "Mercury";

    @PostConstruct
    public void registerInJMX() {
        try {
            objectName = new ObjectName(APP_NAME + ":class=" + getClass().getName());
            platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
            if (!platformMBeanServer.isRegistered(objectName)) {
                platformMBeanServer.registerMBean(this, objectName);
            }
        } catch (Exception e) {
            String message = "Problem during registration of Monitoring into JMX";
            log.error(message, e);
            throw new IllegalStateException(message, e);
        }
    }

    @PreDestroy
    public void unregisterFromJMX() {
        try {
            platformMBeanServer.unregisterMBean(objectName);
        } catch (Exception e) {
            String message = "Problem during unregistration of Monitoring from JMX";
            log.error(message, e);
            throw new IllegalStateException(message, e);
        }
    }
}
