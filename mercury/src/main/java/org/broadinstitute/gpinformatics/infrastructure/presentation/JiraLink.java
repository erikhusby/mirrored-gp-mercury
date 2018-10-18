package org.broadinstitute.gpinformatics.infrastructure.presentation;

import org.broadinstitute.gpinformatics.infrastructure.jira.JiraConfig;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

/**
 * This class is used to generate JIRA links for the UI.
 */
@Dependent
public class JiraLink {
    public static final String BROWSE = "/browse/";

    @Inject
    private JiraConfig jiraConfig;

    public String browseUrl(String jiraTicketKey) {
        return jiraConfig.getUrlBase() + BROWSE + jiraTicketKey;
    }
}
