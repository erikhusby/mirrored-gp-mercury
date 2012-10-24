package org.broadinstitute.gpinformatics.athena.control.dao;

import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import java.util.List;

/**
 * Queries for the research project.
 *
 * Transaction is NOT_SUPPORTED so as to apply to all find methods to help avoid the extended persistence context from
 * eagerly joining any currently active transaction.
 */
@Stateful
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@RequestScoped
public class ResearchProjectDao extends GenericDao {

    public List<ResearchProject> findResearchProjectsByOwner(long username) {
        return findList(ResearchProject.class, ResearchProject_.createdBy, username);
    }

    public ResearchProject findByBusinessKey(String key) {
        return findByJiraTicketKey(key);
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
