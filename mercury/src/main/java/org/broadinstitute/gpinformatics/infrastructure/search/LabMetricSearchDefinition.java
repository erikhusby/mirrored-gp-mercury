package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.bsp.client.search.Search;
import org.broadinstitute.bsp.client.search.SearchItem;
import org.broadinstitute.bsp.client.search.SearchManager;
import org.broadinstitute.bsp.client.search.SearchResponse;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPGroupCollectionList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.columns.AncestorLabMetricPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.DisplayExpression;
import org.broadinstitute.gpinformatics.infrastructure.columns.LabMetricSampleDataAddRowsListener;
import org.broadinstitute.gpinformatics.infrastructure.columns.SampleDataFetcherAddRowsListener;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds ConfigurableSearchDefinition for lab metric user defined search logic  <br />
 * Ancestry and descendant options provide ability to view metrics for tranfers
 */
public class LabMetricSearchDefinition {

    /**
     * Available user selectable traversal options for lab metric search
     * Terms with alternate search definitions have to access user selected state of these options.
     */
    public enum TraversalEvaluatorName {
        ANCESTORS("ancestorOptionEnabled"), DESCENDANTS("descendantOptionEnabled");

        private final String id;

        TraversalEvaluatorName(String id ) {
            this.id = id;
        }

        public String getId(){
            return id;
        }
    }

    // These search term and/or result column names need to be referenced multiple places during processing.
    // Use an enum rather than having to reference via String values of term names
    // TODO: JMS Create a shared interface that this implements then use this as a registry of all term names
    public enum MultiRefTerm {
        METRIC_RUN_ID("Metric Run ID"),
        METRIC_TUBES_ONLY("Only Show Metrics for Tubes"),
        BSP_PARTICIPANT("Collaborator Patient ID");

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

    public LabMetricSearchDefinition(){
        // Cache the metadata keys related to lab metrics available for display
        metadataDisplayKeyMap = new HashMap<>();
        for( Metadata.Key key : Metadata.Key.values() ) {
            if (key.getCategory() == Metadata.Category.LAB_METRIC
                    || key.getCategory() == Metadata.Category.LAB_METRIC_RUN || key.getCategory() == Metadata.Category.LIQUID_HANDLER_METRIC) {
                metadataDisplayKeyMap.put(key.getDisplayName(), key);
            }
        }

        // Cache the metric types available in search term select list
        metricTypeDisplayKeyMap = new HashMap<>();
        for( LabMetric.MetricType metricType : LabMetric.MetricType.values() ) {
            metricTypeDisplayKeyMap.put(metricType.getDisplayName(), metricType);
        }
    }

    private Map<String, LabMetric.MetricType> metricTypeDisplayKeyMap;
    private Map<String, Metadata.Key> metadataDisplayKeyMap;

    ConfigurableSearchDefinition buildSearchDefinition(){

        LabMetricSearchDefinition srchDef = new LabMetricSearchDefinition();
        Map<String, List<SearchTerm>> mapGroupSearchTerms = new LinkedHashMap<>();

        List<SearchTerm> searchTerms = srchDef.buildLabMetricVessels();
        mapGroupSearchTerms.put("Vessels/Buckets", searchTerms);

        searchTerms = srchDef.buildLabMetricIds();
        mapGroupSearchTerms.put("Metric Attributes", searchTerms);

        searchTerms = srchDef.buildLabMetricMetadata();
        mapGroupSearchTerms.put("Metric Metadata", searchTerms);

        SearchTerm.CriteriaPath sampleKeyCriteriaPath = new SearchTerm.CriteriaPath();
        sampleKeyCriteriaPath.setCriteria(Arrays.asList("metricsVessel", "mercurySamples"));
        sampleKeyCriteriaPath.setPropertyName("sampleKey");

        mapGroupSearchTerms.put("BSP", buildBsp(sampleKeyCriteriaPath));
        mapGroupSearchTerms.put("Sample Repository", listByType(mapGroupSearchTerms.values(),
                DisplayExpression.ExpressionGroup.SAMPLE_REPOSITORY));
        mapGroupSearchTerms.put("Sample Processing", listByType(mapGroupSearchTerms.values(),
                DisplayExpression.ExpressionGroup.SAMPLE_PROCESSING));

        List<ConfigurableSearchDefinition.CriteriaProjection> criteriaProjections = new ArrayList<>();

        ConfigurableSearchDefinition.CriteriaProjection criteriaProjection
                = new ConfigurableSearchDefinition.CriteriaProjection(
                    "metricsVessel", "labVessel", "labMetrics", LabVessel.class);

        criteriaProjections.add(criteriaProjection);

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("reworkLabBatches", "rework.labVesselId",
                "reworks", "rework", LabBatch.class));

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("labMetricId", "labMetricId",
                "labMetricId", LabMetric.class));

