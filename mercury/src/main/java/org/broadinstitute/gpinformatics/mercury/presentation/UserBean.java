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
    public static final String SUPPORT_EMAIL = "mercury-support@broadinstitute.org";

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
        if (bspUser != null && !bspUserList.isTestUser(bspUser)) {
            bspStatus = ServerStatus.loggedIn;
        } else if (bspUserList.isServerValid()) {
            bspStatus = ServerStatus.notLoggedIn;
        } else {
            // BSP Server is unresponsive, can't log in to verify user.
            bspStatus = ServerStatus.down;
        }
    }

    private void updateJiraStatus(String username) {
        try {
            if (jiraService.isValidUser(username)) {
                jiraUsername = username;
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
        String username = request.getUserPrincipal().getName();
        bspUser = bspUserList.getByUsername(username);
        updateBspStatus();
        updateJiraStatus(username);
        for (DB.Role role : DB.Role.values()) {
            if (request.isUserInRole(role.name)) {
                roles.add(role);
            }
        }
    }

    /**
     * @return CSS class for styling user name 'badge' at top of screen
     */
    public String getBadgeClass() {
        if (isValidBspUser() && isValidJiraUser()) {
            return "badge-success";
        }
        return "badge-warning";
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

