package org.broadinstitute.gpinformatics.athena.control.dao.projects;

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
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import javax.persistence.metamodel.SingularAttribute;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Queries for the research project.
 * <p/>
 * Transaction is SUPPORTS so as to apply to all find methods to let them see any currently active transaction but not
 * begin, and therefore commit (along with any changes queued up in the persistence context), their own transaction.
 */
@Stateful
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@RequestScoped
public class ResearchProjectDao extends GenericDao {

    private static final boolean WITH_ORDERS = true;
    private static final boolean NO_ORDERS = false;

    public ResearchProject findByBusinessKey(String key) {
        return findByJiraTicketKey(key);
    }

    /**
     * Return a collection of all research projects that are accessible by a user.
     * A project is accessible by a user if:
     * <ul>
     * <li>the project's accessControlEnabled flag is false</li>
     * <li>the user is a person associated with this project in any role</li>
     * <li>the user is a person associated with any of this project's parent projects</li>
     * </ul>
     * To determine person/project association, we traverse the tree of projects in code.  Using a database query
     * would work but would be more complicated, and we have too few RPs in Mercury for this to be a performance
     * issue.
     *
     * @param userId user to search for
     *
     * @return a collection of all accessible projects for this user
     */
    public Set<ResearchProject> findAllAccessibleByUser(long userId) {
        Set<ResearchProject> foundProjects = new HashSet<>();
        List<ResearchProject> allRootProjects =
                findList(ResearchProject.class, ResearchProject_.parentResearchProject, null);

        for (ResearchProject project : allRootProjects) {
            project.collectAccessibleByUser(userId, foundProjects);
        }

        return foundProjects;
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
        SetJoin<ResearchProject, ProjectPerson> projectPersonSetJoin = root.join(ResearchProject_.associatedPeople);

        cq.where(
                // The role predicate below is written as an in expression for extensibility.  There was a concept of a
                // "PM in charge" that has a corresponding enum instance but is not currently being used.  If we want to
                // have this method accept users in additional roles, just add those roles to the in expression.
                projectPersonSetJoin.get(ProjectPerson_.role).in(RoleType.PM),
                projectPersonSetJoin.get(ProjectPerson_.personId).in((Object[]) ids));

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

    public List<ResearchProject> findByBusinessKeyList(List<String> businessKeys) {
        return findByJiraTicketKeys(businessKeys);
    }

    /**
     * Finds a list of research projects whose JIRA ticket key matches the search term passed in.
     *
     * @param searchTerm The term to search on. (case insensitive)
     *
     * @return A list of research projects that matches the search term by JIRA ticket key.
     */
    public List<ResearchProject> findLikeJiraTicketKey(String searchTerm) {
        return findListWithWildcard(ResearchProject.class, searchTerm, true, ResearchProject_.jiraTicketKey);
    }

    /**
     * Finds a list of research projects whose title matches the search term passed in.
     *
     * @param searchTerm The term to search on. (case insensitive)
     *
     * @return A list of research projects that matches the search term by title.
     */
    public List<ResearchProject> findLikeTitle(String searchTerm) {
        return findListWithWildcard(ResearchProject.class, searchTerm, true, ResearchProject_.title);
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

        Map<String, Long> projectOrderCounts = new HashMap<>(listActual.size());
        for (Object result : listActual) {
            Object[] values = (Object[]) result;
            projectOrderCounts.put((String) values[1], (Long) values[0]);
        }

        return projectOrderCounts;
    }

    /**
     * Search Research Projects by title.
     */
    public Collection<ResearchProject> searchProjectsByTitle(String searchText) {
        return searchProjects(searchText, ResearchProject_.title);
    }

    /**
     * Search Research Projects by JIRA ticket (business key).
     */
    public Collection<ResearchProject> searchProjectsByBusinessKey(String searchText) {
        return searchProjects(searchText, ResearchProject_.jiraTicketKey);
    }

    /**
     * Search Research Projects by the specified attribute.
     */
    private Collection<ResearchProject> searchProjects(String searchText,
                                                       SingularAttribute<ResearchProject, String> searchAttribute) {
        String[] searchWords = searchText.split("\\s");
        return findListWithWildcardList(ResearchProject.class, Arrays.asList(searchWords), true, searchAttribute);
    }

    /**
     * @param accessControlEnabled the access control setting to match
     *
     * @return all Research Projects with the access control setting provided
     */
    public Collection<ResearchProject> findAllWithAccess(boolean accessControlEnabled) {
        return findList(ResearchProject.class, ResearchProject_.accessControlEnabled, accessControlEnabled);
    }
}
