package org.broadinstitute.sequel.control.jira;


import org.broadinstitute.sequel.control.jira.issue.CreateRequest;
import org.broadinstitute.sequel.control.jira.issue.CreateResponse;

import java.io.IOException;

public interface JiraService {
    
    CreateResponse createIssue(String key, CreateRequest.Fields.Issuetype.IssuetypeName issuetype, String summary, String description) throws IOException;

}
