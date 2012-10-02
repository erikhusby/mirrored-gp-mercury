package org.broadinstitute.gpinformatics.athena.control.dao;

import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.mercury.control.dao.GenericDao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.Query;
import java.util.ArrayList;

/**
 * Queries for the research project.
 */
@Stateful
@RequestScoped
public class ResearchProjectDAO extends GenericDao {

    @SuppressWarnings("unchecked")
    public ArrayList<ResearchProject> findResearchProjectsByOwner(Long username) {
        Query query = getThreadEntityManager().getEntityManager().createNamedQuery("ResearchProject.fetchByOwner");
        return (ArrayList<ResearchProject>) query.setParameter("owner", username).getResultList();
    }

    @SuppressWarnings("unchecked")
    public ResearchProject findResearchProjectsByName(String name) {
        Query query = this.getThreadEntityManager().getEntityManager().createNamedQuery("ResearchProject.fetchByName");
        return (ResearchProject) query.setParameter("name", name).getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public ArrayList<ResearchProject> findAllResearchProjects() {
        Query query = this.getThreadEntityManager().getEntityManager().createNamedQuery("ResearchProject.fetchAll");
        return (ArrayList<ResearchProject>) query.getResultList();
    }

    public ResearchProject findById(Long rpId) {
        Query query = this.getThreadEntityManager().getEntityManager().createNamedQuery("ResearchProject.findById");
        return (ResearchProject) query.setParameter("id", rpId).getSingleResult();
    }

}
