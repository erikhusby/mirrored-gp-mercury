package org.broadinstitute.sequel.boundary.pmbridge;


import org.broadinstitute.sequel.boundary.pmbridge.data.ResearchProject;

import java.util.HashMap;
import java.util.Map;

public class PMBridgeServiceStub implements PMBridgeService {

    private Map<Long, ResearchProject> researchProjects =
            new HashMap<Long, ResearchProject>();


    public PMBridgeServiceStub() {

        ResearchProject researchProject = new ResearchProject();

        researchProject.setCreatedBy("mcovarr");
        researchProject.setId(1L);
        researchProject.setName("Exome Express Research Project 1");

        researchProjects.put(researchProject.getId(), researchProject);

    }

    @Override
    public ResearchProject getResearchProjectByID(String id) {
        if ( researchProjects.containsKey( Long.valueOf(id)) )
            return researchProjects.get( Long.valueOf(id) );

        throw new ResearchProjectNotFoundException( "Research Project " + id + " not found!" );
    }


    @Override
    public String getNameForResearchProjectID(String id) {

        return getResearchProjectByID(id).getName();

    }
}
