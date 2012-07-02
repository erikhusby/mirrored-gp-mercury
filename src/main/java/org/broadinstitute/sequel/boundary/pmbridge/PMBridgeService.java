package org.broadinstitute.sequel.boundary.pmbridge;


import org.broadinstitute.sequel.boundary.pmbridge.data.ResearchProject;

import java.io.Serializable;

public interface PMBridgeService extends Serializable {


    public ResearchProject getResearchProjectByID(String id);

    public String getNameForResearchProjectID(String id);

}
