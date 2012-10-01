package org.broadinstitute.gpinformatics.athena.control.dao;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectId;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;

import javax.enterprise.inject.Default;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/2/12
 * Time: 12:02 PM
 */
@Default
public class ResearchProjectDAO {

    // TODO Temp map just for phase 1 until we have persistence.
    private Map<String, ResearchProject> researchProjectsMap = new HashMap<String, ResearchProject>();

    public ResearchProjectDAO() {

        //TODO hmc - hook up with the actual DB.
        // Save some dummy research projects
        saveProject(this.findById(new ResearchProjectId("111")));
        saveProject(this.findById(new ResearchProjectId("222")));
        saveProject(this.findById(new ResearchProjectId("333")));
        saveProject(this.findById(new ResearchProjectId("381")));
        saveProject(this.findById(new ResearchProjectId("444")));

    }

    // TODO Temp method just to save an rp.
    public void saveProject(ResearchProject researchProject) {
        String researchProjectTitle = researchProject.getTitle();
        if ((researchProject != null) && StringUtils.isBlank(researchProjectTitle)) {
            throw new IllegalArgumentException("ResearchProject title must non be blank.");
        }
        if (researchProjectsMap.containsKey(researchProjectTitle)) {
            throw new IllegalArgumentException("Research Project title must be unique. Research project title " +
                    researchProjectTitle + " already exists.");
        }
        researchProjectsMap.put(researchProjectTitle, researchProject);
    }

    public ArrayList<ResearchProject> findResearchProjectsByOwner(String username) {
        ArrayList<ResearchProject> result = new ArrayList<ResearchProject>();

        //TODO hmc - hook up with the actual DB. Just return some dummy data. Always returns project 222
        result.add(this.findById(new ResearchProjectId("222")));

        return result;
    }


    public ResearchProject findById(ResearchProjectId rpId) {

        //TODO hmc - hook up with the actual DB.
        // create a dummy research project with rpid appended to title.
        Person programMgr = new Person("shefler@broad", "Erica", "Shefler");
        ResearchProject myResearchProject = new ResearchProject(
                programMgr, "FakeResearchProject" + rpId, "Research Stuff");
        myResearchProject.addPerson(RoleType.PM, programMgr);
        myResearchProject.setId(rpId);

        return myResearchProject;
    }

    public ArrayList<ResearchProject> findAllResearchProjects() {
        ArrayList<ResearchProject> result = new ArrayList<ResearchProject>();

        result.addAll(researchProjectsMap.values());

        return result;

    }

}
