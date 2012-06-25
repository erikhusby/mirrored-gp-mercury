package org.broadinstitute.sequel.boundary.pmbridge;


import org.broadinstitute.sequel.boundary.pmbridge.data.ResearchProject;

public interface PMBridgeService {


    public ResearchProject getResearchProjectByID(String id);

    public String getNameForResearchProjectID(String id);

}
