package org.broadinstitute.pmbridge.infrastructure.bsp;

import org.broadinstitute.pmbridge.control.UsernameAndPassword;

public interface BSPConnectionParameters extends UsernameAndPassword {

    static final String ROOT_BSP_URL = "/ws/bsp/search/runSampleSearch";

    public String getSuperuserLogin();


    public String getSuperuserPassword();


    public String getHostname();
    
    
    public int getPort();
    
}
