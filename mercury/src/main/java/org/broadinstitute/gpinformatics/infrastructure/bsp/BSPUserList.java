package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.logging.Log;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.*;

/**
 * Application wide access to BSP's user list. The list is currently cached once at application startup. In the
 * future, we may want to rebuild the list regularly to account for changes to the user database.
 */
@Named
// MLC @ApplicationScoped breaks the test, as does @javax.ejb.Singleton.  @javax.inject.Singleton is the CDI version
// and does appear to work.  Much to learn about CDI still...
@Singleton
public class BSPUserList {

    @Inject
    private Log logger;

    @Inject
    private Deployment deployment;

    private List<BspUser> users;

    private boolean serverValid;

    public boolean isServerValid() {
        return serverValid;
    }

    /**
     * @return list of bsp users, sorted by lastname, firstname, username, email.
     */
    public List<BspUser> getUsers() {

        if (users == null) {
            refreshUsers();
        }

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
        String[] lowerQueryItems = query.toLowerCase().split("\\s");
        List<BspUser> results = new ArrayList<BspUser>();
        for (BspUser user : users) {
            boolean eachItemMatchesSomething = true;
            for (String lowerQuery : lowerQueryItems) {
                // If none of the fields match this item, then all items are not matched
                if (!anyFieldMatches(lowerQuery, user)) {
                    eachItemMatchesSomething = false;
                }
            }

            if (eachItemMatchesSomething) {
                results.add(user);
            }
        }

        return results;
    }

    private static boolean anyFieldMatches(String lowerQuery, BspUser user) {
        return user.getFirstName().toLowerCase().contains(lowerQuery) ||
            user.getLastName().toLowerCase().contains(lowerQuery) ||
            user.getUsername().contains(lowerQuery) ||
                user.getEmail().contains(lowerQuery);
    }

    private BSPManagerFactory bspManagerFactory;

    @Inject
    // MLC constructor injection appears to be required to get a BSPManagerFactory injected???
    public BSPUserList(BSPManagerFactory bspManagerFactory) {
        this.bspManagerFactory = bspManagerFactory;
        refreshUsers();
    }

    public synchronized void refreshUsers() {
        try {
            List<BspUser> rawUsers = bspManagerFactory.createUserManager().getUsers();
            serverValid = rawUsers != null;

            if (!serverValid) {
                // BSP is down
                if (users != null) {
                    // I have the old set of users, which will include QADude, if needed, so just return.
                    return;
                } else {
                    // set raw users empty so that we can add qa dude and copy just that
                    rawUsers = new ArrayList<BspUser>();
                }
            }

            if (deployment != Deployment.PROD) {
                addQADudeUsers(rawUsers);
            }

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
        } catch (Exception ex) {
            logger.debug("Could not refresh the user list", ex);
        }
    }

    public static class QADudeUser extends BspUser {
        public QADudeUser(String type, long userId) {
            setFields(userId, "QADude" + type, "QADude", type, "qadude" + type.toLowerCase() + "@broadinstitute.org");
        }

        private void setFields(long userId, String username, String firstName, String lastName, String email) {
            setUserId(userId);
            setUsername(username);
            setFirstName(firstName);
            setLastName(lastName);
            setEmail(email);
        }
    }

    private static void addQADudeUsers(List<BspUser> users) {
        // FIXME: should instead generate this dynamically based on current users.properties settings on the server.
        // Could also create QADude entries on demand during login.
        String[] types = {"Test", "PM", "PDM", "LU", "LM"};
        long userIdSeq = 101010101L;
        for (String type : types) {
            users.add(new QADudeUser(type, userIdSeq++));
        }
    }

    public boolean isTestUser(BspUser user) {
        return user instanceof QADudeUser;
    }

    public List<SelectItem> getSelectItems(Set<BspUser> users) {
        List<SelectItem> items = new ArrayList<SelectItem>();
        items.add(new SelectItem("", "Any"));
        for (BspUser user : users) {
            items.add(new SelectItem(user.getUserId(), user.getFirstName() + " " + user.getLastName()));
        }
        return items;
    }
}
