package org.broadinstitute.gpinformatics.infrastructure.bsp.plating;

import org.broadinstitute.bsp.client.container.ContainerManager;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.users.UserManager;
import org.broadinstitute.bsp.client.workrequest.WorkRequestManager;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import java.util.ArrayList;
import java.util.List;

@Stub
public class BSPManagerFactoryStub implements BSPManagerFactory {
    @Override
    public WorkRequestManager createWorkRequestManager() {
        return null;
    }

    @Override
    public ContainerManager createContainerManager() {
        return null;
    }

    @Override
    public UserManager createUserManager() {
        return new UserManager() {
            @Override
            public BspUser get(String s) {
                BspUser user = new BspUser();
                user.setEmail("xxx");
                user.setUserId(100L);
                user.setUsername("test");
                return user;
            }

            @Override
            public List<BspUser> getPrimaryInvestigators() {
                List<BspUser> users = new ArrayList<BspUser>();
                users.add(get(null));
                return users;
            }

            @Override
            public List<BspUser> getProjectManagers() {
                List<BspUser> users = new ArrayList<BspUser>();
                users.add(get(null));
                return users;
            }
        };
    }
}
