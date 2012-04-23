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
 * Time: 5:12 PM
 */
public interface ResearchProjectService {

    Collection<ResearchProject> findAllResearchProjects();

    ResearchProject findResearchProjectByName();

    ResearchProject findResearchProjectById();

    ResearchProject createResearchProject(Person creator, Name title, ResearchProjectId id, String synopsis);

}
