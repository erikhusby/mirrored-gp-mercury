package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds ConfigurableSearchDefinition for lab metric user defined search logic  <br />
 * Ancestry and descendant options provide ability to view metrics for tranfers
 */
public class LabMetricRunSearchDefinition {

    public LabMetricRunSearchDefinition(){
        // Cache the run metadata keys related to lab metrics available for display
        runMetadataDisplayKeyMap = new HashMap<>();
        for( Metadata.Key key : Metadata.Key.values() ) {
            if (key.getCategory() == Metadata.Category.LAB_METRIC_RUN) {
                runMetadataDisplayKeyMap.put(key.getDisplayName(), key);
            }
        }
    }

    private Map<String, Metadata.Key> runMetadataDisplayKeyMap;

    // This singleton is used to determine if search is to be expanded to include all ancestor and descendant vessel metric runs
    private static ConfigurableSearchDefinition ALL_VESSEL_METRIC_RUNS_ALT_SRCH_DEFINITION;

    // These search term and/or result column names need to be referenced multiple places during processing.
    // Use an enum rather than having to reference via String values of term names
    // TODO: JMS Create a shared interface that this implements then use this as a registry of all term names
    public enum MultiRefTerm {
        RUN_VESSEL("Barcode"),
        RUN_PDO("PDO"),
        RUN_LCSET("LCSET"),
        RUN_SAMPLE("Mercury Sample ID");

        MultiRefTerm(String termRefName ) {
            this.termRefName = termRefName;
            if( termNameReference.put(termRefName,this) != null ) {
                throw new RuntimeException( "Attempt to add a term with a duplicate name [" + termRefName + "]." );
            }
        }

        private String termRefName;
        private Map<String,MultiRefTerm> termNameReference = new HashMap<>();

        public String getTermRefName() {
            return termRefName;
        }

        public boolean isNamed(String termName ) {
            return termRefName.equals(termName);
        }
    }

    /**
     * Shared value list of all lab metric types.
     */
    static class RunTypeValuesExpression extends SearchTerm.Evaluator<List<ConstrainedValue>> {
        @Override
        public List<ConstrainedValue> evaluate(Object entity, SearchContext context) {
            List<ConstrainedValue> constrainedValues = new ArrayList<>();
            for (LabMetric.MetricType metricType : LabMetric.MetricType.values()) {
                constrainedValues.add(new ConstrainedValue(metricType.toString(), metricType.getDisplayName()));
            }
            Collections.sort(constrainedValues);
            return constrainedValues;
        }
    }

    /**
     * Shared conversion of input String to LabMetric.MetricType enumeration value
     */
    static class RunTypeValueConversionExpression extends SearchTerm.Evaluator<Object> {
        @Override
        public LabMetric.MetricType evaluate(Object entity, SearchContext context) {
            return Enum.valueOf(LabMetric.MetricType.class, context.getSearchValueString());
        }
    }

    public ConfigurableSearchDefinition buildSearchDefinition(){

        ALL_VESSEL_METRIC_RUNS_ALT_SRCH_DEFINITION = buildMetricRunByVesselAltDefinition();

        Map<String, List<SearchTerm>> mapGroupSearchTerms = new LinkedHashMap<>();

        List<SearchTerm> searchTerms = buildMetricRunByVessels();
        mapGroupSearchTerms.put("Vessels/Buckets", searchTerms);

        searchTerms = buildLabMetricRunIds();
        mapGroupSearchTerms.put("IDs", searchTerms);

        searchTerms = buildLabMetricMetadata();
        mapGroupSearchTerms.put("Metadata", searchTerms);

        searchTerms = buildDrillDowns();
        mapGroupSearchTerms.put("Drill Downs", searchTerms);

        List<ConfigurableSearchDefinition.CriteriaProjection> criteriaProjections = new ArrayList<>();

        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                ColumnEntity.LAB_METRIC_RUN, criteriaProjections, mapGroupSearchTerms);

