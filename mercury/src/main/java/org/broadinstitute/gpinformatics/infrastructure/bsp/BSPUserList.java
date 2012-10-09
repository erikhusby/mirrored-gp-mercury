package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Application wide access to BSP's user list. The list is currently cached once at application startup. In the
 * future, we may want to rebuild the list regularly to account for changes to the user database.
 */
@Named
// MLC @ApplicationScoped breaks the test, as does @javax.ejb.Singleton.  @javax.inject.Singleton is the CDI version
// and does appear to work.  Much to learn about CDI still...
@Singleton
public class BSPUserList {

    private final List<BspUser> users;

    /**
     * @return list of bsp users, sorted by lastname, firstname, username, email.
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
    public BSPUserList(BSPManagerFactory bspManagerFactory) {
        List<BspUser> rawUsers = bspManagerFactory.createUserManager().getUsers();
        Collections.sort(rawUsers, new Comparator<BspUser>() {
            @Override
            public int compare(BspUser o1, BspUser o2) {
                // FIXME: need to figure out what the correct sort criteria are.
                CompareToBuilder builder = new CompareToBuilder();
                builder.append(o1.getLastName(), o2.getLastName());
                builder.append(o1.getFirstName(), o2.getFirstName());
                builder.append(o1.getUsername(), o2.getUsername());
                builder.append(o1.getEmail(), o2.getEmail());
                return builder.build();
            }
        });
        users = ImmutableList.copyOf(rawUsers);
    }
}
