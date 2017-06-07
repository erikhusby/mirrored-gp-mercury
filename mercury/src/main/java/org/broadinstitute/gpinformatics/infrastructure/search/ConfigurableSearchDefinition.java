/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2005 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. Neither
 * the Broad Institute nor MIT can be responsible for its use, misuse, or functionality.
 */
package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnTabulation;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class defines search terms, from which search instances can be built. This class
 * is primarily concerned with storing information necessary to create Hibernate Detached
 * Criteria.
 */
public class ConfigurableSearchDefinition /*extends PreferenceDefinition*/ {

    private static final long serialVersionUID = 4774977707743986613L;

    // If any search terms have an alternate search definitions, give it a common name
    public static final String ALTERNATE_DEFINITION_ID = "ALT";

    /**
     * Hibernate entity returned by the search
     */
    private ColumnEntity resultColumnEntity;

    /**
     * List of information to created DetachedCriteria
     */
    private List<CriteriaProjection> criteriaProjections = new ArrayList<>();

    /**
     * Map from criteriaName to criteriaProjection
     */
    transient private Map<String, CriteriaProjection> mapCriteriaToProjection;

    /**
     * Map from group name to list of (top-level) search terms
     */
    private Map<String, List<SearchTerm>> mapGroupSearchTerms = new LinkedHashMap<>();

    /**
     * Map from group index to UI help text
     */
    private Map<Integer, String> mapGroupHelpText = new HashMap<>();

    /**
     * Map from term name to search term, includes dependent terms
     */
    transient private Map<String, SearchTerm> mapNameToSearchTerm = new HashMap<>();

    /**
     * Default result columns should none be selected by user
     */
    transient private List<SearchTerm> defaultResultColumns = new ArrayList<>();

    /**
     * Allow an evaluator to expand entity list to be attached to search term.
     */
    private Map<String, TraversalEvaluator> traversalEvaluators;

    /**
     * Allow a user to apply a filter to include/exclude what's provided from traversals
     */
    private Map<String, CustomTraversalEvaluator> customTraversalOptions;

    /**
     * Produce named AddRowsListener instances for this search definition.
     */
    private AddRowsListenerFactory addRowsListenerFactory;

    public ConfigurableSearchDefinition(ColumnEntity resultColumnEntity,
            List<CriteriaProjection> criteriaProjections,
            Map<String, List<SearchTerm>> mapGroupSearchTerms) {
        this.resultColumnEntity = resultColumnEntity;
        this.criteriaProjections = criteriaProjections;
        this.mapGroupSearchTerms = mapGroupSearchTerms;
        buildNameMap();
        buildDefaultColumnList();
    }

    /**
     * Returns available column names which are configured for use as search criteria
     * @return
     */
    public Map<String, List<SearchTerm>> getMapGroupSearchTerms() {
        Map<String, List<SearchTerm>> mapGroupAvailableCriteria = new LinkedHashMap<>();
        List<SearchTerm> searchTermList;
        for (Map.Entry<String, List<SearchTerm>> groupSearchListEntry : mapGroupSearchTerms.entrySet()) {
            searchTermList = new ArrayList<>();
            for( SearchTerm term : groupSearchListEntry.getValue() ) {
                if ( term.getCriteriaPaths() != null ) {
                    searchTermList.add(term);
                }
            }
            if( !searchTermList.isEmpty() ) {
                mapGroupAvailableCriteria.put(groupSearchListEntry.getKey(), searchTermList);
            }
        }
        return mapGroupAvailableCriteria;
    }

    public Map<String, List<ColumnTabulation>> getMapGroupToColumnTabulations() {
        Map<String, List<ColumnTabulation>> mapGroupToColumnTabulations = new LinkedHashMap<>();
        for (Map.Entry<String, List<SearchTerm>> groupSearchListEntry : mapGroupSearchTerms.entrySet()) {
            mapGroupToColumnTabulations.put(groupSearchListEntry.getKey(),
                    new ArrayList<ColumnTabulation>(groupSearchListEntry.getValue()));
        }
        return mapGroupToColumnTabulations;
    }

