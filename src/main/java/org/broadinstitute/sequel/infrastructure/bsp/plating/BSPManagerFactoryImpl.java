package org.broadinstitute.sequel.infrastructure.bsp.plating;

import org.broadinstitute.bsp.client.container.BspContainerManager;
import org.broadinstitute.bsp.client.container.ContainerManager;
import org.broadinstitute.bsp.client.users.BspUserManager;
import org.broadinstitute.bsp.client.users.UserManager;
import org.broadinstitute.bsp.client.workrequest.BspWorkRequestManager;
import org.broadinstitute.bsp.client.workrequest.WorkRequestManager;
import org.broadinstitute.sequel.infrastructure.bsp.BSPConnectionParameters;

import javax.inject.Inject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


public class BSPManagerFactoryImpl implements BSPManagerFactory {

    @Inject
    private BSPConnectionParameters params;

    
    private Object create(Class<?> clazz) {
        try {
            Constructor<?> constructor = clazz.getConstructor(String.class, Integer.class, String.class, String.class);
            return constructor.newInstance(params.getHostname(), params.getPort(), params.getSuperuserLogin(), params.getSuperuserPassword());
        }
        catch (SecurityException e) {
            throw new RuntimeException(e);
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
        catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }


    public WorkRequestManager createWorkRequestManager() {
        return (WorkRequestManager) create(BspWorkRequestManager.class);
    }
    
    public ContainerManager createContainerManager() {
        return (ContainerManager) create(BspContainerManager.class);
    }
    
    public UserManager createUserManager() {
        return (UserManager) create(BspUserManager.class);
    }
    
}
