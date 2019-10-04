package org.broadinstitute.gpinformatics.mercury.presentation;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.security.Role;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.EnumSet;

/**
 * @author breilly
 */
// FIXME: properly support serialization; need to add readResolve() method that calls ensureUserValid().
@SessionScoped
public class UserBean implements Serializable {
    private static final String SUPPORT_EMAIL = "mercury-support@broadinstitute.org";
    private static final Log log = LogFactory.getLog(UserBean.class);

    /**
     * Prefix for CRSP roles. This is added to regular mercury roles; you can map from one to the other by adding
     * or removing it.
     */
    private static final String CRSP_ROLE_PREFIX = "CRSP-";

    public static final String LOGIN_WARNING = "Your account needs to connect to JIRA and BSP before you can {0}.  " +
                                               "The application is currently having problems reaching at least one " +
                                               "of them, please try again later.";
    private static final String PRODINFO_USER = "prodinfo";

    private BspUser bspUser;

    @Inject
    BSPConfig bspConfig;

    @Inject
    JiraConfig jiraConfig;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private JiraService jiraService;

    public static final BspUser UNKNOWN = new BspUser() {{
        setUserId(-1L);
        setUsername("");
    }};
    private boolean GPPMUser;

    private boolean isTestUser() {
        return bspUserList.isTestUser(bspUser);
    }

    /**
     * Check and see if the role string is a role on this user.
     *
     * @param roleName the role name
     * @return true if user has this role
     */
    public boolean isUserInRole(String roleName) {
        for (Role role : roles) {
            if (role.name.equals(roleName)) {
                return true;
            }
        }
        return false;
    }

    public enum ServerStatus {
        down("text-error", "Cannot connect to {0} Server: ''{1}''"),
        loggedIn("text-success", "Logged into {0} as ''{2}''"),
        notLoggedIn("text-warning", "Not a {0} User",
                "You do not have an account on the {0} server, which is needed to use this application. "
                + "Please contact {3} to fix this problem.");

        /** The CSS class used to display the status text. */
        private final String cssClass;

        /** A short message used to show the status of this server in a tooltip. */
        private final String statusFormat;

        /** A longer message used to show the status of this server. */
        private final String messageFormat;

        private String formatStatus(String serviceName, String host, String username) {
            return MessageFormat.format(statusFormat, serviceName, host, username, "");
        }

        private String formatMessage(String serviceName, String host, String username) {
            return MessageFormat.format(messageFormat, serviceName, host, username, SUPPORT_EMAIL);
        }

        ServerStatus(String cssClass, String statusFormat) {
            this(cssClass, statusFormat, statusFormat);
        }


        ServerStatus(String cssClass, String statusFormat, String messageFormat) {
            this.cssClass = cssClass;
            this.statusFormat = statusFormat;
            this.messageFormat = messageFormat;
        }

        public boolean isValid() {
            return this == loggedIn;
        }
    }

    private ServerStatus bspStatus = ServerStatus.notLoggedIn;
    private ServerStatus jiraStatus = ServerStatus.notLoggedIn;

    private String jiraUsername;

    private final EnumSet<Role> roles = EnumSet.noneOf(Role.class);

    private String loginUserName;

    public Collection<Role> getRoles() {
        return ImmutableSet.copyOf(roles);
    }

    public BspUser getBspUser() {
        return bspUser;
    }

    public String getLoginUserName() {
        return loginUserName;
    }

    public void logout() {
        loginUserName = null;
        bspUser = UNKNOWN;
        jiraUsername = "";
        roles.clear();
        bspStatus = ServerStatus.notLoggedIn;
        jiraStatus = ServerStatus.notLoggedIn;
    }

    private void updateBspStatus() {
        BspUser user = bspUserList.getByUsername(loginUserName);
        bspUser = UNKNOWN;
        if (user != null) {
            bspUser = user;
            bspStatus = ServerStatus.loggedIn;
        } else if (bspUserList.isServerValid()) {
            bspStatus = ServerStatus.notLoggedIn;
        } else {
            // BSP Server is unresponsive, can't log in to verify user.
            bspStatus = ServerStatus.down;
        }
    }

    private void updateJiraStatus() {
        try {
            if (isTestUser() || jiraService.isValidUser(loginUserName)) {
                jiraUsername = loginUserName;
                jiraStatus = ServerStatus.loggedIn;
            } else {
                // The user is not a valid JIRA User.  Warn, but allow login.
                jiraStatus = ServerStatus.notLoggedIn;
            }
        } catch (Exception e) {
            // This can happen for a few reasons, most common is JIRA server is down/misconfigured
            log.error("Failed to communicate with JIRA", e);
            jiraStatus = ServerStatus.down;
        }
    }

