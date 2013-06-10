package org.broadinstitute.gpinformatics.infrastructure.bsp.plating;

import org.broadinstitute.bsp.client.container.BspContainerManager;
import org.broadinstitute.bsp.client.container.ContainerManager;
import org.broadinstitute.bsp.client.sample.BspSampleManager;
import org.broadinstitute.bsp.client.sample.SampleManager;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.users.BspUserManager;
import org.broadinstitute.bsp.client.users.UserManager;
import org.broadinstitute.bsp.client.workrequest.BspWorkRequestManager;
import org.broadinstitute.bsp.client.workrequest.WorkRequestManager;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;

import javax.inject.Inject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@Impl
public class BSPManagerFactoryImpl implements BSPManagerFactory {
    @Inject
    private BSPConfig params;

    private Object create(Class<?> clazz) {
        try {
            Constructor<?> constructor = clazz.getConstructor(String.class, Integer.class, String.class, String.class);
            return constructor.newInstance(params.getHost(), params.getPort(), params.getLogin(), params.getPassword());
        } catch (SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException |
                IllegalAccessException | InvocationTargetException e) {
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

    // FIXME: This is a stopgap measure until we implement a proper solution for CRSP Mercury.

    private static class CRSPUserManager implements UserManager {
        // CRSP users
        private enum Users {
            // Hardcode CRSP user list for now.
            jowalsh("jowalsh", "John", "Walsh", "jowalsh@broadinstitute.org"),
            hrafal("hrafal", "Howard", "Rafal", "hrafal@broadinstitute.org"),
            pdunlea("pdunlea", "Phil", "Dunlea", "pdunlea@broadinstitute.org"),
            pshapiro("pshapiro", "Phil", "Shapiro", "pshapiro@broadinstitute.org"),
            dinsmore("dinsmore", "Michael", "Dinsmore", "dinsmore@broadinstitute.org");

            private final long userId;
            private final String username;
            private final String firstName;
            private final String lastName;
            private final String email;

            private Users(String username, String firstName, String lastName, String email) {
                userId = ordinal();
                this.username = username;
                this.firstName = firstName;
                this.lastName = lastName;
                this.email = email;
            }

            private BspUser asBspUser() {
                BspUser bspUser = new BspUser();
                bspUser.setUserId(userId);
                bspUser.setUsername(username);
                bspUser.setFirstName(firstName);
                bspUser.setLastName(lastName);
                bspUser.setEmail(email);
                return bspUser;
            }
        }

        private static final List<BspUser> CRSP_USERS = new ArrayList<BspUser>() {{
            for (Users user : Users.values()) {
                add(user.asBspUser());
            }
        }};
        @Override
        public BspUser get(String s) {
            return null;
        }
        @Override
        public BspUser getByDomainUserId(Long aLong) {
            return null;
        }
        @Override
        public List<BspUser> getPrimaryInvestigators() {
            return null;
        }
        @Override
        public List<BspUser> getProjectManagers() {
            return null;
        }
        @Override
        public List<BspUser> getUsers() {
            return CRSP_USERS;
        }
    }

    @Override
    public UserManager createUserManager() {
        if (Deployment.isCRSP) {
            return new CRSPUserManager();
        }
        return (UserManager) create(BspUserManager.class);
    }

    @Override
    public SampleManager createSampleManager() {
        return (SampleManager) create(BspSampleManager.class);
    }
}
