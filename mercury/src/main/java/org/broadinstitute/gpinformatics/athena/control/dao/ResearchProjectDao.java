package org.broadinstitute.gpinformatics.athena.control.dao;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Queries for the research project.
 *
 * Transaction is SUPPORTS so as to apply to all find methods to let them see any currently active transaction but not
 * begin, and therefore commit (along with any changes queued up in the persistence context), their own transaction.
 */
@Stateful
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@RequestScoped
public class ResearchProjectDao extends GenericDao {

    public static final boolean WITH_ORDERS = true;
    public static final boolean NO_ORDERS = false;

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
        return findAllResearchProjects(NO_ORDERS);
    }

    public List<ResearchProject> findAllResearchProjectsWithOrders() {
        return findAllResearchProjects(WITH_ORDERS);
    }

    private List<ResearchProject> findAllResearchProjects(boolean includeOrders) {
        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<ResearchProject> cq = cb.createQuery(ResearchProject.class);
        cq.distinct(true);

        Root<ResearchProject> researchProject = cq.from(ResearchProject.class);
        if (includeOrders) {
            researchProject.fetch(ResearchProject_.productOrders, JoinType.LEFT);
        }

        return getEntityManager().createQuery(cq).getResultList();
    }

    public ResearchProject findByJiraTicketKey(String jiraTicketKey) {
        return findSingle(ResearchProject.class, ResearchProject_.jiraTicketKey, jiraTicketKey);
    }

    public Map<String, Long> getProjectOrderCounts() {

        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<Object> criteriaQuery = cb.createQuery();
        Root<ResearchProject> researchProjectRoot = criteriaQuery.from(ResearchProject.class);

        // join the orders (can be empty, so need left join
        Join<ResearchProject, ProductOrder> orders =
                researchProjectRoot.join(ResearchProject_.productOrders, JoinType.LEFT);

        Expression countExpression = cb.count(orders.get(ProductOrder_.productOrderId));
        Path businessKeyPath = researchProjectRoot.get(ResearchProject_.jiraTicketKey);
        CriteriaQuery<Object> select = criteriaQuery.multiselect(countExpression, businessKeyPath);

        select.groupBy(businessKeyPath);

        TypedQuery<Object> typedQuery = getEntityManager().createQuery(select);
        List<Object> listActual = typedQuery.getResultList();

        Map<String, Long> projectOrderCounts = new HashMap<String, Long>(listActual.size());
        for (Object result : listActual) {
            Object[] values = (Object[]) result;
            projectOrderCounts.put((String) values[1], (Long) values[0]);
        }

        return projectOrderCounts;
    }
}