    /**
     * This method logs in as a test user, for automated test support.
     *
     * ONLY CALL FROM TESTS.
     */
    public void loginTestUser() {
        loginDeveloper("QADudeTest");
    }

    public void loginViewOnlyUser() {
        loginVisitor("QAVisitor", Role.Viewer);
    }

    public void loginFinanceUser() {
        loginVisitor("QAFinance", Role.FinanceViewer);
    }

    /**
     * Logs in as the current OS user.
     * ONLY CALL FROM FIXUP TESTS
     */
    public void loginOSUser() {
        String osUser = System.getProperty("user.name");
        if (PRODINFO_USER.equals(osUser)) {
            throw new RuntimeException("You probably shouldn't be running this script as " + osUser + " because it will corrupt the audit trail.");
        }
        loginDeveloper(osUser);
    }

    private void loginDeveloper(String user) {
        login(user);
        roles.add(Role.Developer);
    }

    private void loginVisitor(String user, Role viewer) {
        login(user);
        roles.add(viewer);
    }

    public void login(String user) {
        loginUserName = user;
        updateBspStatus();
    }

    /**
     * Log in the user and cache user login state.  Application roles, BSP account status and JIRA
     * account are cached for the duration of the session, so a user must log out and log back in to see
     * any changes.
     * @param request the request used for a successful user login
     */
    public void login(HttpServletRequest request) {
        loginUserName = request.getUserPrincipal().getName();
        updateBspStatus();
        updateJiraStatus();
        for (Role role : Role.values()) {
            if (request.isUserInRole(role.name) || request.isUserInRole(CRSP_ROLE_PREFIX + role.name)) {
                roles.add(role);
            }
        }
        if(CollectionUtils.isEmpty(roles)) {
            roles.add(Role.Viewer);
        }
    }

    /**
     * Ensure that the user is logged in to BSP and JIRA, updating the status if necessary. <p/>
     * If the user wasn't already logged into BSP, this will try again. Regardless of the user's JIRA login state
     * it always checks to see if the JIRA server is running. If JIRA isn't running then the user can't continue.
     * If BSP isn't running, it's OK as long as the user was verified with BSP at some point.
     *
     * @return true if user is valid.
     */
    public boolean ensureUserValid() {
        // Check and see if the server state has changed to allow the user to log in.
        if (bspStatus != ServerStatus.loggedIn) {
            updateBspStatus();
        }
        // Always update the JIRA status.
        updateJiraStatus();
        return isValidUser();
    }

    /**
     * @return true if user is valid with BSP and JIRA.
     */
    public boolean isValidUser() {
        return isValidBspUser() && isValidJiraUser();
    }

    /**
     * @return CSS class for styling user name 'badge' at top of screen
     */
    public String getBadgeClass() {
        return isValidUser() ? "badge-success" : "badge-warning";
    }

    public String getBspStatus() {
        return bspStatus.formatStatus("BSP", bspConfig.getHost(), bspUser.getUsername());
    }

    public String getBspMessage() {
        return bspStatus.formatMessage("BSP", bspConfig.getHost(), bspUser.getUsername());
    }

    public String getBspStatusClass() {
        return bspStatus.cssClass;
    }

    public String getJiraStatus() {
        return jiraStatus.formatStatus("JIRA", jiraConfig.getUrlBase(), jiraUsername);
    }

    public String getJiraMessage() {
        return jiraStatus.formatMessage("JIRA", jiraConfig.getUrlBase(), jiraUsername);
    }

    public String getJiraStatusClass() {
        return jiraStatus.cssClass;
    }

    public boolean isValidBspUser() {
        return bspStatus.isValid();
    }

    public boolean isValidJiraUser() {
        return jiraStatus.isValid();
    }

    public boolean isPMUser() {
        return roles.contains(Role.PM);
    }

    public boolean isPDMUser() {
        return roles.contains(Role.PDM);
    }

    public boolean isDeveloperUser() {
        return roles.contains(Role.Developer);
    }

    public boolean isNotPDMOrDev() {return !roles.contains(Role.Developer)&&!roles.contains(Role.PDM);}

    public boolean isGPPMUser() {
        return roles.contains(Role.GPProjectManager);
    }

    public boolean isViewer() {
        return roles.contains(Role.Viewer) || roles.contains(Role.FinanceViewer);
    }

    public String getRolesString() {
        if (roles.isEmpty()) {
            return "No Roles";
        }
        return "Roles: " + StringUtils.join(roles, ", ");
    }
}

