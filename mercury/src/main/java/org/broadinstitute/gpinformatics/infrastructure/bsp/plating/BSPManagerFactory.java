package org.broadinstitute.gpinformatics.infrastructure.bsp.plating;

import org.broadinstitute.bsp.client.collection.BspGroupCollectionManager;
import org.broadinstitute.bsp.client.container.ContainerManager;
import org.broadinstitute.bsp.client.sample.SampleManager;
import org.broadinstitute.bsp.client.site.BspSiteManager;
import org.broadinstitute.bsp.client.users.UserManager;
import org.broadinstitute.bsp.client.workrequest.WorkRequestManager;

import java.io.Serializable;

public interface BSPManagerFactory extends Serializable {

    WorkRequestManager createWorkRequestManager();

    ContainerManager createContainerManager();

    UserManager createUserManager();

    SampleManager createSampleManager();

    BspSiteManager createSiteManager();

    BspGroupCollectionManager createGroupCollectionManager();
}
