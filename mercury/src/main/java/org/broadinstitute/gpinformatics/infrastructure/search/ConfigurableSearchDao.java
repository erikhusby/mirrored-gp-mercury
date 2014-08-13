package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.common.BaseSplitter;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.ejb.HibernateEntityManager;
import org.hibernate.sql.JoinFragment;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class takes a search structure and converts it to Hibernate criteria. The intended
 * calling sequence is:<br/>
 * criteria = dao.buildCriteria(...<br/>
 * <i>add any other hard-coded criteria</i><br/>
 * Pagination page = new Pagination(5000)<br/>
 * dao.startPagination(...<br/>
 * pagination.setJoinFetch(...<br/>
 * dao.getPage(0,...<br/>
 * dao.getPage(1,...<br/>
 * ...
 */
public class ConfigurableSearchDao extends GenericDao {
    /**
     * The maximum allowed length for all {@code IN} queries in a single SQL statement.
     * <p/>
     * If this limit is exceeded, an internal Oracle error is reported, ORA-00600. See
     * <a href="http://www.getdbainfo.com/2011/10/ora-00600-internal-error-code-arguments-kghsskins1/">this</a>
     * for more information. Where possible, check this value before calling oracle to avoid running into the
     * internal error.
     */
    public static final int IN_QUERY_TOTAL_SIZE = 32768;

    private ConfigurableSearchDefinition configurableSearchDefinition;

    /**
     * From a search definition, an instance, and sorting definition, build Hibernate criteria
     *
     * @param configurableSearchDefinition definitions of search terms
     * @param searchInstance               instances of search terms, with values
     * @param orderPath                    dot separated path to the property to order by, null if no sorting
     * @param orderDirection               ASC or DSC (ignored if orderPath is null, otherwise required)
     * @return Hibernate criteria
     */
    public Criteria buildCriteria(
            ConfigurableSearchDefinition configurableSearchDefinition, SearchInstance searchInstance, String orderPath,
            String orderDirection) {

        this.configurableSearchDefinition = configurableSearchDefinition;
        searchInstance.checkValues();
        HibernateEntityManager hibernateEntityManager = getEntityManager().unwrap(HibernateEntityManager.class);
        Session hibernateSession = hibernateEntityManager.getSession();
        Criteria criteria = hibernateSession.createCriteria(configurableSearchDefinition.getResultEntity());
        criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        // Each term contains a list of criteria names and a property. We may encounter
        // the same criteria name more than once in the parent / child hierarchy, so we
        // have to keep track of them and re-use previously created criteria,
        // otherwise Hibernate will give an error about duplicate paths.
        /*
         * Update 02-03-11 jdeffen:
         * 
         * There appears to be a flaw in this logic. The recurseSearchValues() method
         * creates a new mapPathToCriteria Map instance when it encounters child criteria
         * instead of pulling from the previously declared criteria in the Map. Thus
         * creating duplicate sub-criteria. Then addOrderByToCriteria() method doesn't
         * look at the Map either. The same Map of criteria should be used everywhere so
         * no duplicate criteria is created.
         * 
         * When this is done, however, there is a Hibernate 3.2 Bug that creates bad SQL
         * on JBOSS/Oracle systems....
         */
        Map<String, DetachedCriteria> mapPathToCriteria = new HashMap<>();

        recurseSearchValues(1, criteria, null, mapPathToCriteria, searchInstance.getSearchValues(),
                configurableSearchDefinition);

        if (orderPath != null) {
            addOrderByToCriteria(criteria, orderPath, orderDirection);
        }
        return criteria;
    }

    /**
     * Build Hibernate criteria without any sorting
     * As of 08/06/2014 only called from ConfigurableSearchDaoTest
     * @param configurableSearchDefinition
     * @param searchInstance
     * @return
     */
    public Criteria buildCriteria(ConfigurableSearchDefinition configurableSearchDefinition,
                                  SearchInstance searchInstance) {
        return buildCriteria(configurableSearchDefinition, searchInstance, null, null );
    }

    /**
     * @param criteria
     * @param orderPath
     * @param orderDirection
     */
    private static void addOrderByToCriteria(Criteria criteria, String orderPath, String orderDirection) {

        if (orderPath != null && orderDirection != null) {

            if (!orderPath.contains(".")) {

                // The orderBy property is on the result entity, so we can apply it
                // directly
                if (orderDirection.equals("ASC")) {
                    criteria.addOrder(Order.asc(orderPath));
                } else {
                    criteria.addOrder(Order.desc(orderPath));
                }
            }
            /*
             * Applying child criteria here instead of pulling from the predefined
             * criteria map is probably a problem. See above notes.
             */
            else {

                // To apply the orderBy to a nested property, we have to create criteria
                // for the intermediate levels
                String[] criteriaSteps = orderPath.split("\\.");
                Criteria childCriteria = criteria;

                for (int i = 0; i < criteriaSteps.length - 1; i++) {
                    childCriteria = childCriteria.createCriteria(criteriaSteps[i], JoinFragment.LEFT_OUTER_JOIN);
                }
                if (orderDirection.equals("ASC")) {
                    childCriteria.addOrder(Order.asc(criteriaSteps[criteriaSteps.length - 1]));
                } else {
                    childCriteria.addOrder(Order.desc(criteriaSteps[criteriaSteps.length - 1]));
                }
            }
        }
    }

    /**
     * Recurse over the hiearchical search instance and build Hibernate criteria
     *
     * @param depth                        The depth of the recursion, starting at 1
     * @param resultCriteria               Hibernate criteria for search result entity
     * @param detachedCriteria             Hibernate criteria for current search hierarchy
     * @param mapPathToCriteria            identifies previously created Hibernate criteria, so they
     *                                     can be reused
     * @param searchValues                 list of search values at one level in the hieararchy
     * @param configurableSearchDefinition definitions of search terms
     */
    private static void recurseSearchValues(int depth, Criteria resultCriteria, DetachedCriteria detachedCriteria,
                                            Map<String, DetachedCriteria> mapPathToCriteria,
                                            List<SearchInstance.SearchValue> searchValues,
                                            ConfigurableSearchDefinition configurableSearchDefinition) {

        for (SearchInstance.SearchValue searchValue : searchValues) {
            // Only include terms for which the user specified a value, include SQL
            // because it never has a value. Some terms are used only to narrow down the
            // constrained values for a child term, e.g. group narrows down the list of
            // projects, but group is not included in the criteria

            if ((searchValue.getAddToCriteria() || searchValue.getSearchTerm().getSqlRestriction() != null)
                && searchValue.getSearchTerm().getCriteriaPaths() != null) {

                int criteriaPathsIndex = 0;
                Disjunction disjunction = Restrictions.disjunction();
                // If there are multiple criteria paths, OR them together, and use the same
                // values for each, e.g. search across all aliases ORs together a search
                // of individual aliases with a search of sample aliases
                for (SearchTerm.CriteriaPath criteriaPath : searchValue.getSearchTerm().getCriteriaPaths()) {
                    // If the term is a child criteria of the result entity (rather than a
                    // property directly on the result entity)
                    if (criteriaPath.getCriteria() != null && criteriaPath.getCriteria().size() > 0) {

                        // Do we need a new subquery?
                        if (depth == 1
                            || (searchValue.getSearchTerm().isNewDetachedCriteria() != null && searchValue
                                .getSearchTerm().isNewDetachedCriteria())) {
                            if (criteriaPathsIndex == 0) {
                                resultCriteria.add(disjunction);
                            }
                            // Create a detached criteria for the top level criteria, and find
                            // the definition of which property will be projected out of the criteria
                            String firstCriteriaName = criteriaPath.getCriteria().get(0);
                            ConfigurableSearchDefinition.CriteriaProjection criteriaProjection = configurableSearchDefinition
                                    .getCriteriaProjection(firstCriteriaName);
                            if (criteriaProjection == null) {
                                throw new RuntimeException("Failed to find criteria projection for "
                                                              + firstCriteriaName);
                            }
                            detachedCriteria = DetachedCriteria.forEntityName(criteriaProjection.getEntityName());

                            // Start a new map for the new detached criteria (the map avoids duplicate paths)
                            mapPathToCriteria = new HashMap<>();

                            // Add the detached criteria as a subquery of the result criteria
                            detachedCriteria.setProjection(Property.forName(criteriaProjection.getSubProperty()));
                            disjunction.add(Subqueries.propertyIn(criteriaProjection.getSuperProperty(),
                                    detachedCriteria));
                        }
                        DetachedCriteria nestedCriteria = createCriteria(mapPathToCriteria, criteriaPath,
                                detachedCriteria);

                        // Add operator and value
                        Criterion criterion = buildCriterion(searchValue);
                        if (criterion != null) {
                            nestedCriteria.add(criterion);
                        }
                    } else {
                        Criterion criterion = buildCriterion(searchValue);
                        if (criterion != null) {
                            resultCriteria.add(criterion);
                        }
                    }
                    criteriaPathsIndex++;
                }
            }

            recurseSearchValues(depth + (searchValue.getSearchTerm().getCriteriaPaths() == null ? 0 : 1),
                    resultCriteria, detachedCriteria, mapPathToCriteria, searchValue.getChildren(),
                    configurableSearchDefinition);
        }
    }

    /**
     * Fetches list of IDs that satisfy search criteria
     *
     * @param pagination to hold results IDs
     * @param criteria   from buildCriteria
     */
    public void startPagination(PaginationDao.Pagination pagination, Criteria criteria, boolean doInitialfullFetch ) {
        pagination.setResultEntity(configurableSearchDefinition.getResultEntity());
        pagination.setResultEntityId(configurableSearchDefinition.getResultEntityId());
        // TODO set join fetch paths? would require access to column defs
        PaginationDao paginationDao = new PaginationDao();
        paginationDao.startPagination( criteria, pagination, doInitialfullFetch );
    }

    /**
     * Create a Hibernate restriction, based on the operator
     *
     * @param searchValue contains operator chosen by user
     * @return Hibernate restriction
     */
    private static Criterion buildCriterion(SearchInstance.SearchValue searchValue) {
        Criterion criterion;
        // Add the criterion for the search value
        List<Object> propertyValues = searchValue.convertSearchValue();

        if (searchValue.getSearchTerm().getSqlRestriction() != null) {
            criterion = Restrictions.sqlRestriction(searchValue.getSearchTerm().getSqlRestriction());
        } else if (propertyValues.get(0).equals("NoHibernateCriteria")) {
            // The value conversion expression may decide to remove the value, e.g.
            // Search across projects = Yes, means don't include project in search
            return null;
        } else if (propertyValues.get(0) instanceof List) {
            //noinspection unchecked
            criterion = createInCriterion(searchValue, (List<Object>) propertyValues.get(0));
        } else if (searchValue.getOperator() == SearchInstance.Operator.EQUALS) {
            if (searchValue.getDataType().equals("Date")) {
                // Date has implied midnight, so we need between this date and next day
                Calendar nextDay = Calendar.getInstance();
                nextDay.setTime((Date) propertyValues.get(0));
                nextDay.add(Calendar.DATE, 1);
                criterion = Restrictions.between(searchValue.getPropertyName(), propertyValues.get(0),
                        new Date(nextDay.getTimeInMillis()));
            } else if (searchValue.getCaseInsensitive() != null && searchValue.getCaseInsensitive()) {
                criterion = Restrictions.ilike(searchValue.getPropertyName(), propertyValues.get(0));
            } else {
                criterion = Restrictions.eq(searchValue.getPropertyName(), propertyValues.get(0));
            }
        } else if (searchValue.getOperator() == SearchInstance.Operator.LIKE) {
            criterion = Restrictions.ilike(searchValue.getPropertyName(), "%" + propertyValues.get(0) + "%");
        } else if (searchValue.getOperator() == SearchInstance.Operator.BETWEEN) {
            criterion = Restrictions.between(searchValue.getPropertyName(), propertyValues.get(0),
                    propertyValues.get(1));
        } else if (SearchInstance.Operator.IN == searchValue.getOperator()) {
            criterion = createInCriterion(searchValue, propertyValues);
        } else if (SearchInstance.Operator.NOT_IN == searchValue.getOperator()) {
            criterion = Restrictions.not(createInCriterion(searchValue, propertyValues));
        } else if (searchValue.getOperator() == SearchInstance.Operator.GREATER_THAN) {
            criterion = Restrictions.gt(searchValue.getPropertyName(), propertyValues.get(0));
        } else if (searchValue.getOperator() == SearchInstance.Operator.LESS_THAN) {
            criterion = Restrictions.lt(searchValue.getPropertyName(), propertyValues.get(0));
        } else {
            throw new RuntimeException("Unknown operator " + searchValue.getOperator());
        }
        return criterion;
    }

    /**
     * Returns a new Criterion object from
     * {@code Restrictions.in(searchValue.getPropertyName(), propertyValues)}.
     * Respects the {@code searchValue.getCaseInsensitive()} property.
     */
    private static Criterion createInCriterion(SearchInstance.SearchValue searchValue, List<Object> propertyValues) {

        Criterion criterion = null;

        if (searchValue.getCaseInsensitive() != null && searchValue.getCaseInsensitive()) {
            for (Object propertyValue : propertyValues) {
                if (criterion == null) {
                    criterion = Restrictions.ilike(searchValue.getPropertyName(), propertyValue);
                } else {
                    criterion = Restrictions.or(criterion,
                            Restrictions.ilike(searchValue.getPropertyName(), propertyValue));
                }
            }
        } else {
            if (propertyValues.size() > IN_QUERY_TOTAL_SIZE) {
                throw new RuntimeException("Oracle does not allow an IN query with more than "
                                           + IN_QUERY_TOTAL_SIZE +" total values, had "
                                           + propertyValues.size());
            }
            // Use OR to avoid Oracle's IN query batch limit.
            for (Collection<Object> subList : BaseSplitter.split(propertyValues)) {
                Criterion in = Restrictions.in(searchValue.getPropertyName(), subList);
                if (criterion != null) {
                    criterion = Restrictions.or(criterion, in);
                } else {
                    criterion = in;
                }
            }
        }
        return criterion;
    }

    /**
     * Creates the criteria specified in the search term
     *
     * @param mapPathToCriteria keeps track of criteria we've already created
     * @param criteriaPath      holds list of steps in criteria path
     * @param nestedCriteria    the criteria that will be attached to the result entity
     * @return the end of the criteria path
     */
    private static DetachedCriteria createCriteria(Map<String, DetachedCriteria> mapPathToCriteria,
                                                   SearchTerm.CriteriaPath criteriaPath,
                                                   DetachedCriteria nestedCriteria) {

        // Step over the first criteria, because we already created the detached criteria
        // for it
        for (int i = 1; i < criteriaPath.getCriteria().size(); i++) {

            String criteriaKey = getCriteriaKey(criteriaPath.getCriteria().get(i));

            // Have we created this criteria already?
            DetachedCriteria existingCriteria = mapPathToCriteria.get(criteriaKey);
            if (existingCriteria == null) {
                nestedCriteria = nestedCriteria.createCriteria(criteriaPath.getCriteria().get(i));
                mapPathToCriteria.put(criteriaKey, nestedCriteria);
            } else {
                nestedCriteria = existingCriteria;
            }
        }
        return nestedCriteria;
    }

    private static String getCriteriaKey(String path) {
        return path + "|";
    }
}
