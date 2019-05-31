package org.broadinstitute.gpinformatics.infrastructure.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.broadinstitute.gpinformatics.infrastructure.common.BaseSplitter;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.entity.sample.BulkQueryParameter;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.ejb.HibernateEntityManager;
import org.hibernate.sql.JoinFragment;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class ConfigurableSearchDao extends GenericDao {
    /**
     * The maximum allowed length for all {@code IN} queries in a single SQL statement.
     * <p/>
     * If this limit is exceeded, an internal Oracle error is reported, ORA-00600. See
     * <a href="http://www.getdbainfo.com/2011/10/ora-00600-internal-error-code-arguments-kghsskins1/">this</a>
     * for more information. Where possible, check this value before calling oracle to avoid running into the
     * internal error.
     */
    private static final int IN_QUERY_TOTAL_SIZE = 32768;

    private static final Log log = LogFactory.getLog(ConfigurableSearchDao.class);

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

        // Note: IllegalArgumentException will cleanly percolate up to UI error message
        // Test if inputs are blank (otherwise, a table scan is performed)
        // The case where a user supplies no terms is handled in ConfigurableSearchActionBean
        if( !searchInstance.checkValues() ) {
            throw new InformaticsServiceException("A value is required for each selected search term.");
        }

        // Fail cleanly with error message if any other terms are include with an exclusive term
        if( searchInstance.hasExclusiveViolation() ) {
            throw new InformaticsServiceException("No other search terms are allowed with an exclusive search term.");
        }

        HibernateEntityManager hibernateEntityManager = getEntityManager().unwrap(HibernateEntityManager.class);
        Session hibernateSession = hibernateEntityManager.getSession();
        Criteria criteria = hibernateSession.createCriteria(configurableSearchDefinition.getResultEntity().getEntityClass());
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

        addOrderByToCriteria(criteria, orderPath, orderDirection, configurableSearchDefinition);

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
     * @param configurableSearchDefinition
     */
    private void addOrderByToCriteria(Criteria criteria, String orderPath, String orderDirection
            , ConfigurableSearchDefinition configurableSearchDefinition ) {

        if (orderPath != null && orderDirection != null) {

            if (!orderPath.contains(".")) {
                // The orderBy property is on the result entity, so we can apply it directly
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

        // Always append entity ID to order (unless explicit sort on the same property requested),
        //    because a non-unique sort column will not yield reproducible results.
        if( orderPath == null || !orderPath.equals( configurableSearchDefinition.getResultEntity().getEntityIdProperty() ) ) {
            criteria.addOrder(Order.asc(configurableSearchDefinition.getResultEntity().getEntityIdProperty()));
        }

    }

    /**
     * Recurse over the search instance hierarchy and build Hibernate criteria
     *
     * @param depth                        The depth of the recursion, starting at 1
     * @param resultCriteria               Hibernate criteria for search result entity
     * @param detachedCriteria             Hibernate criteria for current search hierarchy
     * @param mapPathToCriteria            identifies previously created Hibernate criteria, so they
     *                                     can be reused
     * @param searchValues                 list of search values at one level in the hieararchy
     * @param configurableSearchDefinition definitions of search terms
     */
    private void recurseSearchValues(int depth, Criteria resultCriteria, DetachedCriteria detachedCriteria,
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
                                || ( searchValue.getSearchTerm().isNewDetachedCriteria() != null
                                     && searchValue.getSearchTerm().isNewDetachedCriteria()))
                        {
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
                            detachedCriteria = DetachedCriteria.forClass(criteriaProjection.getSubEntityClass());

                            // Start a new map for the new detached criteria (the map avoids duplicate paths)
                            mapPathToCriteria = new HashMap<>();

                            // Add the detached criteria as a subquery of the result criteria
                            detachedCriteria.setProjection(Property.forName(criteriaProjection.getSubProperty()));
                            disjunction.add(Subqueries.propertyIn(criteriaProjection.getSuperProperty(),
                                    detachedCriteria));
                        }

                        if (searchValue.getSearchTerm().getExternalDataExpression() != null) {
                            log.info("Fetching external data expression for " + searchValue.getSearchTerm().getName());
                            List<Object> objectList = searchValue.getSearchTerm().getExternalDataExpression().generate(searchValue.getValues());
                            log.info("Storing bulk query parameters " + objectList.size());
                            for (int i = 0; i < objectList.size(); i++) {
                                Object o = objectList.get(i);
                                getEntityManager().persist(new BulkQueryParameter((String) o));
                                if (i == objectList.size() - 1 || i % 40 == 0) {
                                    getEntityManager().flush();
                                }
                            }
                            log.info("Finished storing bulk query parameters " + objectList.size());
                        }

                        // Create the base search criterion using operator and value(s)
                        // Note:  Regardless of depth of any nested criteria paths,
                        //    the criterion property name is attached to the root criteria path
                        Criterion criterion = buildCriterion(searchValue, criteriaPath);

                        createCriteria( configurableSearchDefinition,
                                mapPathToCriteria, criteriaPath,
                                detachedCriteria, criterion, searchValue );

                    } else {
                        Criterion criterion = buildCriterion(searchValue, criteriaPath);
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
     * Allow a single exclusive search term to use an alternate search definition to return a list of ids
     *
     * @param pagination to hold results IDs
     * @param criteria   from buildCriteria
     * @param searchInstance search options
     * @param configurableSearchDef search configuration
     */
    public void startPagination(PaginationUtil.Pagination pagination, Criteria criteria, SearchInstance searchInstance,
                                ConfigurableSearchDefinition configurableSearchDef ) {

        // Existence of an exclusive term being the only one present will have been validated
        boolean isAlternateSearchDefinition = searchInstance.hasAlternateSearchDefinition();

        if( isAlternateSearchDefinition ) {
            pagination.setResultEntity(searchInstance.getAlternateSearchDefinition().getResultEntity());
        } else {
            pagination.setResultEntity(configurableSearchDef.getResultEntity());
        }
        searchInstance.getEvalContext().setPagination(pagination);

        // Determine if we need to expand the core entity list via a user selectable traversal option
        Map<String,Boolean> traversalEvaluatorValues = searchInstance.getTraversalEvaluatorValues();
        boolean traversalRequired = false;
        // User selectable traversal check box option(s)
        if( configurableSearchDef.getTraversalEvaluators() != null
            && traversalEvaluatorValues != null
            && traversalEvaluatorValues.containsValue(Boolean.TRUE) ) {
            traversalRequired = true;
        }

        if( !traversalRequired && !isAlternateSearchDefinition ) {
            // Overwhelming majority of searches fetch only ID values for pagination
            PaginationUtil.startPagination(criteria, pagination, false);
        } else {
            // Fetch the core entities before the traversal
            PaginationUtil.startPagination(criteria, pagination, true);

            TraversalEvaluator evaluator = null;
            Set<Object> traversalEntities = null;

            Map<String,TraversalEvaluator> traversalEvaluators;
            if( isAlternateSearchDefinition ) {
                traversalEvaluators = searchInstance.getAlternateSearchDefinition().getTraversalEvaluators();

                // Alternate traversal evaluator configured
                evaluator = traversalEvaluators.get(ConfigurableSearchDefinition.ALTERNATE_DEFINITION_ID);
                traversalEntities = evaluator.evaluate(pagination.getIdList(), searchInstance);
                // Reconfigure pagination with correct base entity type
                pagination.setResultEntity(configurableSearchDef.getResultEntity());
            } else {
                boolean isCustomTraversal = false;
                traversalEvaluators = configurableSearchDef.getTraversalEvaluators();
                for (Map.Entry<String, TraversalEvaluator> configuredEvaluatorEntry
                        : traversalEvaluators.entrySet()) {
                    // Traverse the options which are checked
                    if (traversalEvaluatorValues.get(configuredEvaluatorEntry.getKey())) {
                        evaluator = configuredEvaluatorEntry.getValue();

                        // Did user select a custom evaluator?
                        isCustomTraversal = configurableSearchDef.getCustomTraversalOptions() != null &&
                                configurableSearchDef.getCustomTraversalOptions().containsKey(
                                        searchInstance.getCustomTraversalOptionName() );

                        if( isCustomTraversal ) {
                            // Do the traversal via the custom evaluator
                            TransferTraverserCriteria.TraversalDirection traversalDirection = evaluator.getTraversalDirection();
                            CustomTraversalEvaluator customEvaluator = configurableSearchDef.getCustomTraversalOptions().get(searchInstance.getCustomTraversalOptionName());
                            if (traversalEntities == null) {
                                traversalEntities = customEvaluator.evaluate(pagination.getIdList(), traversalDirection, searchInstance);
                            } else {
                                traversalEntities.addAll(customEvaluator.evaluate(pagination.getIdList(), traversalDirection, searchInstance));
                            }
                        } else {
                            // Do the traversal via the standard (ancestor/descendant) evaluator
                            if (traversalEntities == null) {
                                traversalEntities = evaluator.evaluate(pagination.getIdList(), searchInstance);
                            } else {
                                traversalEntities.addAll(evaluator.evaluate(pagination.getIdList(), searchInstance));
                            }
                        }
                    }
                }

                // Add initial vessels to custom traversal results if selected
                // The custom traverser logic is responsible for adding initial vessels as required if exclude is checked
                if( isCustomTraversal ) {
                    if (!searchInstance.getExcludeInitialEntitiesFromResults()) {
                        // Put all the initial entities back into the result Set
                        traversalEntities.addAll(pagination.getIdList());
                    }
                }
            }

            // Replace the full entities in the pagination with ids using last evaluator
            List<Object> rootIdList = evaluator.buildEntityIdList(traversalEntities);
            pagination.setIdList(rootIdList);

        }

    }

    /**
     * Create a Hibernate restriction, based on the operator
     *
     * @param searchValue contains operator chosen by user
     * @param criteriaPath The criteria path used for this criterion (to obtain any optional hardcoded filters)
     * @return Hibernate restriction
     */
    private Criterion buildCriterion(SearchInstance.SearchValue searchValue, SearchTerm.CriteriaPath criteriaPath) {
        Criterion criterion;
        // Add the criterion for the search value
        List<Object> propertyValues = searchValue.convertSearchValue();

        if (searchValue.getSearchTerm().getSqlRestriction() != null) {
            criterion = Restrictions.sqlRestriction(searchValue.getSearchTerm().getSqlRestriction());
        } else if (propertyValues.get(0).equals("NoHibernateCriteria")) {
            // The value conversion expression may decide to remove the value, e.g.
            // Search across projects = Yes, means don't include project in search
            return null;
        } else if (searchValue.getSearchTerm().getExternalDataExpression() != null) {
            return null;
        } else if (propertyValues.get(0) instanceof List) {
            //noinspection unchecked
            criterion = createInCriterion(searchValue, (List<Object>) propertyValues.get(0));
        } else if (searchValue.getOperator() == SearchInstance.Operator.EQUALS) {
            if (searchValue.getDataType() == ColumnValueType.DATE || searchValue.getDataType() == ColumnValueType.DATE_TIME ) {
                // Date has implied midnight, so we need between this date and next day
                Calendar nextDay = Calendar.getInstance();
                nextDay.setTime((Date) propertyValues.get(0));
                nextDay.add(Calendar.DATE, 1);
                // Prevent getting next day if time is 12:00 AM
                nextDay.add(Calendar.SECOND, -1);
                criterion = Restrictions.between(searchValue.getPropertyName(), propertyValues.get(0),
                        nextDay.getTime());
            } else if (searchValue.getCaseInsensitive() != null && searchValue.getCaseInsensitive()) {
                criterion = Restrictions.ilike(searchValue.getPropertyName(), propertyValues.get(0));
            } else {
                criterion = Restrictions.eq(searchValue.getPropertyName(), propertyValues.get(0));
            }
        } else if (searchValue.getOperator() == SearchInstance.Operator.LIKE) {
            criterion = Restrictions.ilike(searchValue.getPropertyName(), "%" + propertyValues.get(0) + "%");
        } else if (searchValue.getOperator() == SearchInstance.Operator.BETWEEN) {
            if (searchValue.getDataType() == ColumnValueType.DATE) {
                // Date has implied midnight, so we need to adjust upper value to end of day
                Calendar nextDay = Calendar.getInstance();
                nextDay.setTime((Date) propertyValues.get(1));
                nextDay.add(Calendar.DATE, 1);
                // Prevent getting next day if time is 12:00 AM
                nextDay.add(Calendar.SECOND, -1);
                criterion = Restrictions.between(searchValue.getPropertyName(), propertyValues.get(0),
                        nextDay.getTime());
            } else {
                criterion = Restrictions.between(searchValue.getPropertyName(), propertyValues.get(0),
                        propertyValues.get(1));
            }
        } else if (SearchInstance.Operator.IN == searchValue.getOperator()) {
            criterion = createInCriterion(searchValue, propertyValues);
        } else if (SearchInstance.Operator.NOT_IN == searchValue.getOperator()) {
            criterion = Restrictions.not(createInCriterion(searchValue, propertyValues));
        } else if (searchValue.getOperator() == SearchInstance.Operator.GREATER_THAN) {
            criterion = Restrictions.gt(searchValue.getPropertyName(), propertyValues.get(0));
        } else if (searchValue.getOperator() == SearchInstance.Operator.LESS_THAN) {
            criterion = Restrictions.lt(searchValue.getPropertyName(), propertyValues.get(0));
        } else if (searchValue.getOperator() == SearchInstance.Operator.NOT_NULL ) {
            criterion = Restrictions.isNotNull(searchValue.getPropertyName());
        } else {
            throw new RuntimeException("Unknown operator " + searchValue.getOperator());
        }

        if( criteriaPath.getImmutableTermFilters() != null && criteriaPath.getImmutableTermFilters().size() > 0 ) {
            return addFiltersToCriterion(criterion, criteriaPath);
        } else {
            return criterion;
        }
    }

    /**
     * Adds additional hardcoded filters to a search term criterion
     * @param criterion The criterion created with the user entered operator and value(s)
     * @param criteriaPath The criteria path which has a filter attached
     * @return A junction containing the user criterion an all filters
     */
    private Criterion addFiltersToCriterion(Criterion criterion, SearchTerm.CriteriaPath criteriaPath){
        Conjunction junction = new Conjunction();
        junction.add(criterion);
        for (SearchTerm.ImmutableTermFilter immutableTermFilter : criteriaPath.getImmutableTermFilters()) {
            switch( immutableTermFilter.getOperator() ){
                case EQUALS:
                    junction.add(Restrictions.eq(immutableTermFilter.getPropertyName(), immutableTermFilter.getValues()[0]));
                    break;
                case GREATER_THAN:
                    junction.add(Restrictions.gt(immutableTermFilter.getPropertyName(), immutableTermFilter.getValues()[0]));
                    break;
                case GREATER_THAN_EQUAL:
                    junction.add(Restrictions.ge(immutableTermFilter.getPropertyName(), immutableTermFilter.getValues()[0]));
                    break;
                case LESS_THAN:
                    junction.add(Restrictions.lt(immutableTermFilter.getPropertyName(), immutableTermFilter.getValues()[0]));
                    break;
                case LESS_THAN_EQUAL:
                    junction.add(Restrictions.le(immutableTermFilter.getPropertyName(), immutableTermFilter.getValues()[0]));
                    break;
                case BETWEEN:
                    junction.add(Restrictions.between(immutableTermFilter.getPropertyName(), immutableTermFilter.getValues()[0], immutableTermFilter.getValues()[1]));
                    break;
                case IN:
                    junction.add(Restrictions.in(immutableTermFilter.getPropertyName(), immutableTermFilter.getValues()));
                    break;
                case LIKE:
                    junction.add(Restrictions.ilike(immutableTermFilter.getPropertyName(), immutableTermFilter.getValues()[0]));
                    break;
                case NOT_EQUALS:
                    junction.add(Restrictions.ne(immutableTermFilter.getPropertyName(), immutableTermFilter.getValues()[0]));
                    break;
                case NOT_IN:
                    junction.add(Restrictions.not(Restrictions.in(immutableTermFilter.getPropertyName(), immutableTermFilter.getValues())));
                    break;
                case NOT_NULL:
                    junction.add(Restrictions.isNotNull(immutableTermFilter.getPropertyName()));
                    break;
                default:
                    throw new RuntimeException("Unknown criteria operator");
            }

        }
        return junction;
    }

    /**
     * Returns a new Criterion object from
     * {@code Restrictions.in(searchValue.getPropertyName(), propertyValues)}.
     * Respects the {@code searchValue.getCaseInsensitive()} property.
     */
    private Criterion createInCriterion(SearchInstance.SearchValue searchValue, List<Object> propertyValues) {

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
     * Builds the criteria specified in the search term and attaches the criterion
     * This function is recursive on nested criteria paths which build subqueries against unrelated entities
     *    or to project a collection of properties.
     * Note:  As of 04/2015, child search terms and nested criteria paths are mutually exclusive.
     * @param configurableSearchDefinition Stores criteria projections for any nested criteria paths
     * @param mapPathToCriteria keeps track of criteria we've already created
     * @param criteriaPath      holds list of steps in criteria path
     * @param parentCriteria    the criteria that will be attached to the result entity
     * @param searchValueCriterion    The criterion to be added to the lowest depth of the criteria
     * @param searchValue   for external data expresssion
     * @return the end of the criteria path
     */
    private DetachedCriteria createCriteria(
            ConfigurableSearchDefinition configurableSearchDefinition,
            Map<String, DetachedCriteria> mapPathToCriteria,
            SearchTerm.CriteriaPath criteriaPath,
            DetachedCriteria parentCriteria,
            Criterion searchValueCriterion,
            SearchInstance.SearchValue searchValue) {

        // Logic allows for nesting subqueries recursively
        boolean isNestedSubquery = criteriaPath.getNestedCriteriaPath() != null;

        int buildCriteriaToIndex = criteriaPath.getCriteria().size();
        if( isNestedSubquery ) {
            // Ignore last criteria if nested criteria exist (used for projected property for nested subquery)
            buildCriteriaToIndex--;
        }

        // Step over the first criteria, because we already created the disjunction and detached criteria for it
        for (int i = 1; i < buildCriteriaToIndex; i++) {

            String criteriaKey = getCriteriaKey(criteriaPath.getCriteria().get(i));

            // Have we created this criteria already?
            DetachedCriteria existingCriteria = mapPathToCriteria.get(criteriaKey);
            if (existingCriteria == null) {
                // Create a new criteria and store it
                parentCriteria = parentCriteria.createCriteria(criteriaPath.getCriteria().get(i));
                mapPathToCriteria.put(criteriaKey, parentCriteria);
            } else {
                // Re-use existing
                parentCriteria = existingCriteria;
            }
        }

        // Done parsing parent criteria path, now recurse into any nested subqueries
        if( isNestedSubquery ) {

            SearchTerm.CriteriaPath nestedSubCriteriaPath = criteriaPath.getNestedCriteriaPath();
            String nestedCriteriaName = nestedSubCriteriaPath.getCriteria().get(0);
            ConfigurableSearchDefinition.CriteriaProjection nestedCriteriaProj =
                    configurableSearchDefinition.getCriteriaProjection(nestedCriteriaName);
            DetachedCriteria nestedSubCriteria = DetachedCriteria
                    .forClass(nestedCriteriaProj.getSubEntityClass())
                    .createAlias(nestedCriteriaProj.getSubProperty(), nestedCriteriaProj.getSubPropertyAlias())
                    .setProjection(Projections.property(nestedCriteriaProj.getSuperProperty()));

            nestedSubCriteria = createCriteria(configurableSearchDefinition, mapPathToCriteria,
                    nestedSubCriteriaPath, nestedSubCriteria, searchValueCriterion, searchValue);

            // Append the subquery to the parent criteria
            String parentProp = criteriaPath.getCriteria().get(criteriaPath.getCriteria().size() - 1);
            parentCriteria.add(Subqueries.propertyIn(parentProp, nestedSubCriteria));

        } else {
            // Add criterion to last criteria in chain
            if (searchValue.getSearchTerm().getExternalDataExpression() == null) {
                parentCriteria.add(searchValueCriterion);
            } else {
                DetachedCriteria nestedSubCriteria = DetachedCriteria
                        .forClass(BulkQueryParameter.class)
                        .setProjection(Projections.property("param"));
                String parentProp = criteriaPath.getPropertyName();
                parentCriteria.add(Subqueries.propertyIn(parentProp, nestedSubCriteria));
            }
        }

        return parentCriteria;
    }

    private String getCriteriaKey(String path) {
        return path + "|";
    }
}
