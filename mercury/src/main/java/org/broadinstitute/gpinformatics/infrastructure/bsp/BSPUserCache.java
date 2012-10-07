package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.google.common.collect.ImmutableList;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.users.UserManager;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Application wide cache of BSP's user list.
 */
@Named
// MLC @ApplicationScoped breaks the test, as does @javax.ejb.Singleton.  @javax.inject.Singleton is the CDI version
// and does appear to work.  Much to learn about CDI still...
@Singleton
public class BSPUserCache {

    private final List<BspUser> users;


    /**
     * @return list of bsp users, sorted by username.
     */
    public List<BspUser> getUsers() {
        return users;
    }

    /**
     * @param id key of user to look up
     * @return if found, the user, otherwise null
     */
    public BspUser getById(long id) {
        // Could improve performance here by storing users in a TreeMap.  Wait until performance becomes
        // an issue, then fix.
        for (BspUser user : users) {
            if (user.getUserId() == id) {
                return user;
            }
        }
        return null;
    }

    @Inject
    // MLC constructor injection appears to be required to get a BSPManagerFactory injected???
    public BSPUserCache(BSPManagerFactory bspManagerFactory) {

        // FIXME: Update to use correct BSP API call once it's present.
        UserManager userManager = bspManagerFactory.createUserManager();
        List<BspUser> rawUsers = userManager.getPrimaryInvestigators();
        Collections.sort(rawUsers, new Comparator<BspUser>() {
            @Override
            public int compare(BspUser o1, BspUser o2) {
                // FIXME: need to figure out what the correct sort criteria are.

                String u1 = o1.getUsername();
                String u2 = o2.getUsername();

                // MLC these null checks proved to be necessary, though that definitely doesn't seem right.
                // Need to talk to Jason
                if (u1 == null && u2 == null) {
                    return 0;
                }

                if (u1 == null) {
                    return -1;
                }

                if (u2 == null) {
                    return 1;
                }

                return u1.compareTo(u2);
            }
        });
        users = ImmutableList.copyOf(rawUsers);
    }
}
