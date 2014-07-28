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

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnTabulation;

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

    /**
     * Name of the search definition
     */
    private String name;

    // todo jmt use ColumnEntity here?
    /**
     * Hibernate entity returned by the search
     */
    private String resultEntity;

    /**
     * For pagination, queries need to be ordered by a unique ID
     */
    private String resultEntityId;

    /**
     * How many entities on each page of results
     */
    private Integer pageSize;

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
     * Map from term name to search term, includes dependent terms
     */
    transient private Map<String, SearchTerm> mapNameToSearchTerm = new HashMap<>();

    public ConfigurableSearchDefinition(String name, String resultEntity, String resultEntityId, Integer pageSize,
            List<CriteriaProjection> criteriaProjections,
            Map<String, List<SearchTerm>> mapGroupSearchTerms) {
        this.name = name;
        this.resultEntity = resultEntity;
        this.resultEntityId = resultEntityId;
        this.pageSize = pageSize;
        this.criteriaProjections = criteriaProjections;
        this.mapGroupSearchTerms = mapGroupSearchTerms;
        buildNameMap();
    }

    /**
     * Returns available column names which are configured for use as search criteria
     * @return
     */
    public Map<String, List<SearchTerm>> getMapGroupSearchTerms() {
        Map<String, List<SearchTerm>> mapGroupAvailableCriteria = new HashMap<>();
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
        Map<String, List<ColumnTabulation>> mapGroupToColumnTabulations = new HashMap<>();
        for (Map.Entry<String, List<SearchTerm>> groupSearchListEntry : mapGroupSearchTerms.entrySet()) {
            mapGroupToColumnTabulations.put(groupSearchListEntry.getKey(),
                    new ArrayList<ColumnTabulation>(groupSearchListEntry.getValue()));
        }
        return mapGroupToColumnTabulations;
    }

    public List<SearchTerm> getRequiredSearchTerms() {
        List<SearchTerm> requiredSearchTerms = new ArrayList<>();
        for (List<SearchTerm> searchTermList : mapGroupSearchTerms.values()) {
            for (SearchTerm searchTerm : searchTermList) {
                if (searchTerm.getRequired() != null && searchTerm.getRequired()) {
                    requiredSearchTerms.add(searchTerm);
                }
            }
        }
        return requiredSearchTerms;
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
         * Name of the property in the subquery (DetachedCriteria)
         */
        private String subProperty;

        /**
         * The sub entity for which to create a DetachedCriteria
         */
        private String entityName;

        public CriteriaProjection(String criteriaName, String superProperty, String subProperty, String entityName) {
            this.criteriaName = criteriaName;
            this.superProperty = superProperty;
            this.entityName = entityName;
            this.subProperty = subProperty;
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

        public String getEntityName() {
            return entityName;
        }

        public String getSubProperty() {
            if (subProperty == null) {
                return superProperty;
            }
            return subProperty;
        }
    }

    private void buildNameMap() {
        if (this.mapGroupSearchTerms != null) {
            for (List<SearchTerm> searchTerms : mapGroupSearchTerms.values()) {
                recurseSearchTerms(searchTerms);
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

    public String getName() {
        return name;
    }

    public String getResultEntity() {
        return resultEntity;
    }

    public String getResultEntityId() {
        return resultEntityId;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public SearchTerm getSearchTerm(String name) {
        // If the object has been serialized and deserialized then we have to
        // rebuild the map
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
}
