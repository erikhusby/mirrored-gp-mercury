package org.broadinstitute.sequel.control.jira;

import org.broadinstitute.sequel.control.UsernameAndPassword;


public interface JiraConnectionParameters extends UsernameAndPassword {
    
    int getPort();
    
    String getHostname();
}
