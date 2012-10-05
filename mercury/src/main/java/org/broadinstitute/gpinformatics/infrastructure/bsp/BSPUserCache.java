package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.google.common.collect.ImmutableList;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Application wide cache of BSP's user list.
 */
@Named
@ApplicationScoped
public class BSPUserCache {
    private final List<BspUser> users;

    @Inject
    private BSPManagerFactory bspManagerFactory;

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

    public BSPUserCache() {
        // FIXME: Update to use correct BSP API call once it's present.
        List<BspUser> rawUsers = bspManagerFactory.createUserManager().getPrimaryInvestigators();
        Collections.sort(rawUsers, new Comparator<BspUser>() {
            @Override
            public int compare(BspUser o1, BspUser o2) {
                // FIXME: need to figure out what the correct sort criteria are.
                return o1.getUsername().compareTo(o2.getUsername());
            }
        });
        users = ImmutableList.copyOf(rawUsers);
    }
}
