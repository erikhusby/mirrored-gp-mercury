package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
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

    private static long userIdSeq = 101010101L;

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

    /**
     * Returns the BSP user for the given username, or null if no user exists with that name. Username comparison
     * ignores case.
     *
     * @param username the username to look for
     * @return the BSP user or null
     */
    public BspUser getByUsername(String username) {
        for (BspUser user : users) {
            if (user.getUsername().equalsIgnoreCase(username)) {
                return user;
            }
        }
        return null;
    }

    /**
     * Returns a list of users whose first name, last name, or username match the given query.
     *
     * @param query the query string to match on
     * @return a list of matching users
     */
    public List<BspUser> find(String query) {
        String lowerQuery = query.toLowerCase();
        List<BspUser> results = new ArrayList<BspUser>();
        for (BspUser user : users) {
            if (user.getFirstName().toLowerCase().contains(lowerQuery) || user.getLastName().toLowerCase().contains(lowerQuery) || user.getUsername().contains(lowerQuery)) {
                results.add(user);
            }
        }
        return results;
    }

    @Inject
    // MLC constructor injection appears to be required to get a BSPManagerFactory injected???
    public BSPUserList(BSPManagerFactory bspManagerFactory) {
        List<BspUser> rawUsers = bspManagerFactory.createUserManager().getUsers();

        if (rawUsers != null) {
            addQADudeUsers(rawUsers);
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
        } else {
            users = new ArrayList<BspUser>();
        }
    }

    private void addQADudeUsers(List<BspUser> users) {
        users.add(makeBspUser("QADudeTest", "QADude", "Test", "qadudetest@broadinstitute.org"));
        users.add(makeBspUser("QADudePM", "QADude", "PM", "qadudepm@broadinstitute.org"));
        users.add(makeBspUser("QADudeLU", "QADude", "LU", "qadudelu@broadinstitute.org"));
        users.add(makeBspUser("QADudeLM", "QADude", "LM", "qadudelm@broadinstitute.org"));
    }

    private synchronized BspUser makeBspUser(String username, String firstName, String lastName, String email) {
        BspUser user = new BspUser();
        user.setUserId(userIdSeq++);
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        return user;
    }
}
