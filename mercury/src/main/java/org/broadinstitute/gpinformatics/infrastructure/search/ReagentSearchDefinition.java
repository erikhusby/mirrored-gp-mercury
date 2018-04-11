package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Builds ConfigurableSearchDefinition for mercury sample user defined search logic
 */
public class ReagentSearchDefinition {

    public ConfigurableSearchDefinition buildSearchDefinition() {
        Map<String, List<SearchTerm>> mapGroupSearchTerms = new LinkedHashMap<>();

        List<SearchTerm> searchTerms = buildReagentGroup();
        mapGroupSearchTerms.put("Reagents", searchTerms);

        searchTerms = buildReagentEventGroup();
        mapGroupSearchTerms.put("Events", searchTerms);

        List<ConfigurableSearchDefinition.CriteriaProjection> criteriaProjections = new ArrayList<>();

        criteriaProjections.add( new ConfigurableSearchDefinition.CriteriaProjection("ReagentID",
                "reagentId", "reagentId", Reagent.class));

        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                ColumnEntity.REAGENT, criteriaProjections, mapGroupSearchTerms);

        return configurableSearchDefinition;
    }

    private List<SearchTerm> buildReagentGroup() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Reagent Name");
        searchTerm.setIsDefaultResultColumn(Boolean.TRUE);
        searchTerm.setDbSortPath("name");
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria( Arrays.asList( "ReagentID" ) );
        criteriaPath.setPropertyName("name");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                Reagent reagent = (Reagent) entity;
                return reagent.getName();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Lot Number");
        searchTerm.setIsDefaultResultColumn(Boolean.TRUE);
        searchTerm.setDbSortPath("lot");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("ReagentID"));
        criteriaPath.setPropertyName("lot");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                Reagent reagent = (Reagent) entity;
                return reagent.getLot();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Expiration Date");
        searchTerm.setDbSortPath("expiration");
        searchTerm.setValueType(ColumnValueType.DATE);
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("ReagentID"));
        criteriaPath.setPropertyName("expiration");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Date evaluate(Object entity, SearchContext context) {
                Reagent reagent = (Reagent) entity;
                return reagent.getExpiration();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("First Use Date");
        searchTerm.setDbSortPath("firstUse");
        searchTerm.setValueType(ColumnValueType.DATE_TIME);
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("ReagentID"));
        criteriaPath.setPropertyName("firstUse");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Date evaluate(Object entity, SearchContext context) {
                Reagent reagent = (Reagent) entity;
                return reagent.getFirstUse();
            }
        });
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    private List<SearchTerm> buildReagentEventGroup() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        // This is an exclusive search term using programmatic logic to populate the entity list
        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("LCSET");
        searchTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerm.setAlternateSearchDefinition(buildLcsetAlternateSearchDef());
        searchTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getBatchNameInputConverter());
        searchTerm.setHelpText( "Reagents are collected and consolidated from all descendant events in the LCSET."
            + "\nThe LCSET term is exclusive, no other terms can be selected.");
        // Need criteria path in order to see in list of terms
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria( new ArrayList<String>() );
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);

        searchTerms.add(searchTerm);

        return searchTerms;
    }

    /**
     * Build an alternate search definition to query for lab events
     *    and use programmatic logic to populate the reagent entity list
     * @return
     */
    private ConfigurableSearchDefinition buildLcsetAlternateSearchDef() {
        Map<String, List<SearchTerm>> mapGroupSearchTerms = new LinkedHashMap<>();
        List<SearchTerm> searchTerms = new ArrayList<>();

        // Mercury only cares about workflow batches
        SearchTerm.ImmutableTermFilter workflowOnlyFilter = new SearchTerm.ImmutableTermFilter(
                "labBatchType", SearchInstance.Operator.EQUALS, LabBatch.LabBatchType.WORKFLOW);

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("LCSET");
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList(/* LabEvent*/ "inPlaceLabEvents", /* LabVessel */
                "labBatches", /* LabBatchStartingVessel */ "labBatch" /* LabBatch */));
        criteriaPath.setPropertyName("batchName");
        criteriaPath.addImmutableTermFilter(workflowOnlyFilter);
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);

        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList(/* LabEvent*/ "inPlaceLabEvents", /* LabVessel */
                "reworkLabBatches" /* LabBatch */));
        criteriaPath.setPropertyName("batchName");
        criteriaPath.addImmutableTermFilter(workflowOnlyFilter);
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);

        List<ConfigurableSearchDefinition.CriteriaProjection> criteriaProjections = new ArrayList<>();

        // This will return LabEvents
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("inPlaceLabEvents", "inPlaceLabVesselId",
                "inPlaceLabEvents", LabVessel.class));

        searchTerms.add(searchTerm);

        mapGroupSearchTerms.put("Never Seen", searchTerms);

        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                ColumnEntity.LAB_EVENT, criteriaProjections, mapGroupSearchTerms);

        configurableSearchDefinition.addTraversalEvaluator(ConfigurableSearchDefinition.ALTERNATE_DEFINITION_ID
                , new LcsetReagentTraversalEvaluator() );

        return configurableSearchDefinition;
    }

}
