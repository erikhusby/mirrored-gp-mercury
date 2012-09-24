package org.broadinstitute.gpinformatics.infrastructure.jmx;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Startup;
import javax.inject.Singleton;
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
@Singleton
@Startup
public abstract class AbstractCacheControl implements CacheControlMXBean {

    private MBeanServer platformMBeanServer;

    private ObjectName objectName;

    private static final String APP_NAME = "Mercury";

    public AbstractCacheControl() {}

    @PostConstruct
    public void registerInJMX() {
        try {
            objectName = new ObjectName(APP_NAME + ":class=" + this.getClass().getName());
            platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
            if (!platformMBeanServer.isRegistered(objectName)) {
                platformMBeanServer.registerMBean(this, objectName);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Problem during registration of Monitoring into JMX:" + e);
        }
    }

    @PreDestroy
    public void unregisterFromJMX() {
        try {
            platformMBeanServer.unregisterMBean(this.objectName);
        } catch (Exception e) {
            throw new IllegalStateException("Problem during unregistration of Monitoring into JMX:" + e);
        }
    }
}