    /**
     * Allow UI help text to be displayed for an option group of columns
     */
    public void addColumnGroupHelpText( Map<String, String> nameHelpMap) {
        int index = 0;
        for( String groupName : mapGroupSearchTerms.keySet() ) {
            for( Map.Entry<String,String> helpNameEntry : nameHelpMap.entrySet() ) {
                if( helpNameEntry.getKey().equals(groupName) ) {
                    mapGroupHelpText.put(index, helpNameEntry.getValue() );
                    // Group name should never be duplicated
                    break;
                }
            }
            index++;
        }
    }

    /**
     * Allow UI help text to be displayed for an option group of columns
     * @return  Map of group name to help text to display
     */
    public Map<Integer,String> getMapGroupHelpText( ) {
        return mapGroupHelpText;
    }

    public List<SearchTerm> getRequiredSearchTerms() {
        List<SearchTerm> requiredSearchTerms = new ArrayList<>();
        for (List<SearchTerm> searchTermList : mapGroupSearchTerms.values()) {
            for (SearchTerm searchTerm : searchTermList) {
                if (searchTerm.getRequired() != null && searchTerm.getRequired() && !searchTerm.isNestedParent() ) {
                    requiredSearchTerms.add(searchTerm);
                }
            }
        }
        return requiredSearchTerms;
    }

    public List<SearchTerm> getDefaultResultColumns() {
        return defaultResultColumns;
    }

    /**
     * Allow an optional traversal evaluator to be attached to this search
     * @param Id - The identifier representing this evaluator (presented as the UI identifier)
     * @param traversalEvaluator
     */
    public void addTraversalEvaluator(String Id, TraversalEvaluator traversalEvaluator) {
        if( traversalEvaluators == null ) {
            traversalEvaluators = new LinkedHashMap<>();
        } else {
            if( traversalEvaluators.containsKey(Id) ) {
                throw new RuntimeException("Duplicate TraversalEvaluator Id in ConfigurableSearchDefinition: " + Id);
            }
        }
        this.traversalEvaluators.put(Id, traversalEvaluator);
    }

    /**
     * Obtain the TraversalEvaluator implementations
     */
    public Map<String,TraversalEvaluator> getTraversalEvaluators(){
        return traversalEvaluators;
    }

    /**
     * Allow an optional custom traversal evaluator to be attached to this search
     * @param customTraversalOption The custom traversal implementation
     */
    public void addCustomTraversalOption(CustomTraversalEvaluator customTraversalOption) {
        if( customTraversalOptions == null ) {
            // Retain deterministic ordering
            customTraversalOptions = new LinkedHashMap<>();
        } else {
            if( customTraversalOptions.containsKey(customTraversalOption.getUiName()) ) {
                throw new RuntimeException("Duplicate CustomTraversalOptions Id in ConfigurableSearchDefinition: " + customTraversalOption.getUiName());
            }
        }
        this.customTraversalOptions.put(customTraversalOption.getUiName(), customTraversalOption);
    }

    /**
     * Obtain the TraversalSelectionFilter implementations
     */
    public Map<String,CustomTraversalEvaluator> getCustomTraversalOptions(){
        return customTraversalOptions;
    }


    public void setAddRowsListenerFactory(AddRowsListenerFactory addRowsListenerFactory) {
        this.addRowsListenerFactory = addRowsListenerFactory;
    }

    public AddRowsListenerFactory getAddRowsListenerFactory(){
        return this.addRowsListenerFactory;
    }

    /**
     * Holds information necessary to attach a subquery for a Hibernate criteria. A
     * DetachedCriteria is created for an entity, a property is projected out of the
     * DetachedCriteria, and the DetachedCriteria is attached as a subquery to a property
     * in the superquery.
     */
    public static class CriteriaProjection implements Serializable {

        private static final long serialVersionUID = 7678380711117518632L;

        /**
         * Name of the criteria in the super entity
         */
        private String criteriaName;

        /**
         * Name of the property in the super entity
         */
        private String superProperty;

