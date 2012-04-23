package org.broadinstitute.pmbridge.boundary.projects;

import org.broadinstitute.pmbridge.entity.common.Name;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.entity.project.ResearchProject;
import org.broadinstitute.pmbridge.entity.project.ResearchProjectId;

import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/17/12
 * Time: 5:16 PM
 */
public class ResearchProjectServiceImpl implements ResearchProjectService {

    @Override
    public Collection<ResearchProject> findAllResearchProjects() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResearchProject findResearchProjectByName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResearchProject findResearchProjectById() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResearchProject createResearchProject(Person creator, Name title, ResearchProjectId id, String synopsis) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
