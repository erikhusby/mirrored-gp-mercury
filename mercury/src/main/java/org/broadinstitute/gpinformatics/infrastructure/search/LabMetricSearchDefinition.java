package org.broadinstitute.gpinformatics.infrastructure.search;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
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
        criteriaPath.setCriteria(Arrays.asList("metricsVessel"));
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
        nestedCriteriaPath.setCriteria(Arrays.asList("reworkLabBatches"));

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
        searchTerm.setName("Mercury Sample ID");
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
        searchTerm.setName("Only Show Metrics for Tubes");
        searchTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerm.setValueType(ColumnValueType.NOT_NULL);
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("metricsVessel"));
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
                Set<String> barcodes = null;

                List<LabEventType> labEventTypes = new ArrayList<>();
                labEventTypes.add(LabEventType.FLOWCELL_TRANSFER);
                labEventTypes.add(LabEventType.DENATURE_TO_FLOWCELL_TRANSFER);
                labEventTypes.add(LabEventType.DILUTION_TO_FLOWCELL_TRANSFER);

                LabVesselSearchDefinition.VesselDescendantTraverserCriteria eval
                        = new LabVesselSearchDefinition.VesselDescendantTraverserCriteria(labEventTypes );
                labMetric.getLabVessel().evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

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

        SearchTerm searchTerm;
        for( Metadata.Key key : metadataDisplayKeyMap.values() ) {
            searchTerm  = new SearchTerm();
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
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabMetric labMetric = (LabMetric) entity;
                return labMetric.getLabMetricRun() == null? null: labMetric.getLabMetricRun().getRunName();
            }
        });
        criteriaPaths = new ArrayList<>();

        SearchTerm.CriteriaPath nestedCriteriaPath = new SearchTerm.CriteriaPath();
        nestedCriteriaPath.setCriteria(Arrays.asList("metricsRun"));

        criteriaPath = new SearchTerm.CriteriaPath();
        // Projected property of nested subquery
        criteriaPath.setCriteria(Arrays.asList("labMetricId"));
        criteriaPath.setNestedCriteriaPath(nestedCriteriaPath);

        criteriaPath.setPropertyName("runName");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        return searchTerms;
    }

}