        /**
         * Name of the property in a subquery (DetachedCriteria)
         */
        private String subProperty;

        /**
         * Optional alias for the property in a subquery if a collection is
         *   projected from a subquery DetachedCriteria <br /><code>
         * // e.g. LabVessel containers subquery
         * DetachedCriteria tubeCriteria = DetachedCriteria
         *    .forEntityName(LabVessel.class.getName() )
         *    .createAlias( "containers", "container" ) // LabVessel.containers property is aliased as "container"
         *    .setProjection(Projections.property("container.labVesselId"));  // subProperty using aliased name (default is "this")
         * criterion = Restrictions.eq("label", ... ); // propertyName from SearchTerm.CriteriaPath
         * tubeCriteria.add(criterion);
         </code>
         */
        private String subPropertyAlias;

        /**
         * The sub entity for which to create a DetachedCriteria
         */
        private Class subEntityClass;

        public CriteriaProjection(String criteriaName, String superProperty, String subProperty
                , String subPropertyAlias, Class subEntityClass) {
            this.subPropertyAlias = subPropertyAlias;
            this.criteriaName = criteriaName;
            this.superProperty = superProperty;
            this.subProperty = subProperty;
            this.subEntityClass = subEntityClass;
        }

        public CriteriaProjection(String criteriaName, String superProperty, String subProperty
                , Class subEntityClass) {
            this( criteriaName, superProperty, subProperty, null, subEntityClass);
        }

        public String getCriteriaName() {
            return criteriaName;
        }

        public String getSuperProperty() {
            if (superProperty == null) {
                return subProperty;
            }
            return superProperty;
        }

        public Class getSubEntityClass() {
            return subEntityClass;
        }

        public String getSubProperty() {
            if (subProperty == null) {
                return superProperty;
            }
            return subProperty;
        }

        public String getSubPropertyAlias(){
            return subPropertyAlias;
        }
    }



    private void buildNameMap() {
        if (this.mapGroupSearchTerms != null) {
            for (List<SearchTerm> searchTerms : mapGroupSearchTerms.values()) {
                recurseSearchTerms(searchTerms);
            }
        }
    }

    private void buildDefaultColumnList(){
        for (List<SearchTerm> searchTerms : mapGroupSearchTerms.values()) {
            for( SearchTerm searchTerm : searchTerms ) {
                if( searchTerm.isDefaultResultColumn()) {
                    defaultResultColumns.add(searchTerm);
                }
            }
        }
    }

    private void recurseSearchTerms(List<SearchTerm> searchTerms) {
        if (searchTerms != null) {
            for (SearchTerm searchTerm : searchTerms) {
                mapNameToSearchTerm.put(searchTerm.getName(), searchTerm);
                recurseSearchTerms(searchTerm.getDependentSearchTerms());
            }
        }
    }

    public ColumnEntity getResultEntity() {
        return resultColumnEntity;
    }

    public SearchTerm getSearchTerm(String name) {
        // If the object has been serialized and deserialized then we have to rebuild the map
        if (mapNameToSearchTerm == null) {
            mapNameToSearchTerm = new HashMap<>();
            buildNameMap();
        }
        return mapNameToSearchTerm.get(name);
    }

    public List<CriteriaProjection> getCriteriaProjections() {
        return criteriaProjections;
    }

    public CriteriaProjection getCriteriaProjection(String criteriaName) {
        if (mapCriteriaToProjection == null) {
            mapCriteriaToProjection = new HashMap<>();
            for (CriteriaProjection criteriaProjection : criteriaProjections) {
                mapCriteriaToProjection.put(criteriaProjection.getCriteriaName(), criteriaProjection);
            }
        }
        return mapCriteriaToProjection.get(criteriaName);
    }

    /**
     * Attached to a ConfigurableSearchDefinition to produce ConfigurableList.AddRowsListener instances.
     */
    public abstract static class AddRowsListenerFactory {
        public abstract Map<String,ConfigurableList.AddRowsListener> getAddRowsListeners();
    }
}
