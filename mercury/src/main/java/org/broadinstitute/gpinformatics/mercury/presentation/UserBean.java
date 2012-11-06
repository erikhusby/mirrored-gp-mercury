package org.broadinstitute.gpinformatics.mercury.presentation;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.text.MessageFormat;

/**
 * @author breilly
 */
@Named
@SessionScoped
public class UserBean implements Serializable {
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
        notLoggedIn("text-warning", "Not a {0} User");

        private final String cssClass;
        private final String format;

        private String formatStatus(String serviceName, String host, String username) {
            return MessageFormat.format(format, serviceName, host, username);
        }

        private ServerStatus(String cssClass, String format) {
            this.cssClass = cssClass;
            this.format = format;
        }

        public boolean isValid() {
            return this == loggedIn;
        }
    }

    private ServerStatus bspStatus = ServerStatus.notLoggedIn;
    private ServerStatus jiraStatus = ServerStatus.notLoggedIn;

    private String jiraUsername;

    @Nullable
    public BspUser getBspUser() {
        return bspUser;
    }

    public void logout() {
        bspUser = null;
        jiraUsername = "";
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

    public void login(@Nonnull String username) {
        bspUser = bspUserList.getByUsername(username);
        updateBspStatus();
        updateJiraStatus(username);
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

    public String getBspStatusClass() {
        return bspStatus.cssClass;
    }

    public String getJiraStatus() {
        return jiraStatus.formatStatus("JIRA", jiraConfig.getUrlBase(), jiraUsername);
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
}
