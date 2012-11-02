package org.broadinstitute.gpinformatics.mercury.presentation;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraConfig;

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

    public void setBspUser(@Nullable BspUser bspUser) {
        this.bspUser = bspUser;
    }

    public String getBadgeClass() {
        if (bspStatus != ServerStatus.loggedIn || jiraStatus != ServerStatus.loggedIn) {
            return "badge-warning";
        }
        return "badge-success";
    }

    public void setBspStatus(ServerStatus bspStatus) {
        this.bspStatus = bspStatus;
    }

    public void setJiraStatus(ServerStatus jiraStatus) {
        this.jiraStatus = jiraStatus;
    }

    public void loginJiraUser(String jiraUsername) {
        this.jiraUsername = jiraUsername;
        jiraStatus = ServerStatus.loggedIn;
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
}
