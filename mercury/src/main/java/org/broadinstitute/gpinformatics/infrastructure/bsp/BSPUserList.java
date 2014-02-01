package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jmx.AbstractCache;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Application wide access to BSP's user list. The list is regularly refreshed by ExternalDataCacheControl.
 */
@ApplicationScoped
public class BSPUserList extends AbstractCache implements Serializable {
    private static final Log logger = LogFactory.getLog(BSPUserList.class);
    private static final long serialVersionUID = -8290793988380748612L;

    @Inject
    private Deployment deployment;

    private BSPManagerFactory bspManagerFactory;

    private Map<Long, BspUser> users;

    private boolean serverValid;

    public boolean isServerValid() {
        return serverValid;
    }

    /**
     * @return list of bsp users, sorted by lastname, firstname, username, email.
     */
    public synchronized Map<Long, BspUser> getUsers() {
        if (users == null) {
            refreshCache();
        }
        return users;
    }

    /**
     * @param id key of user to look up
     * @return if found, the user, otherwise null
     */
    public BspUser getById(long id) {
        return getUsers().get(id);
    }

    /**
     * Returns the BSP user for the given username, or null if no user exists with that name. Username comparison
     * ignores case.
     *
     * @param username the username to look for
     * @return the BSP user or null
     */
    public BspUser getByUsername(String username) {
        for (BspUser user : getUsers().values()) {
            if (user.getUsername().equalsIgnoreCase(username)) {
                return user;
            }
        }
        return null;
    }

    /**
     * Returns the BSP user for the given badge ID, or null if no user exists with that badge ID.
     *
     * @param badgeId    the user's badge ID
     * @return the BSP user or null
     */
    public BspUser getByBadgeId(@Nonnull String badgeId) {
        for (BspUser user : getUsers().values()) {
            if (badgeId.equalsIgnoreCase(user.getBadgeNumber())) {
                return user;
            }
        }
        return null;
    }

    /**
     * Returns a list of users whose first name, last name, or username match the given query.  If the query is
     * null then it will return an empty list.
     *
     * @param query the query string to match on
     * @return a list of matching users
     */
    @Nonnull
    public List<BspUser> find(String query) {
        if (StringUtils.isBlank(query)) {
            // no query string supplied
            return Collections.emptyList();
        }

        String[] lowerQueryItems = query.toLowerCase().split("\\s");
        List<BspUser> results = new ArrayList<>();
        for (BspUser user : getUsers().values()) {
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
        return safeToLowerCase(user.getFirstName()).contains(lowerQuery) ||
               safeToLowerCase(user.getLastName()).contains(lowerQuery) ||
               safeToLowerCase(user.getUsername()).contains(lowerQuery) ||
               safeToLowerCase(user.getEmail()).contains(lowerQuery);
    }

    private static String safeToLowerCase(String s) {
        if (s == null) {
            return "";
        } else {
            return s.toLowerCase();
        }
    }

    public BSPUserList() {
    }

    @Inject
    public BSPUserList(@SuppressWarnings("CdiInjectionPointsInspection") BSPManagerFactory bspManagerFactory) {
        this.bspManagerFactory = bspManagerFactory;
    }

    @Override
    public synchronized void refreshCache() {
        try {
            List<BspUser> rawUsers = bspManagerFactory.createUserManager().getUsers();
            serverValid = rawUsers != null;

            if (!serverValid) {
                // BSP is down
                if (users != null) {
                    return;
                } else {
                    rawUsers = new ArrayList<>();
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

            // Use a LinkedHashMap since (1) it preserves the insertion order of its elements, so
            // our entries stay sorted and (2) it has lower overhead than a TreeMap.
            Map<Long, BspUser> userMap = new LinkedHashMap<>(rawUsers.size());
            for (BspUser user : rawUsers) {
                userMap.put(user.getUserId(), user);
            }

            users = ImmutableMap.copyOf(userMap);

        } catch (Exception ex) {
            logger.error("Could not refresh the user list", ex);
        }
    }

    public String getUserFullName(long userId) {
        BspUser bspUser = getById(userId);
        if (bspUser == null) {
            return "(Unknown user: " + userId + ")";
        }

        return bspUser.getFullName();
    }

    /**
     * Find the user by an email address.
     *
     * @param email The string to match
     *
     * @return The matching user, null if not found
     */
    public BspUser getByEmail(String email) {
        for (BspUser user : getUsers().values()) {
            if (user.getEmail().equalsIgnoreCase(email)) {
                return user;
            }
        }

        return null;
    }

    public static class QADudeUser extends BspUser {
        public QADudeUser(String type, long userId) {
            setFields(userId, "QADude" + type, "QADude", type, "qadude" + type.toLowerCase() + "@broadinstitute.org",
                    type + userId);
        }

        private void setFields(long userId, String username, String firstName, String lastName, String email,
                               String badgeId) {
            setUserId(userId);
            setUsername(username);
            setFirstName(firstName);
            setLastName(lastName);
            setEmail(email);
            setBadgeNumber(badgeId);
        }
    }

    private static void addQADudeUsers(List<BspUser> users) {
        // FIXME: should instead generate this dynamically based on current users.properties settings on the server.
        // Could also create QADude entries on demand during login.
        String[] types = {"Test", "PM", "PDM", "LU", "LM", "BM"};
        long userIdSeq = 101010101L;
        for (String type : types) {
            users.add(new QADudeUser(type, userIdSeq++));
        }
    }

    public static boolean isTestUser(BspUser user) {
        return user instanceof QADudeUser;
    }
}