        return configurableSearchDefinition;
    }

    private List<SearchTerm> buildMetricRunByVessels() {
        List<SearchTerm> searchTerms = new ArrayList<>();
        List<SearchTerm.CriteriaPath> emptyCriteriaPaths = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.RUN_VESSEL.getTermRefName());
        searchTerm.setRackScanSupported(Boolean.TRUE);
        searchTerm.setCriteriaPaths(emptyCriteriaPaths);
        searchTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerm.setAlternateSearchDefinition(ALL_VESSEL_METRIC_RUNS_ALT_SRCH_DEFINITION);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.RUN_LCSET.getTermRefName());
        searchTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getBatchNameInputConverter());
        searchTerm.setCriteriaPaths(emptyCriteriaPaths);
        searchTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerm.setAlternateSearchDefinition(ALL_VESSEL_METRIC_RUNS_ALT_SRCH_DEFINITION);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.RUN_PDO.getTermRefName());
        searchTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getPdoInputConverter());
        searchTerm.setCriteriaPaths(emptyCriteriaPaths);
        searchTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerm.setAlternateSearchDefinition(ALL_VESSEL_METRIC_RUNS_ALT_SRCH_DEFINITION);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.RUN_SAMPLE.getTermRefName());
        searchTerm.setCriteriaPaths(emptyCriteriaPaths);
        searchTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerm.setAlternateSearchDefinition(ALL_VESSEL_METRIC_RUNS_ALT_SRCH_DEFINITION);
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    /**
     * Build an alternate search definition to query for lab vessels. Then use programmatic logic (traversal evaluator) </br >
     *  to populate the metric run list with all vessel ancestor and descendant metric runs.
     * @return
     */
    private ConfigurableSearchDefinition buildMetricRunByVesselAltDefinition() {

        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.RUN_VESSEL.getTermRefName());
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("label");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.RUN_LCSET.getTermRefName());
        criteriaPaths = new ArrayList<>();

        // Mercury only cares about workflow batches for metric runs
        SearchTerm.ImmutableTermFilter workflowOnlyFilter = new SearchTerm.ImmutableTermFilter(
                "labBatchType", SearchInstance.Operator.EQUALS, LabBatch.LabBatchType.WORKFLOW);

        // Non-reworks
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("labBatches", "labBatch"));
        criteriaPath.setPropertyName("batchName");
        criteriaPath.addImmutableTermFilter(workflowOnlyFilter);
        criteriaPath.setJoinFetch(Boolean.TRUE);
        criteriaPaths.add(criteriaPath);

        // Reworks
        SearchTerm.CriteriaPath nestedCriteriaPath = new SearchTerm.CriteriaPath();
        nestedCriteriaPath.setCriteria(Collections.singletonList("reworkLabBatches"));
        nestedCriteriaPath.addImmutableTermFilter(workflowOnlyFilter);

        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("vesselById", "labVesselId"));
        criteriaPath.setNestedCriteriaPath(nestedCriteriaPath);
        criteriaPath.setPropertyName("batchName");
        criteriaPath.setJoinFetch(Boolean.TRUE);
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getBatchNameInputConverter());
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.RUN_PDO.getTermRefName());
        searchTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getPdoInputConverter());
        criteriaPaths = new ArrayList<>();

        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("mercurySamples", "mercurySamples", "productOrderSamples", "productOrder" ));
        criteriaPath.setPropertyName("jiraTicketKey");
        criteriaPaths.add(criteriaPath);

        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                Set<String> results = new HashSet<>();
                LabMetric labMetric = (LabMetric) entity;
                for (SampleInstanceV2 sampleInstanceV2 : labMetric.getLabVessel().getSampleInstancesV2()) {
                    for (ProductOrderSample productOrderSample : sampleInstanceV2.getAllProductOrderSamples() ) {
                        results.add(productOrderSample.getProductOrder().getBusinessKey());
                    }
                }
                return results;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.RUN_SAMPLE.getTermRefName());
        searchTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        criteriaPaths = new ArrayList<>();

        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("mercurySamples", "mercurySamples" ));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPaths.add(criteriaPath);

        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                Set<String> results = new HashSet<>();
                LabMetric labMetric = (LabMetric) entity;
                for (SampleInstanceV2 sampleInstanceV2 : labMetric.getLabVessel().getSampleInstancesV2()) {
                    for (MercurySample mercurySample: sampleInstanceV2.getRootMercurySamples() ) {
                        results.add(mercurySample.getSampleKey());
                    }
                }
                return results;
            }
        });
        searchTerms.add(searchTerm);

        List<ConfigurableSearchDefinition.CriteriaProjection> criteriaProjections = new ArrayList<>();

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("labBatches", "labVesselId",
                "labVessel", LabBatchStartingVessel.class));

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("vesselById", "labVesselId",
                "labVesselId", LabVessel.class));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("reworkLabBatches", "rework.labVesselId",
                "reworks", "rework", LabBatch.class));

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("mercurySamples", "labVesselId",
                "mercurySamples", LabVessel.class));

        Map<String, List<SearchTerm>> mapGroupSearchTerms = new LinkedHashMap<>();
        mapGroupSearchTerms.put("Never Seen", searchTerms);

        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                ColumnEntity.LAB_VESSEL, criteriaProjections, mapGroupSearchTerms);

        // Convert list of lab vessels to list of ancestor and descendant lab metric runs
        configurableSearchDefinition.addTraversalEvaluator(ConfigurableSearchDefinition.ALTERNATE_DEFINITION_ID
                , new LabMetricRunTraversalEvaluator());

        return configurableSearchDefinition;
    }

    private List<SearchTerm> buildLabMetricMetadata() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        for( Metadata.Key key : runMetadataDisplayKeyMap.values() ) {
            SearchTerm searchTerm = new SearchTerm();
            searchTerm.setName(key.getDisplayName());
            searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
                @Override
                public String evaluate(Object entity, SearchContext context) {
                    LabMetricRun labMetricRun = (LabMetricRun) entity;
                    if (labMetricRun.getMetadata().isEmpty()) {
                        return "";
                    } else {
                        Metadata.Key key = runMetadataDisplayKeyMap.get(context.getSearchTerm().getName());
                        for (Metadata metadata : labMetricRun.getMetadata()) {
                            if( metadata.getKey() == key ) {
                                return metadata.getValue();
                            }
                        }
                        return "";
                    }
                }
            });

            searchTerms.add(searchTerm);
        }

        return searchTerms;
    }

    private List<SearchTerm> buildLabMetricRunIds() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Metric Run Name");
        searchTerm.setDbSortPath("runName");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabMetricRun labMetricRun = (LabMetricRun) entity;
                return labMetricRun.getRunName();
            }
        });
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();

        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("runName");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Metric Date");
        searchTerm.setDbSortPath("runDate");
        searchTerm.setValueType(ColumnValueType.DATE_TIME);
        searchTerm.setIsDefaultResultColumn(Boolean.TRUE);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Date evaluate(Object entity, SearchContext context) {
                LabMetricRun labMetricRun = (LabMetricRun) entity;
                return labMetricRun.getRunDate();
            }
        });
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("runDate");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Metric Type");
        searchTerm.setDbSortPath("metricType");
        searchTerm.setIsDefaultResultColumn(Boolean.TRUE);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabMetricRun labMetricRun = (LabMetricRun) entity;
                return labMetricRun.getMetricType().getDisplayName();
            }
        });
        searchTerm.setConstrainedValuesExpression( new RunTypeValuesExpression() );
        searchTerm.setSearchValueConversionExpression( new RunTypeValueConversionExpression() );
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("metricType");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    private List<SearchTerm> buildDrillDowns() {
        List<SearchTerm> searchTerms = new ArrayList<>();
        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Run Drill Down");
        searchTerm.setIsDefaultResultColumn(Boolean.TRUE);
        searchTerm.setMustEscape(false);

        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {

            final String drillDownSearchName = "GLOBAL|GLOBAL_LAB_METRIC_SEARCH_INSTANCES|Run Drill Down";
            final String drillDownIdSearchTerm = LabMetricSearchDefinition.MultiRefTerm.METRIC_RUN_ID.getTermRefName();
            final String drillDownTubesSearchTerm = LabMetricSearchDefinition.MultiRefTerm.METRIC_TUBES_ONLY.getTermRefName();

            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabMetricRun labMetricRun = (LabMetricRun) entity;

                Long labMetricRunId = labMetricRun.getLabMetricRunId();

                Map<String, String[]> terms = new HashMap<>();
                terms.put(drillDownIdSearchTerm, new String[]{labMetricRunId.toString()});
                terms.put(drillDownTubesSearchTerm, new String[]{"N/A"});
                return SearchDefinitionFactory.buildDrillDownLink(labMetricRun.getRunName(), ColumnEntity.LAB_METRIC, drillDownSearchName, terms, context);
            }
        });

        searchTerms.add(searchTerm);

        return searchTerms;
    }
}
