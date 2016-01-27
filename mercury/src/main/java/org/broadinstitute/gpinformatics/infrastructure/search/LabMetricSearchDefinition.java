package org.broadinstitute.gpinformatics.infrastructure.search;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.columns.AncestorLabMetricPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.math.BigDecimal;
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

    public LabMetricSearchDefinition(){
        // Cache the metadata keys related to lab metrics available for display
        metadataDisplayKeyMap = new HashMap<>();
        for( Metadata.Key key : Metadata.Key.values() ) {
            if (key.getCategory() == Metadata.Category.LAB_METRIC
                    || key.getCategory() == Metadata.Category.LAB_METRIC_RUN) {
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

    public ConfigurableSearchDefinition buildSearchDefinition(){

        LabMetricSearchDefinition srchDef = new LabMetricSearchDefinition();
        Map<String, List<SearchTerm>> mapGroupSearchTerms = new LinkedHashMap<>();

        List<SearchTerm> searchTerms = srchDef.buildLabMetricVessels();
        mapGroupSearchTerms.put("Vessels/Buckets", searchTerms);

        searchTerms = srchDef.buildLabMetricIds();
        mapGroupSearchTerms.put("IDs", searchTerms);

        searchTerms = srchDef.buildLabMetricMetadata();
        mapGroupSearchTerms.put("Metadata", searchTerms);

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

        return configurableSearchDefinition;
    }

    private List<SearchTerm> buildLabMetricVessels() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Barcode");
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
        searchTerm.setName("LCSET");
        criteriaPaths = new ArrayList<>();
        // Non-reworks
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("metricsVessel", "labBatches", "labBatch"));
        criteriaPath.setPropertyName("batchName");
        criteriaPath.setJoinFetch(Boolean.TRUE);
        criteriaPaths.add(criteriaPath);
        // Reworks
        SearchTerm.CriteriaPath nestedCriteriaPath = new SearchTerm.CriteriaPath();
        nestedCriteriaPath.setCriteria(Collections.singletonList("reworkLabBatches"));

        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("metricsVessel", "labMetrics"));
        criteriaPath.setNestedCriteriaPath(nestedCriteriaPath);
        criteriaPath.setPropertyName("batchName");
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
        searchTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getLcsetInputConverter());
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
                        results.add(productOrderSample.getProductOrder().getJiraTicketKey());
                    }
                }
                return results;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Nearest Sample ID");
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
                    results.add(sampleInstanceV2.getNearestMercurySampleName());
                }
                return results;
            }
        });
        searchTerms.add(searchTerm);

        // User-selectable filter to exclude plate wells from the results
        // Note: Accessed by name in LabMetricTraversalEvaluator to filter ancestors and descendants
        searchTerm = new SearchTerm();
        searchTerm.setName("Only Show Metrics for Tubes");
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

                List<LabEventType> labEventTypes = new ArrayList<>();
                labEventTypes.add(LabEventType.FLOWCELL_TRANSFER);
                labEventTypes.add(LabEventType.DENATURE_TO_FLOWCELL_TRANSFER);
                labEventTypes.add(LabEventType.DILUTION_TO_FLOWCELL_TRANSFER);

                LabVesselSearchDefinition.VesselDescendantTraverserCriteria eval
                        = new LabVesselSearchDefinition.VesselDescendantTraverserCriteria(labEventTypes );
                labMetric.getLabVessel().evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

                Set<String> barcodes = null;
                if (!eval.getPositions().isEmpty()) {
                    barcodes = new HashSet<>();
                    for( Pair<String,VesselPosition> positionPair : eval.getPositions() ) {
                        barcodes.add(positionPair.getLeft());
                    }
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
                    if (labMetric.getLabMetricRun().getMetadata().isEmpty()) {
                        return "";
                    } else {
                        Metadata.Key key = metadataDisplayKeyMap.get(context.getSearchTerm().getName());
                        for (Metadata metadata : labMetric.getLabMetricRun().getMetadata()) {
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
        searchTerm.setName("Collaborator Patient ID");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                LabMetric labMetric = (LabMetric) entity;
                // todo jmt BSP too
                List<String> patientIds = new ArrayList<>();
                Collections.addAll(patientIds, labMetric.getLabVessel().getMetadataValues(Metadata.Key.PATIENT_ID));
                return patientIds;
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
                return labMetric.getLabMetricDecision().getDecision().toString();
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
                return labMetric.getLabMetricDecision().getNote();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("User");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabMetric labMetric = (LabMetric) entity;
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
                return labMetric.getLabMetricDecision().getOverrideReason();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Root Sample ID");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                List<String> rootSampleIds = new ArrayList<>();
                LabMetric labMetric = (LabMetric) entity;
                for (SampleInstanceV2 sampleInstanceV2 : labMetric.getLabVessel().getSampleInstancesV2()) {
                    rootSampleIds.add(sampleInstanceV2.getMercuryRootSampleName());
                }

                return rootSampleIds;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Product");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                List<String> products = new ArrayList<>();
                LabMetric labMetric = (LabMetric) entity;
                for (SampleInstanceV2 sampleInstanceV2 : labMetric.getLabVessel().getSampleInstancesV2()) {
                    BucketEntry singleBucketEntry = sampleInstanceV2.getSingleBucketEntry();
                    if (singleBucketEntry != null) {
                        products.add(singleBucketEntry.getProductOrder().getProduct().getName());
                    }
                }

                return products;
            }
        });
        searchTerms.add(searchTerm);

        // todo jmt Proceed if OOS

        searchTerm = new SearchTerm();
        searchTerm.setName("Original Material Type");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                LabMetric labMetric = (LabMetric) entity;
                List<String> materialTypes = new ArrayList<>();
                Collections.addAll(materialTypes, labMetric.getLabVessel().getMetadataValues(
                        Metadata.Key.ORIGINAL_MATERIAL_TYPE));
                // todo BSP too
                return materialTypes;
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
                // todo jmt most recent
                LabMetric pico = labMetrics.iterator().next();
                return pico.getTotalNg();
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

}
