package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.Date;

@Singleton
@Startup
public class SAPDisableMaterialControl implements Serializable {

    private Log log = LogFactory.getLog(this.getClass());

    private MBeanServer platformMBeanServer;

    private ObjectName objectName;

    private static final String APP_NAME = "Mercury";

    private static final long serialVersionUID = 3994885065710066403L;

    @Inject
    ProductEjb productEjb;

    public SAPDisableMaterialControl() {
    }

    @Schedule(minute = "0", hour = "2", persistent = false)
    public void disableProducts() {
        productEjb.disableProductsFromDate(new Date());
    }

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
