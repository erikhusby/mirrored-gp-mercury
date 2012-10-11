package org.broadinstitute.gpinformatics.athena.control.dao;

import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.List;

/**
 * Queries for the research project.
 */
@Stateful
@RequestScoped
public class ResearchProjectDao extends GenericDao {

    public List<ResearchProject> findResearchProjectsByOwner(long username) {
        return findList(ResearchProject.class, ResearchProject_.createdBy, username);
    }

    public ResearchProject findByBusinessKey(String key) {
        return findByTitle(key);
    }

    public ResearchProject findByTitle(String title) {
        return findSingle(ResearchProject.class, ResearchProject_.title, title);
    }

    public List<ResearchProject> findAllResearchProjects() {
        return findAll(ResearchProject.class);
    }

    public ResearchProject findByJiraTicketKey(String jiraTicketKey) {
        return findSingle(ResearchProject.class, ResearchProject_.jiraTicketKey, jiraTicketKey);
    }
}
