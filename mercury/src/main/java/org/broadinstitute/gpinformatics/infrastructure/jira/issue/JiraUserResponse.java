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

package org.broadinstitute.gpinformatics.infrastructure.jira.issue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.xml.bind.annotation.XmlAttribute;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraUserResponse implements Serializable {

@XmlAttribute(name = "users")
private List<JiraUser> jiraUsers=new ArrayList<>();

    public JiraUserResponse() {
    }

    public JiraUserResponse(List<JiraUser> jiraUsers) {
        this.jiraUsers = jiraUsers;
    }

    public List<JiraUser> getJiraUsers() {
        return jiraUsers;
    }

    public void setJiraUsers(List<JiraUser> jiraUsers) {
        this.jiraUsers = jiraUsers;
    }
}
