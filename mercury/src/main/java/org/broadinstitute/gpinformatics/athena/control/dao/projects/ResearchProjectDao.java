package org.broadinstitute.gpinformatics.athena.control.dao.projects;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.ProjectPerson;
import org.broadinstitute.gpinformatics.athena.entity.project.ProjectPerson_;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.*;

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

    public ResearchProject findByBusinessKey(String key) {
        return findByJiraTicketKey(key);
    }

    /**
     * Find the ResearchProjects having personnel with the specified ids in {@link RoleType} PM.  This method
     * currently does not do Oracle-safe splits by 1000 ids, though there are not nearly 1000 PMs so this should not
     * be an issue.
     *
     * @param ids BSP ids of PMs.
     *
     * @return Associated ResearchProjects.
     */
    // TODO Splitterize
    public List<ResearchProject> findByProjectManagerIds(Long... ids) {
        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<ResearchProject> cq = cb.createQuery(ResearchProject.class);
        cq.distinct(true);

        Root<ResearchProject> root = cq.from(ResearchProject.class);
        SetJoin<ResearchProject,ProjectPerson> projectPersonSetJoin = root.join(ResearchProject_.associatedPeople);

        cq.where(
            // The role predicate below is written as an in expression for extensibility.  There was a concept of a
            // "PM in charge" that has a corresponding enum instance but is not currently being used.  If we want to
            // have this method accept users in additional roles, just add those roles to the in expression.
            projectPersonSetJoin.get(ProjectPerson_.role).in(RoleType.PM),
            projectPersonSetJoin.get(ProjectPerson_.personId).in((Object []) ids));

        return getEntityManager().createQuery(cq).getResultList();
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

    public List<ResearchProject> findByJiraTicketKeys(List<String> jiraTicketKeys) {
        return findListByList(ResearchProject.class, ResearchProject_.jiraTicketKey, jiraTicketKeys);
    }

    public Map<String, Long> getProjectOrderCounts() {

        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<Object> criteriaQuery = cb.createQuery();
        Root<ResearchProject> researchProjectRoot = criteriaQuery.from(ResearchProject.class);

        // join the orders (can be empty, so need left join
        Join<ResearchProject, ProductOrder> orders =
                researchProjectRoot.join(ResearchProject_.productOrders, JoinType.LEFT);

        Expression<Long> countExpression = cb.count(orders.get(ProductOrder_.productOrderId));
        Path<String> businessKeyPath = researchProjectRoot.get(ResearchProject_.jiraTicketKey);
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

    public Collection<ResearchProject> searchProjects(String searchText) {
        List<ResearchProject> allProjects = findAllResearchProjects();
        SortedSet<ResearchProject> list = new TreeSet<ResearchProject>();
        String[] searchWords = searchText.split("\\s");

        for (ResearchProject project : allProjects) {
            for (String searchWord : searchWords) {
                if (StringUtils.containsIgnoreCase(project.getTitle(), searchWord)) {
                    list.add(project);
                    break;
                }
            }
        }

        return list;
    }

}
