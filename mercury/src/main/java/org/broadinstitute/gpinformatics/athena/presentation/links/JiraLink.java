package org.broadinstitute.gpinformatics.athena.presentation.links;

import org.broadinstitute.gpinformatics.infrastructure.jira.JiraConfig;

import javax.faces.bean.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * This is a bean to help the UI deal with Jira links
 */
@Named
@RequestScoped
public class JiraLink {
    private static final String BROWSE = "/browse/";

    @Inject
    private JiraConfig jiraConfig;

    public String browseUrl(String jiraTicketKey) {
        return jiraConfig.getUrlBase() + BROWSE + jiraTicketKey;
    }
}
