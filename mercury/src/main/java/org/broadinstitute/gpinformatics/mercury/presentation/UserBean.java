package org.broadinstitute.gpinformatics.mercury.presentation;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.mercury.entity.DB;

import javax.annotation.Nullable;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.EnumSet;

/**
 * @author breilly
 */
@Named
@SessionScoped
public class UserBean implements Serializable {
    private static final String SUPPORT_EMAIL = "mercury-support@broadinstitute.org";

    private static final String LOGIN_WARNING = "You need to log into JIRA and BSP before you can {0}.";

    @Nullable
    private BspUser bspUser;

    @Inject
    BSPConfig bspConfig;

    @Inject
    JiraConfig jiraConfig;

    private BSPUserList bspUserList;

    @Inject
    private JiraService jiraService;

    // FIXME: This is a HACK because we can't inject BSPUserList when running in arquillian.
    public void setBspUserList(BSPUserList bspUserList) {
        this.bspUserList = bspUserList;
    }

    public enum ServerStatus {
        down("text-error", "Cannot connect to {0} Server: ''{1}''"),
        loggedIn("text-success", "Logged into {0} as ''{2}''"),
        notLoggedIn("text-warning", "Not a {0} User",
                "You do not have an account on the {0} server, which is needed to use this application. "
                + "Please contact {3} to fix this problem.");

        /** The CSS class used to display the status test. */
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

        private ServerStatus(String cssClass, String statusFormat) {
            this(cssClass, statusFormat, statusFormat);
        }

        private ServerStatus(String cssClass, String statusFormat, String messageFormat) {
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

    private final EnumSet<DB.Role> roles = EnumSet.noneOf(DB.Role.class);

    private String loginUserName;

    public Collection<DB.Role> getRoles() {
        return ImmutableSet.copyOf(roles);
    }

    @Nullable
    public BspUser getBspUser() {
        return bspUser;
    }

    public void logout() {
        bspUser = null;
        jiraUsername = "";
        roles.clear();
        bspStatus = ServerStatus.notLoggedIn;
        jiraStatus = ServerStatus.notLoggedIn;
    }

    private void updateBspStatus() {
        bspUser = bspUserList.getByUsername(loginUserName);
        if (bspUser != null && !bspUserList.isTestUser(bspUser)) {
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
            if (jiraService.isValidUser(loginUserName)) {
                jiraUsername = loginUserName;
                jiraStatus = ServerStatus.loggedIn;
            } else {
                // The user is not a valid JIRA User.  Warn, but allow login.
                jiraStatus = ServerStatus.notLoggedIn;
            }
        } catch (Exception e) {
            // This can happen for a few reasons, most common is JIRA server is down/misconfigured
            jiraStatus = ServerStatus.down;
        }
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
        for (DB.Role role : DB.Role.values()) {
            if (request.isUserInRole(role.name)) {
                roles.add(role);
            }
        }
    }

    /**
     * Ensure that the user is logged in to BSP and JIRA, if not issue a warning using JSF. <p/>
     * If the user wasn't already logged into BSP, this will try again. Regardless of the user's JIRA login state
     * it always checks to see if the JIRA server is running. If JIRA isn't running then the user can't continue.
     * If BSP isn't running, it's OK as long as the user was verified with BSP at some point.
     * @param operation the operation name, for the warning text.
     * @param jsfBean the JSF bean used to issue the warning.
     * @return true if user is valid.
     */
    public void checkUserValidForOperation(String operation, AbstractJsfBean jsfBean) {
        // Check and see if the server state has changed to allow the user to log in.
        if (bspStatus != ServerStatus.loggedIn) {
            updateBspStatus();
        }
        // Always update the JIRA status.
        updateJiraStatus();
        if (!isValidUser()) {
            jsfBean.addErrorMessage(MessageFormat.format(LOGIN_WARNING, operation));
        }
    }

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
        String username = bspUser == null ? "" : bspUser.getUsername();
        return bspStatus.formatStatus("BSP", bspConfig.getHost(), username);
    }

    public String getBspMessage() {
        String username = bspUser == null ? "" : bspUser.getUsername();
        return bspStatus.formatMessage("BSP", bspConfig.getHost(), username);
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

    public boolean isPDMUser() {
        return roles.contains(DB.Role.PDM);
    }

    public boolean isDeveloperUser() {
        return roles.contains(DB.Role.Developer);
    }

    public String getRolesString() {
        if (roles.isEmpty()) {
            return "No Roles";
        }
        return "Roles: " + StringUtils.join(roles, ", ");
    }

    public String getDeveloperRole() {
        return DB.Role.Developer.name;
    }

    public String getProjectManagerRole() {
        return DB.Role.PM.name;
    }

    public String getBillingManagerRole() {
        return DB.Role.BillingManager.name;
    }

    public String getProductManagerRole() {
        return DB.Role.PDM.name;
    }

    public String getLabUserRole() {
        return DB.Role.LabUser.name;
    }

    public String getLabManagerRole() {
        return DB.Role.LabManager.name;
    }

    public String getAllRole() {
        return DB.Role.All.name;
    }
}

