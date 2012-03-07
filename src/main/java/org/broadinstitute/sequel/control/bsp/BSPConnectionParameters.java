package org.broadinstitute.sequel.control.bsp;

import org.broadinstitute.sequel.control.UsernameAndPassword;

public interface BSPConnectionParameters extends UsernameAndPassword {

    
    public String getSuperuserLogin();


    public String getSuperuserPassword();


    public String getHostname();
    
    
    public int getPort();
    
}
