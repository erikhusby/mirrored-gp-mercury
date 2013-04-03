package org.broadinstitute.gpinformatics.athena.presentation.links;

import org.broadinstitute.gpinformatics.infrastructure.jira.JiraConfig;

import javax.inject.Inject;

/**
 * This class is used to generate JIRA links for the UI.
 */
public class JiraLink {
    private static final String BROWSE = "/browse/";

    @Inject
    private JiraConfig jiraConfig;

    public String browseUrl(String jiraTicketKey) {
        return jiraConfig.getUrlBase() + BROWSE + jiraTicketKey;
    }
}
