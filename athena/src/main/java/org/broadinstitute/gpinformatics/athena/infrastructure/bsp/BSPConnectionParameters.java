package org.broadinstitute.gpinformatics.athena.infrastructure.bsp;

import org.broadinstitute.gpinformatics.athena.control.UsernameAndPassword;

public interface BSPConnectionParameters extends UsernameAndPassword {

//    static final String ROOT_BSP_URL = ;
    static final String BSP_SAMPLE_SEARCH_URL = "/ws/bsp/search/runSampleSearch";
    static final String BSP_USERS_COHORT_URL = "/ws/bsp/collection/getSampleCollectionsByPM?username=";

    public String getSuperuserLogin();


    public String getSuperuserPassword();


    public String getHostname();
    
    
    public int getPort();
    
}
