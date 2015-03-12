package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.ArrayList;
import java.util.Arrays;
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
                ColumnEntity.REAGENT, 100, criteriaProjections, mapGroupSearchTerms);

        return configurableSearchDefinition;
    }

    private List<SearchTerm> buildReagentGroup() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Reagent Name");
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria( Arrays.asList( "ReagentID" ) );
        criteriaPath.setPropertyName("name");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                Reagent reagent = (Reagent) entity;
                return reagent.getName();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Lot Number");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("ReagentID"));
        criteriaPath.setPropertyName("lot");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                Reagent reagent = (Reagent) entity;
                return reagent.getLot();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Expiration Date");
        searchTerm.setTypeExpression(new SearchTerm.Evaluator<String>() {
            @Override
            public String evaluate(Object entity, Map<String, Object> context) {
                return "Date";
            }
        });
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("ReagentID"));
        criteriaPath.setPropertyName("expiration");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                Reagent reagent = (Reagent) entity;
                return reagent.getExpiration();
            }
        });
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    private List<SearchTerm> buildReagentEventGroup() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        // This is an exclusive search term using programmatic logic to populate the entity list
        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("LCSET Events");
        searchTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerm.setAlternateSearchDefinition(buildLcsetAlternateSearchDef());
        searchTerm.setValueConversionExpression(SearchDefinitionFactory.getLcsetInputConverter());
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

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("LCSET Events");
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(
                Arrays.asList(/* LabEvent*/ "inPlaceLabEvents", /* LabVessel */ "bucketEntries", /* BucketEntry */
                        "labBatch" /* LabBatch */));
        criteriaPath.setPropertyName("batchName");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);

        List<ConfigurableSearchDefinition.CriteriaProjection> criteriaProjections = new ArrayList<>();

        // This will return LabEvents
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("inPlaceLabEvents", "inPlaceLabVesselId",
                "inPlaceLabEvents", LabVessel.class));

        searchTerms.add(searchTerm);

        mapGroupSearchTerms.put("Never Seen", searchTerms);

        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                ColumnEntity.LAB_EVENT, 100, criteriaProjections, mapGroupSearchTerms);

        configurableSearchDefinition.addTraversalEvaluator(ConfigurableSearchDefinition.ALTERNATE_DEFINITION_ID
                , new LcsetReagentTraversalEvaluator() );

        return configurableSearchDefinition;
    }

}
