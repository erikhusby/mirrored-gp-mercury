package org.broadinstitute.gpinformatics.infrastructure.bsp.plating;

import org.broadinstitute.bsp.client.collection.BspGroupCollectionManager;
import org.broadinstitute.bsp.client.container.BspContainerManager;
import org.broadinstitute.bsp.client.container.ContainerManager;
import org.broadinstitute.bsp.client.sample.BspSampleManager;
import org.broadinstitute.bsp.client.sample.SampleManager;
import org.broadinstitute.bsp.client.site.BspSiteManager;
import org.broadinstitute.bsp.client.users.BspUserManager;
import org.broadinstitute.bsp.client.users.UserManager;
import org.broadinstitute.bsp.client.workrequest.BspWorkRequestManager;
import org.broadinstitute.bsp.client.workrequest.WorkRequestManager;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * A CDI bean created by a producer
 * @see org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer
 */
public class BSPManagerFactoryImpl implements BSPManagerFactory {

    private BSPConfig params;

    private Object create(Class<?> clazz) {
        try {
            Constructor<?> constructor = clazz.getConstructor(String.class, Integer.class, String.class, String.class, Boolean.class);
            return constructor.newInstance(BSPConfig.getHttpScheme()+params.getHost(), params.getPort(),
                    params.getLogin(), params.getPassword(),Boolean.FALSE);
        } catch (SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException |
                IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public BSPManagerFactoryImpl(BSPConfig params) {
        this.params = params;
    }

    @Override
    public WorkRequestManager createWorkRequestManager() {
        return (WorkRequestManager) create(BspWorkRequestManager.class);
    }

    @Override
    public ContainerManager createContainerManager() {
        return (ContainerManager) create(BspContainerManager.class);
    }

    @Override
    public UserManager createUserManager() {
        return (UserManager) create(BspUserManager.class);
    }

    @Override
    public SampleManager createSampleManager() {
        return (SampleManager) create(BspSampleManager.class);
    }

    @Override
    public BspSiteManager createSiteManager() {
        return (BspSiteManager) create(BspSiteManager.class);
    }

    @Override
    public BspGroupCollectionManager createGroupCollectionManager() {
        return (BspGroupCollectionManager) create(BspGroupCollectionManager.class);
    }
}
