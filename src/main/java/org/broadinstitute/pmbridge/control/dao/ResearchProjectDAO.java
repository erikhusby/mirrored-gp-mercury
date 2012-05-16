package org.broadinstitute.pmbridge.control.dao;

import clover.org.apache.commons.lang.StringUtils;
import org.broadinstitute.pmbridge.entity.common.Name;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.entity.person.RoleType;
import org.broadinstitute.pmbridge.entity.project.ResearchProject;

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
        saveProject(this.findById(111L));
        saveProject(this.findById(222L));
        saveProject(this.findById(333L));
        saveProject(this.findById(444L));

    }

    // TODO Temp method just to save an rp.
    public void saveProject(ResearchProject researchProject) {
        String researchProjectTitle = researchProject.getTitle().name;
        if ((researchProject!=null) && StringUtils.isBlank(researchProjectTitle)) {
            throw new IllegalArgumentException("ResearchProject title must non be blank.");
        }
        if (researchProjectsMap.containsKey(researchProjectTitle)) {
            throw new IllegalArgumentException("Research Project title must be unique. Research project title " +
                    researchProjectTitle  + " already exists.");
        }
        researchProjectsMap.put(researchProjectTitle, researchProject);
    }

    public ArrayList<ResearchProject> findResearchProjectsByOwner(String username) {
        ArrayList<ResearchProject> result = new ArrayList<ResearchProject>();

        //TODO hmc - hook up with the actual DB. Just return some dummy data. Always returns project 222
        result.add(this.findById( 222L ));

        return result;
    }


    public ResearchProject findById( Long researchProjectId ) {

        //TODO hmc - hook up with the actual DB.
        // create a dummy research project with rpid appended to title.
        Person programMgr = new Person("shefler@broad", "Erica", "Shefler", "1", RoleType.PROGRAM_PM );
        ResearchProject myResearchProject = new ResearchProject(programMgr,
                new Name("FakeResearchProject" + researchProjectId ), "Research Stuff");
        myResearchProject.setId( researchProjectId );

        return myResearchProject;
    }

    public ArrayList<ResearchProject> findAllResearchProjects() {
        ArrayList<ResearchProject> result = new ArrayList<ResearchProject>();

        result.addAll(researchProjectsMap.values());

        return result;

    }




}
