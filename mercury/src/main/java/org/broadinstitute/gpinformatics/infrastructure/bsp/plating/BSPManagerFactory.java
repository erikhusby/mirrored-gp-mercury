package org.broadinstitute.gpinformatics.infrastructure.bsp.plating;

import org.broadinstitute.bsp.client.container.ContainerManager;
import org.broadinstitute.bsp.client.users.UserManager;
import org.broadinstitute.bsp.client.workrequest.WorkRequestManager;

import java.io.Serializable;

public interface BSPManagerFactory extends Serializable {

    public WorkRequestManager createWorkRequestManager();

    public ContainerManager createContainerManager();

    public UserManager createUserManager();

}
