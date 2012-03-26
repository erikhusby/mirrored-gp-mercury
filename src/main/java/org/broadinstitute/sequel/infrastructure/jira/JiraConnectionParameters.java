package org.broadinstitute.sequel.infrastructure.jira;

import org.broadinstitute.sequel.control.UsernameAndPassword;


public interface JiraConnectionParameters extends UsernameAndPassword {
    
    int getPort();
    
    String getHostname();
}
