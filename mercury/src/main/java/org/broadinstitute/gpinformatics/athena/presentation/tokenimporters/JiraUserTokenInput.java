/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2015 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraUser;
import org.json.JSONException;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.List;

/**
 * Token Input support for 
 */
@Dependent
public class JiraUserTokenInput extends TokenInput<JiraUser> {

    @Inject
    private JiraService jiraService;
    private JiraUserTokenInput() {
        super(SINGLE_LINE_FORMAT);
    }

    @Override
    protected String getTokenId(  JiraUser jiraUser) {
        return jiraUser.getKey();
    }

    @Override
    protected String getTokenName(JiraUser jiraUser) {
        return displayName(jiraUser);
    }

    @Override
    protected String formatMessage(String messageString, JiraUser jiraUser) {
        return MessageFormat.format(messageString, displayName(jiraUser));
    }

    private static String displayName(JiraUser jiraUser) {
        return String.format("%s (%s)", jiraUser.getDisplayName(), jiraUser.getName());
    }

    public String getJsonString(String query) throws JSONException {
        if (StringUtils.isNotBlank(query)) {
            List<JiraUser> jiraJiraUsers = jiraService.getJiraUsers(query);
            return createItemListString(jiraJiraUsers);
        }
        return "";
    }

    @Override
    protected JiraUser getById(String key) {
        List<JiraUser> jiraUsers = jiraService.getJiraUsers(key);
        for (JiraUser jiraUser : jiraUsers) {
            if (jiraUser.getKey().equals(key)) {
                return jiraUser;
            }
        }
        return null;
    }
}
