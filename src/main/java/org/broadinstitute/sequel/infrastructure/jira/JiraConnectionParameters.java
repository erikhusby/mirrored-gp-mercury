package org.broadinstitute.sequel.infrastructure.jira;

import org.broadinstitute.sequel.control.LoginAndPassword;


public interface JiraConnectionParameters extends LoginAndPassword {
    
    int getPort();
    
    String getHostname();
}
