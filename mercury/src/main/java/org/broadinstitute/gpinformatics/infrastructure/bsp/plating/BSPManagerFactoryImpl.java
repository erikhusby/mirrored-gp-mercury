package org.broadinstitute.gpinformatics.infrastructure.bsp.plating;

import org.broadinstitute.bsp.client.container.BspContainerManager;
import org.broadinstitute.bsp.client.container.ContainerManager;
import org.broadinstitute.bsp.client.sample.BspSampleManager;
import org.broadinstitute.bsp.client.sample.SampleManager;
import org.broadinstitute.bsp.client.users.BspUserManager;
import org.broadinstitute.bsp.client.users.UserManager;
import org.broadinstitute.bsp.client.workrequest.BspWorkRequestManager;
import org.broadinstitute.bsp.client.workrequest.WorkRequestManager;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;

import javax.inject.Inject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@Impl
public class BSPManagerFactoryImpl implements BSPManagerFactory {

    @Inject
    private BSPConfig params;

    private Object create(Class<?> clazz) {
        try {
            Constructor<?> constructor = clazz.getConstructor(String.class, Integer.class, String.class, String.class);
            return constructor.newInstance(params.getHost(), params.getPort(), params.getLogin(), params.getPassword());
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public BSPManagerFactoryImpl () {
    }

    public BSPManagerFactoryImpl (BSPConfig params) {
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
}
