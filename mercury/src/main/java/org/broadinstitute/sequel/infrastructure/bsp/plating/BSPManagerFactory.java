package org.broadinstitute.sequel.infrastructure.bsp.plating;

import org.broadinstitute.bsp.client.container.ContainerManager;
import org.broadinstitute.bsp.client.users.UserManager;
import org.broadinstitute.bsp.client.workrequest.WorkRequestManager;

public interface BSPManagerFactory {

    public WorkRequestManager createWorkRequestManager();

    public ContainerManager createContainerManager();

    public UserManager createUserManager();

}
