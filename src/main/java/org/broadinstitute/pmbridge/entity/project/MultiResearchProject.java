package org.broadinstitute.pmbridge.entity.project;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.broadinstitute.pmbridge.entity.common.Name;
import org.broadinstitute.pmbridge.entity.person.Person;

import java.util.Collection;
import java.util.HashSet;

/**
 * This class encapsulates a number of actual research projects.
 * It exists to allow the class to impose/associate any specific behaviour on the
 * research projects as they get added.
 *
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/12/12
 * Time: 1:36 PM
 */
public class MultiResearchProject extends AbstractResearchProject {

    private final Collection<ResearchProject> researchProjects = new HashSet<ResearchProject>();

    public MultiResearchProject(Person creator, Name title, ResearchProjectId id, String synopsis) {
        super(creator, title, id, synopsis);
    }

    public Collection<ResearchProject> getResearchProjects() {
        return researchProjects;
    }
    public Collection<ResearchProject> addResearchProject(ResearchProject researchProject) {
        researchProjects.add(researchProject);
        return researchProjects;
    }
    public Collection<ResearchProject> removeResearchProject(ResearchProject researchProject) {
        researchProjects.remove(researchProject);
        return researchProjects;
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
     }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