        // TODO JMS Halves cost but term might not be used much:  create index mercury.idx_metric_run on mercury.lab_metric ( lab_metric_run ) compute statistics;
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("metricsRun", "metric.labMetricId",
                "labMetrics", "metric", LabMetricRun.class));

        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                ColumnEntity.LAB_METRIC, criteriaProjections, mapGroupSearchTerms);

        // Allow user to search ancestor and/or descendant metrics for initial metrics vessels
        configurableSearchDefinition.addTraversalEvaluator(TraversalEvaluatorName.ANCESTORS.getId()
                , new LabMetricTraversalEvaluator.AncestorTraversalEvaluator());
        configurableSearchDefinition.addTraversalEvaluator(TraversalEvaluatorName.DESCENDANTS.getId()
                , new LabMetricTraversalEvaluator.DescendantTraversalEvaluator());

        configurableSearchDefinition.setAddRowsListenerFactory(
                new ConfigurableSearchDefinition.AddRowsListenerFactory() {
                    @Override
                    public Map<String, ConfigurableList.AddRowsListener> getAddRowsListeners() {
                        Map<String, ConfigurableList.AddRowsListener> listeners = new HashMap<>();
                        // todo jmt collapse to one?
                        listeners.put(LabMetricSampleDataAddRowsListener.class.getSimpleName(),
                                new LabMetricSampleDataAddRowsListener());
                        listeners.put(SampleDataFetcherAddRowsListener.class.getSimpleName(),
                                new SampleDataFetcherAddRowsListener());
                        return listeners;
                    }
                });

        return configurableSearchDefinition;
    }

    private List<SearchTerm> buildLabMetricVessels() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Barcode");
        searchTerm.setRackScanSupported(Boolean.TRUE);
        searchTerm.setDbSortPath("labVessel.label");
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Collections.singletonList("metricsVessel"));
        criteriaPath.setPropertyName("label");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabMetric labMetric = (LabMetric) entity;
                return labMetric.getLabVessel().getLabel();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Lab Batch");
        criteriaPaths = new ArrayList<>();

        // Mercury only cares about workflow batches
        SearchTerm.ImmutableTermFilter workflowOnlyFilter = new SearchTerm.ImmutableTermFilter(
                "labBatchType", SearchInstance.Operator.EQUALS, LabBatch.LabBatchType.WORKFLOW);

        // Non-reworks
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("metricsVessel", "labBatches", "labBatch"));
        criteriaPath.setPropertyName("batchName");
        criteriaPath.addImmutableTermFilter(workflowOnlyFilter);
        criteriaPath.setJoinFetch(Boolean.TRUE);
        criteriaPaths.add(criteriaPath);
        // Reworks
        SearchTerm.CriteriaPath nestedCriteriaPath = new SearchTerm.CriteriaPath();
        nestedCriteriaPath.setCriteria(Collections.singletonList("reworkLabBatches"));

        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("metricsVessel", "labMetrics"));
        criteriaPath.setNestedCriteriaPath(nestedCriteriaPath);
        criteriaPath.setPropertyName("batchName");
        criteriaPath.addImmutableTermFilter(workflowOnlyFilter);
        criteriaPath.setJoinFetch(Boolean.TRUE);
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                Set<String> results = new HashSet<>();
                LabMetric labMetric = (LabMetric) entity;

                // Navigate back to sample(s)
                for (SampleInstanceV2 sampleInstanceV2 : labMetric.getLabVessel().getSampleInstancesV2()) {
                    for( LabBatch labBatch : sampleInstanceV2.getAllWorkflowBatches() ) {
                        results.add(labBatch.getBatchName());
                    }
                }
                return results;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("PDO");
        searchTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getPdoInputConverter());
        criteriaPaths = new ArrayList<>();

        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("metricsVessel", "mercurySamples", "productOrderSamples", "productOrder" ));
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
        searchTerm.setName("Mercury Sample ID");
        searchTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        criteriaPaths = new ArrayList<>();

        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("metricsVessel", "mercurySamples" ));
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

        // User-selectable filter to exclude plate wells from the results
        // Note: Accessed by name in LabMetricTraversalEvaluator to filter ancestors and descendants
        searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.METRIC_TUBES_ONLY.getTermRefName());
        searchTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerm.setValueType(ColumnValueType.NOT_NULL);
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Collections.singletonList("metricsVessel"));
        criteriaPath.setPropertyName("tubeType");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Flowcell Barcode");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {

                LabMetric labMetric = (LabMetric) entity;

                LabVesselSearchDefinition.VesselsForEventTraverserCriteria eval
                        = new LabVesselSearchDefinition.VesselsForEventTraverserCriteria(
                                LabVesselSearchDefinition.FLOWCELL_LAB_EVENT_TYPES);
                labMetric.getLabVessel().evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

                Set<String> barcodes = null;
                for(Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                        : eval.getPositions().asMap().entrySet()) {
                        (barcodes==null?barcodes = new HashSet<>():barcodes)
                                .add(labVesselAndPositions.getKey().getLabel());
                }
                return barcodes;
            }
        });
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    private List<SearchTerm> buildLabMetricMetadata() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        for( Metadata.Key key : metadataDisplayKeyMap.values() ) {
            SearchTerm searchTerm = new SearchTerm();
            searchTerm.setName(key.getDisplayName());
            searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
                @Override
                public String evaluate(Object entity, SearchContext context) {
                    LabMetric labMetric = (LabMetric) entity;
                    Metadata.Key key = metadataDisplayKeyMap.get(context.getSearchTerm().getName());
                    // Check metric metadata
                    for (Metadata metadata : labMetric.getMetadataSet()) {
                        if (metadata.getKey() == key) {
                            return metadata.getValue();
                        }
                    }
                    // Still here? Check run metadata
                    if (labMetric.getLabMetricRun() != null) {
                        for (Metadata metadata : labMetric.getLabMetricRun().getMetadata()) {
                            if (metadata.getKey() == key) {
                                return metadata.getValue();
                            }
                        }
                    }
                    return "";
                }
            });

            searchTerms.add(searchTerm);
        }

        return searchTerms;
    }

    private List<SearchTerm> buildLabMetricIds() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Metric Date");
        searchTerm.setDbSortPath("createdDate");
        searchTerm.setValueType(ColumnValueType.DATE_TIME);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Date evaluate(Object entity, SearchContext context) {
                LabMetric labMetric = (LabMetric) entity;
                return labMetric.getCreatedDate();
            }
        });
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("createdDate");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Metric Type");
        // Not exact (enum type vs. enum display name)
        searchTerm.setDbSortPath("metricType");
        searchTerm.setIsDefaultResultColumn(Boolean.TRUE);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabMetric labMetric = (LabMetric) entity;
                return labMetric.getName().getDisplayName();
            }
        });

        searchTerm.setConstrainedValuesExpression(new SearchTerm.Evaluator<List<ConstrainedValue>>() {
            @Override
            public List<ConstrainedValue> evaluate(Object entity, SearchContext context) {
                List<ConstrainedValue> metricTypeConstrainedValues = new ArrayList<>();
                for( Map.Entry<String,LabMetric.MetricType> mapEntry : metricTypeDisplayKeyMap.entrySet() ) {
                    metricTypeConstrainedValues.add(new ConstrainedValue(mapEntry.getValue().toString(), mapEntry.getKey()));
                }
                return metricTypeConstrainedValues;
            }
        });

        searchTerm.setSearchValueConversionExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<LabMetric.MetricType> evaluate(Object entity, SearchContext context) {
                List<LabMetric.MetricType> values = new ArrayList<>();
                for( String value : context.getSearchValue().getValues() ) {
                    values.add(LabMetric.MetricType.valueOf(value));
                }
                return values;
            }
        });
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("metricType");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);

        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Metric Value");
        searchTerm.setDbSortPath("value");
        searchTerm.setIsDefaultResultColumn(Boolean.TRUE);
        searchTerm.setValueType(ColumnValueType.TWO_PLACE_DECIMAL);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public BigDecimal evaluate(Object entity, SearchContext context) {
                LabMetric labMetric = (LabMetric) entity;
                return labMetric.getValue();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Metric Units");
        // Not exact (enum type vs. enum display name)
        searchTerm.setDbSortPath("labUnit");
        searchTerm.setIsDefaultResultColumn(Boolean.TRUE);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabMetric labMetric = (LabMetric) entity;
                return labMetric.getUnits().getDisplayName();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Metric Run Name");
        searchTerm.setDbSortPath("labMetricRun.runName");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabMetric labMetric = (LabMetric) entity;
                return labMetric.getLabMetricRun() == null? null: labMetric.getLabMetricRun().getRunName();
            }
        });
        criteriaPaths = new ArrayList<>();

        SearchTerm.CriteriaPath nestedCriteriaPath = new SearchTerm.CriteriaPath();
        nestedCriteriaPath.setCriteria(Collections.singletonList("metricsRun"));

        criteriaPath = new SearchTerm.CriteriaPath();
        // Projected property of nested subquery
        criteriaPath.setCriteria(Collections.singletonList("labMetricId"));
        criteriaPath.setNestedCriteriaPath(nestedCriteriaPath);

        criteriaPath.setPropertyName("runName");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.METRIC_RUN_ID.getTermRefName());
        searchTerm.setValueType(ColumnValueType.UNSIGNED);
        searchTerm.setDbSortPath("labMetricRun.labMetricRunId");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Long evaluate(Object entity, SearchContext context) {
                LabMetric labMetric = (LabMetric) entity;
                return labMetric.getLabMetricRun() == null? null: labMetric.getLabMetricRun().getLabMetricRunId();
            }
        });
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();

        // Projected property of nested subquery
        criteriaPath.setCriteria(Collections.singletonList("labMetricId"));
        criteriaPath.setNestedCriteriaPath(nestedCriteriaPath);

        criteriaPath.setPropertyName("labMetricRunId");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Position");
        searchTerm.setDbSortPath("vesselPosition");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabMetric labMetric = (LabMetric) entity;
                return labMetric.getVesselPosition();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Volume");
        searchTerm.setValueType(ColumnValueType.TWO_PLACE_DECIMAL);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public BigDecimal evaluate(Object entity, SearchContext context) {
                LabMetric labMetric = (LabMetric) entity;
                return labMetric.getLabVessel().getVolume();
            }
        });
        searchTerms.add(searchTerm);

        // todo jmt why doesn't Total ng metadata search term work?
        searchTerm = new SearchTerm();
        searchTerm.setName("Metric Total ng");
        searchTerm.setValueType(ColumnValueType.TWO_PLACE_DECIMAL);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public BigDecimal evaluate(Object entity, SearchContext context) {
                LabMetric labMetric = (LabMetric) entity;
                return labMetric.getTotalNg();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Decision");
        searchTerm.setDbSortPath("labMetricDecision.decision");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabMetric labMetric = (LabMetric) entity;
                if( labMetric.getLabMetricDecision() != null ) {
                    return labMetric.getLabMetricDecision().getDecision().toString();
                } else {
                    return null;
                }
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Note");
        searchTerm.setDbSortPath("labMetricDecision.note");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabMetric labMetric = (LabMetric) entity;
                if( labMetric.getLabMetricDecision() != null ) {
                    return labMetric.getLabMetricDecision().getNote();
                } else {
                    return null;
                }
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("User");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabMetric labMetric = (LabMetric) entity;
                if( labMetric.getLabMetricDecision() == null ) {
                    return null;
                }
                // TODO JMS Result column name may be misleading - displaying the user who made the decision.
                // Adding lab event logic would get the user who created the metric.
                BSPUserList bspUserList = context.getBspUserList();
                Long userId = labMetric.getLabMetricDecision().getDeciderUserId();
                BspUser bspUser = bspUserList.getById(userId);
                if (bspUser == null) {
                    return "Unknown user - ID: " + userId;
                }
                return bspUser.getFullName();
            }
        });
        searchTerms.add(searchTerm);


        searchTerm = new SearchTerm();
        searchTerm.setName("Reason");
        searchTerm.setDbSortPath("labMetricDecision.overrideReason");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabMetric labMetric = (LabMetric) entity;
                if( labMetric.getLabMetricDecision() != null ) {
                    return labMetric.getLabMetricDecision().getOverrideReason();
                } else {
                    return null;
                }
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Initial Total ng of stock");
        searchTerm.setValueType(ColumnValueType.TWO_PLACE_DECIMAL);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public BigDecimal evaluate(Object entity, SearchContext context) {
                LabMetric labMetric = (LabMetric) entity;
                Map<LabMetric.MetricType, Set<LabMetric>> mapTypeToMetrics =
                        labMetric.getLabVessel().getMetricsForVesselAndAncestors();
                Set<LabMetric> labMetrics = mapTypeToMetrics.get(LabMetric.MetricType.INITIAL_PICO);
                if (labMetrics != null) {
                    LabMetric mostRecentLabMetric = AncestorLabMetricPlugin.findMostRecentLabMetric(labMetrics);
                    return mostRecentLabMetric.getTotalNg();
                }
                return null;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Ancestor Quants");
        searchTerm.setPluginClass(AncestorLabMetricPlugin.class);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Sample Count");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Integer evaluate(Object entity, SearchContext context) {
                LabMetric labMetric = (LabMetric) entity;
                return labMetric.getLabVessel().getSampleInstancesV2().size();
            }
        });
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    static List<SearchTerm> buildBsp(SearchTerm.CriteriaPath sampleKeyCriteriaPath) {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm workRequestTerm = new SearchTerm();
        workRequestTerm.setName("Work Request ID");
        workRequestTerm.setExternalDataExpression(values -> {
            SearchItem searchItem = new SearchItem("Fullfilled Samples - Work Request ID", "IN", values);
            return runBspSearch(searchItem);
        });
        workRequestTerm.setCriteriaPaths(Collections.singletonList(sampleKeyCriteriaPath));
        workRequestTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
/*
        workRequestTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                return "TBD";
            }
        });
*/
        searchTerms.add(workRequestTerm);
        // todo jmt needs a display expression

        SearchTerm collectionTerm = new SearchTerm();
        collectionTerm.setName("Collection sub");
        collectionTerm.setExternalDataExpression(values -> {
            SearchItem group = new SearchItem("Group / Collection", "EQUALS", values);
            SearchItem collection = new SearchItem("Collection", "EQUALS", values);
            group.setChildren(Collections.singletonList(collection));
            return runBspSearch(group);
        });
        collectionTerm.setConstrainedValuesExpression(new SearchTerm.Evaluator<List<ConstrainedValue>>() {
            @Override
            public List<ConstrainedValue> evaluate(Object entity, SearchContext context) {
                BSPGroupCollectionList groupCollectionList = ServiceAccessUtility.getBean(BSPGroupCollectionList.class);
                Long groupId = Long.valueOf(context.getSearchValue().getParent().getValues().get(0));
                return groupCollectionList.collectionsForGroup(groupId).stream().map(coll ->
                        new ConstrainedValue(Long.toString(coll.getCollectionId()), coll.getCollectionName())).
                        collect(Collectors.toList());
            }
        });
        collectionTerm.setCriteriaPaths(Collections.singletonList(sampleKeyCriteriaPath));
        initBspTerm(collectionTerm, DisplayExpression.COLLECTION);
        collectionTerm.setNewDetachedCriteria(true);

        SearchTerm groupTerm = new SearchTerm();
        List<SearchTerm.CriteriaPath> blankCriteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath blankCriteriaPath = new SearchTerm.CriteriaPath();
        blankCriteriaPath.setCriteria(new ArrayList<>());
        blankCriteriaPaths.add(blankCriteriaPath);
        groupTerm.setCriteriaPaths(blankCriteriaPaths);
        groupTerm.setName("Group / Collection");
        groupTerm.setConstrainedValuesExpression(new SearchTerm.Evaluator<List<ConstrainedValue>>() {
            @Override
            public List<ConstrainedValue> evaluate(Object entity, SearchContext context) {
                BSPGroupCollectionList groupCollectionList = ServiceAccessUtility.getBean(BSPGroupCollectionList.class);
                return groupCollectionList.getGroups().values().stream().map(
                        group -> new ConstrainedValue(Long.toString(group.getGroupId()), group.getGroupName())).
                        collect(Collectors.toList());
            }
        });
        groupTerm.setDependentSearchTerms(Collections.singletonList(collectionTerm));
        groupTerm.setSearchValueConversionExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, SearchContext context) {
                return "NoHibernateCriteria";
            }
        });
        searchTerms.add(groupTerm);

        SearchTerm collabSampleTerm = new SearchTerm();
        collabSampleTerm.setName(DisplayExpression.COLLABORATOR_SAMPLE_ID.getColumnName());
        collabSampleTerm.setExternalDataExpression(values -> {
            SearchItem aliasName = new SearchItem("Sample Alias Name", "EQUALS",
                    Collections.singletonList("Collaborator Sample ID"));
            SearchItem aliasValue = new SearchItem("Sample Alias Value", "IN", values);
            aliasName.setChildren(Collections.singletonList(aliasValue));
            return runBspSearch(aliasName);
        });
        collabSampleTerm.setCriteriaPaths(Collections.singletonList(sampleKeyCriteriaPath));
        initBspTerm(collabSampleTerm, DisplayExpression.COLLABORATOR_SAMPLE_ID);
        searchTerms.add(collabSampleTerm);

        SearchTerm collabParticipantTerm = new SearchTerm();
        collabParticipantTerm.setName(DisplayExpression.COLLABORATOR_PARTICIPANT_ID.getColumnName());
        collabParticipantTerm.setExternalDataExpression(values -> {
            SearchItem aliasName = new SearchItem("Participant Alias Name", "EQUALS",
                    Collections.singletonList("Collaborator Participant ID"));
            SearchItem aliasValue = new SearchItem("Participant Alias Value", "IN", values);
            aliasName.setChildren(Collections.singletonList(aliasValue));
            return runBspSearch(aliasName);
        });
        collabParticipantTerm.setCriteriaPaths(Collections.singletonList(sampleKeyCriteriaPath));
        initBspTerm(collabParticipantTerm, DisplayExpression.COLLABORATOR_PARTICIPANT_ID);
        searchTerms.add(collabParticipantTerm);

        // This is a display-only duplicate of collab PT ID, for backwards compatibility
        SearchTerm collabPatientTerm = new SearchTerm();
        collabPatientTerm.setName(MultiRefTerm.BSP_PARTICIPANT.getTermRefName());
        initBspTerm(collabPatientTerm, DisplayExpression.COLLABORATOR_PARTICIPANT_ID);
        searchTerms.add(collabPatientTerm);

        return searchTerms;
    }

    private static void initBspTerm(SearchTerm collabPatientTerm, DisplayExpression displayExpression) {
        collabPatientTerm.setDisplayExpression(displayExpression);
        collabPatientTerm.setAddRowsListenerHelper(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, SearchContext context) {
                return displayExpression.getBspSampleSearchColumn();
            }
        });
    }

    private List<SearchTerm> listByType(Collection<List<SearchTerm>> existingSearchTerms,
                                        DisplayExpression.ExpressionGroup expressionGroup) {
        List<SearchTerm> searchTerms = new ArrayList<>();
        Set<String> termNames = existingSearchTerms.stream().flatMap(Collection::stream).map(SearchTerm::getName).
                collect(Collectors.toSet());
        for (DisplayExpression displayExpression : DisplayExpression.listByGroup(expressionGroup)) {
            String columnName = displayExpression.getColumnName();
            if (columnName != null && !termNames.contains(columnName)) {
                SearchTerm searchTerm = new SearchTerm();
                searchTerm.setName(columnName);
                searchTerm.setDisplayExpression(displayExpression);
                BSPSampleSearchColumn bspSampleSearchColumn = displayExpression.getBspSampleSearchColumn();
                if (bspSampleSearchColumn != null) {
                    searchTerm.setAddRowsListenerHelper(new SearchTerm.Evaluator<Object>() {
                        @Override
                        public Object evaluate(Object entity, SearchContext context) {
                            return bspSampleSearchColumn;
                        }
                    });
                }
                searchTerms.add(searchTerm);
            }
        }
        return searchTerms;
    }

    @NotNull
    public static List<Object> runBspSearch(SearchItem searchItem) {
        BSPConfig bspConfig = ServiceAccessUtility.getBean(BSPConfig.class);
        SearchManager searchManager = new SearchManager(bspConfig.getHost(), bspConfig.getPort(),
                bspConfig.getLogin(), bspConfig.getPassword());
        Search search = new Search();
        search.setEntityName("Sample");
        search.setSearchItems(Collections.singletonList(searchItem));
        search.setViewColumns(Collections.singletonList("Sample ID"));
        search.setMaxResults(1000000);
        SearchResponse searchResponse = searchManager.runSearch(search);
        if (!searchResponse.isSuccess()) {
            throw new RuntimeException("Failed to fetch from BSP " + searchResponse.getMessages().get(0));
        }
        return searchResponse.getResult().getRows().stream().map(
                strings -> strings.get(0)).collect(Collectors.toList());
    }
}
