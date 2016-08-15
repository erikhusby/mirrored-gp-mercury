package org.broadinstitute.gpinformatics.infrastructure.search;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.columns.BspSampleSearchAddRowsListener;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.LabVesselArrayMetricPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.LabVesselLatestEventPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.LabVesselMetadataPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.LabVesselMetricPlugin;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds ConfigurableSearchDefinition for lab vessel user defined search logic
 */
public class LabVesselSearchDefinition {

    // This singleton is used to determine if search is related specifically to Infinium arrays
    private static ConfigurableSearchDefinition ARRAYS_ALT_SRCH_DEFINITION;

    // These search term and/or result column names need to be referenced multiple places during processing.
    // Use an enum rather than having to reference via String values of term names
    // TODO: JMS Create a shared interface that this implements then use this as a registry of all term names
    public enum MultiRefTerm {
        INFINIUM_PDO("Infinium PDO"),
        INFINIUM_DNA_PLATE("DNA Array Plate Barcode"),
        INFINIUM_AMP_PLATE("Amp Plate Barcode"),
        INFINIUM_CHIP("Infinium Chip Barcode");

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

    public ConfigurableSearchDefinition buildSearchDefinition(){

        ARRAYS_ALT_SRCH_DEFINITION = buildArraysAlternateSearchDefinition();

        LabVesselSearchDefinition srchDef = new LabVesselSearchDefinition();
        Map<String, List<SearchTerm>> mapGroupSearchTerms = new LinkedHashMap<>();

        List<SearchTerm> searchTerms = srchDef.buildLabVesselIds();
        mapGroupSearchTerms.put("IDs", searchTerms);

        // Are there alternatives to search terms that aren't searchable?  Should they be in a different structure, then merged with search terms for display?

        // XX version - from workflow? 3.2 doesn't seem to be in XML
        // Start date - LabBatch.createdOn? usually 1 day before "scheduled to start"
        // Due date - LabBatch.dueDate is transient!
        searchTerms = srchDef.buildLabVesselBsp();
        mapGroupSearchTerms.put("BSP", searchTerms);

        searchTerms = srchDef.buildLabVesselMetadata();
        mapGroupSearchTerms.put("Mercury Metadata", searchTerms);

        searchTerms = srchDef.buildLabVesselBuckets();
        mapGroupSearchTerms.put("Buckets", searchTerms);

        searchTerms = srchDef.buildLabVesselEvent();
        mapGroupSearchTerms.put("Events", searchTerms);

        searchTerms = srchDef.buildRackScanTerms();
        mapGroupSearchTerms.put("Rack Scan Data", searchTerms);

        searchTerms = srchDef.buildMetricsTerms();
        mapGroupSearchTerms.put("Metrics", searchTerms);

        searchTerms = srchDef.buildArrayTerms();
        mapGroupSearchTerms.put("Arrays", searchTerms);

        searchTerms = srchDef.buildLabVesselMultiCols();
        mapGroupSearchTerms.put("Multi-Columns", searchTerms);

        // Raising volume to 65ul - sample annotation?
        // Billing Risk - from PDO
        // Kapa QC Score - uploaded
        // Pull sample if low input and Kapa QC? - sample annotation?

        // Exported/Daughter Volume - ?
        // Exported/Daughter Concentration - ?
        // Calculated ng into shearing - ?
        // Lab Risk - sample annotation?
        // Requested Sequencing Deliverable - from PDO?
        // Pond Quant - upload
        // Rework low pond - sample annotation?
        // Index adapter - reagent?
        // Remaining Pond Volume after Plex / SPRI Concentration - ?
        // Plex Tube Barcode - lab event
        // Plexed SPRI Concentration Tube Barcode - lab event
        // MiSeq Sample Barcode - lab event
        // Per Pool Eco Quant for MiSeq Sample - lab event
        // Normalization Tube Barcode for MiSeq Sample - lab event
        // Denature Barcode for MiSeq Sample - lab event
        // % representation of each sample within each plex - ?
        // SPRI Concentration tube / Catch Tube Pooling Penalty - ?
        // Catch Quant - upload
        // Rework Low Catch - sample annotation?
        // Per Plex Eco Quant for Catch Tube - ?
        // Normalization Tube Barcode for Catch Tube - lab event
        // Denature IDs and which samples are in those tubes - lab event
        // Total lanes sequenced per Denature tube - ?
        // HiSeq FCTs and Barcode and which samples were on those - partly lab batch
        // Mean Target Coverage - PDO?
        // % Target Bases 20x - PDO?
        // Penalty 20x - PDO?
        // HS Library Size - ?
        // PF Reads - ?
        // % Duplication - ?
        // % Contamination - ?
        // Status on requested coverage - ?
        // Total Lanes Sequenced per Sample - ?
        // Comments - sample annotation?

        List<ConfigurableSearchDefinition.CriteriaProjection> criteriaProjections = new ArrayList<>();
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("bucketEntries", "labVesselId",
                "labVessel", BucketEntry.class));

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("labBatches", "labVesselId",
                "labVessel", LabBatchStartingVessel.class));

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("vesselById", "labVesselId",
                "labVesselId", LabVessel.class));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("reworkLabBatches", "rework.labVesselId",
                "reworks", "rework", LabBatch.class));

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("mercurySample", "labVesselId",
                "mercurySamples", LabVessel.class));

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("inPlaceLabVesselId", "labVesselId",
                "inPlaceLabVesselId", LabEvent.class));

        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                ColumnEntity.LAB_VESSEL, criteriaProjections, mapGroupSearchTerms);

        configurableSearchDefinition.setAddRowsListenerFactory(
                new ConfigurableSearchDefinition.AddRowsListenerFactory() {
                    @Override
                    public Map<String, ConfigurableList.AddRowsListener> getAddRowsListeners() {
                        Map<String, ConfigurableList.AddRowsListener> listeners = new HashMap<>();
                        listeners.put(BspSampleSearchAddRowsListener.class.getSimpleName(), new BspSampleSearchAddRowsListener());
                        return listeners;
                    }
                });

        return configurableSearchDefinition;
    }

    private List<SearchTerm> buildLabVesselIds() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        // LCSET and XTR batches are filtered by workflow batches
        SearchTerm.ImmutableTermFilter workflowOnlyFilter = new SearchTerm.ImmutableTermFilter(
                "labBatchType", SearchInstance.Operator.EQUALS, LabBatch.LabBatchType.WORKFLOW);
        // LCSET batches are filtered by name prefix = LCSET-
        SearchTerm.ImmutableTermFilter lscetBatchFilter = new SearchTerm.ImmutableTermFilter(
                "batchName", SearchInstance.Operator.LIKE, "LCSET-%");
        // XTR batches are filtered by name prefix = XTR-
        SearchTerm.ImmutableTermFilter xtrBatchFilter = new SearchTerm.ImmutableTermFilter(
                "batchName", SearchInstance.Operator.LIKE, "XTR-%");
        // FCT batches are filtered by workflow batches
        SearchTerm.ImmutableTermFilter fctOnlyFilter = new SearchTerm.ImmutableTermFilter(
                "labBatchType", SearchInstance.Operator.IN, LabBatch.LabBatchType.FCT, LabBatch.LabBatchType.MISEQ);


        // todo jmt look at search inputs, to show only LCSET that was searched on?
        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("LCSET");
        searchTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getLcsetInputConverter());
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                Set<String> results = new HashSet<>();
                LabVessel labVessel = (LabVessel) entity;
                // Navigate back to sample(s)
                for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                    for( LabBatch labBatch : sampleInstanceV2.getAllWorkflowBatches() ) {
                        if( labBatch.getBatchName().startsWith("LCSET")) {
                            results.add(labBatch.getBatchName());
                        }
                    }
                }
                return results;
            }
        });

        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        // Non-reworks
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("labBatches", "labBatch"));
        criteriaPath.setPropertyName("batchName");
        criteriaPath.addImmutableTermFilter(workflowOnlyFilter);
        criteriaPath.addImmutableTermFilter(lscetBatchFilter);
        criteriaPath.setJoinFetch(Boolean.TRUE);
        criteriaPaths.add(criteriaPath);
        // Reworks
        SearchTerm.CriteriaPath nestedCriteriaPath = new SearchTerm.CriteriaPath();
        nestedCriteriaPath.setCriteria(Arrays.asList("reworkLabBatches"));
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("vesselById", "labVesselId"));
        criteriaPath.setNestedCriteriaPath(nestedCriteriaPath);
        criteriaPath.setPropertyName("batchName");
        criteriaPath.setJoinFetch(Boolean.TRUE);
        criteriaPath.addImmutableTermFilter(workflowOnlyFilter);
        criteriaPath.addImmutableTermFilter(lscetBatchFilter);
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("XTR");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                Set<String> results = null;
                LabVessel labVessel = (LabVessel) entity;
                // Navigate back to sample(s)
                for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                    for( LabBatch labBatch : sampleInstanceV2.getAllWorkflowBatches() ) {
                        if( labBatch.getBatchName().startsWith("XTR")) {
                            (results==null?results = new HashSet<>():results)
                                    .add(labBatch.getBatchName());
                        }
                    }
                }
                return results;
            }
        });

        criteriaPaths = new ArrayList<>();
        // Non-reworks
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("labBatches", "labBatch"));
        criteriaPath.setPropertyName("batchName");
        criteriaPath.addImmutableTermFilter(workflowOnlyFilter);
        criteriaPath.addImmutableTermFilter(xtrBatchFilter);
        criteriaPath.setJoinFetch(Boolean.TRUE);
        criteriaPaths.add(criteriaPath);
        // Reworks
        nestedCriteriaPath = new SearchTerm.CriteriaPath();
        nestedCriteriaPath.setCriteria(Arrays.asList("reworkLabBatches"));
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("vesselById", "labVesselId"));
        criteriaPath.setNestedCriteriaPath(nestedCriteriaPath);
        criteriaPath.setPropertyName("batchName");
        criteriaPath.setJoinFetch(Boolean.TRUE);
        criteriaPath.addImmutableTermFilter(workflowOnlyFilter);
        criteriaPath.addImmutableTermFilter(xtrBatchFilter);
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("FCT");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                Set<String> results = null;
                LabVessel labVessel = (LabVessel) entity;

                VesselBatchTraverserCriteria downstreamBatchFinder = new VesselBatchTraverserCriteria();
                if( labVessel.getContainerRole() != null ) {
                    labVessel.getContainerRole().applyCriteriaToAllPositions(
                            downstreamBatchFinder, TransferTraverserCriteria.TraversalDirection.Ancestors);
                } else {
                    labVessel.evaluateCriteria(
                            downstreamBatchFinder, TransferTraverserCriteria.TraversalDirection.Ancestors);
                }

                for ( LabBatch labBatch : downstreamBatchFinder.getLabBatches() ) {
                    if( labBatch.getLabBatchType() == LabBatch.LabBatchType.FCT
                            || labBatch.getLabBatchType() == LabBatch.LabBatchType.MISEQ ) {
                        (results==null?results = new HashSet<>():results).add(labBatch.getBatchName());
                    }
                }
                return results;
            }
        });

        criteriaPaths = new ArrayList<>();
        // Non-reworks
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("labBatches", "labBatch"));
        criteriaPath.setPropertyName("batchName");
        criteriaPath.addImmutableTermFilter(fctOnlyFilter);
        criteriaPath.setJoinFetch(Boolean.TRUE);
        criteriaPaths.add(criteriaPath);
        // No FCT reworks, another ticket is created
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Barcode");
        searchTerm.setIsDefaultResultColumn(Boolean.TRUE);
        searchTerm.setRackScanSupported(Boolean.TRUE);
        searchTerm.setDbSortPath("label");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("label");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                return labVessel.getLabel();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Vessel Type");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                return SearchDefinitionFactory.findVesselType(labVessel);
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Vessel Volume");
        searchTerm.setDbSortPath("volume");
        searchTerm.setValueType(ColumnValueType.TWO_PLACE_DECIMAL);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public BigDecimal evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                return labVessel.getVolume();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Vessel Concentration");
        searchTerm.setDbSortPath("concentration");
        searchTerm.setValueType(ColumnValueType.TWO_PLACE_DECIMAL);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public BigDecimal evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                return labVessel.getConcentration();
            }
        });
        searchTerms.add(searchTerm);
        return searchTerms;
    }

    private List<SearchTerm> buildLabVesselBsp() {
        List<SearchTerm> searchTerms = new ArrayList<>();
        // Non-searchable data from BSP
        searchTerms.add(buildLabVesselBspTerm(BSPSampleSearchColumn.STOCK_SAMPLE, "Stock Sample ID"));
        searchTerms.add(buildLabVesselBspTerm(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID));
        searchTerms.add(buildLabVesselBspTerm(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID ));
        searchTerms.add(buildLabVesselBspTerm(BSPSampleSearchColumn.SAMPLE_TYPE, "Tumor / Normal"));
        searchTerms.add(buildLabVesselBspTerm(BSPSampleSearchColumn.COLLECTION));
        searchTerms.add(buildLabVesselBspTerm(BSPSampleSearchColumn.ORIGINAL_MATERIAL_TYPE));
        return searchTerms;
    }

    /**
     * Builds BSP term with default display name
     * @param bspSampleSearchColumn
     * @return
     */
    private SearchTerm buildLabVesselBspTerm(final BSPSampleSearchColumn bspSampleSearchColumn) {
        return buildLabVesselBspTerm(bspSampleSearchColumn, bspSampleSearchColumn.columnName());
    }

    /**
     * Builds BSP term with user specified display name
     * @param bspSampleSearchColumn
     * @param name
     * @return
     */
    private SearchTerm buildLabVesselBspTerm(final BSPSampleSearchColumn bspSampleSearchColumn, String name) {
        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName(name);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                List<String> results = null;
                Collection<MercurySample> mercurySamples = labVessel.getMercurySamples();
                if (!mercurySamples.isEmpty()) {
                    BspSampleSearchAddRowsListener bspColumns =
                            (BspSampleSearchAddRowsListener) context.getRowsListener(BspSampleSearchAddRowsListener.class.getSimpleName());
                    results = new ArrayList<>();
                    for( MercurySample mercurySample : mercurySamples) {
                        results.add(bspColumns.getColumn(mercurySample.getSampleKey(), bspSampleSearchColumn));
                    }
                }
                return results;
            }
        });
        searchTerm.setAddRowsListenerHelper(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, SearchContext context) {
                return bspSampleSearchColumn;
            }
        });
        return searchTerm;
    }

    private List<SearchTerm> buildLabVesselBuckets() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        // Sample History (1st Plating, Rework from Shearing, Rework from LC, Rework from Pooling, 2nd Plating)
        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Sample History");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                List<String> results = new ArrayList<>();
                LabVessel labVessel = (LabVessel) entity;
                for (BucketEntry bucketEntry : labVessel.getBucketEntries()) {
                    if (bucketEntry.getLabBatch() != null) {
                        Bucket bucket = bucketEntry.getBucket();
                        if (bucketEntry.getReworkDetail() == null) {
                            if ("Pico/Plating Bucket".equals(bucket.getBucketDefinitionName())) {
                                results.add("1st Plating");
                            }
                        } else {
                            switch (bucket.getBucketDefinitionName()) {
                            case "Pico/Plating Bucket":
                                results.add("2nd Plating");
                                break;
                            case "Shearing Bucket":
                                results.add("Rework from Shearing");
                                break;
                            case "Hybridization Bucket":
                                results.add("Rework from LC");
                                break;
                            case "Pooling Bucket":
                                results.add("Rework from Pooling");
                                break;
                            }
                        }
                    }
                }
                return results;
            }
        });
        searchTerms.add(searchTerm);

        // PDO
        searchTerm = new SearchTerm();
        searchTerm.setName("PDO");
        searchTerm.setDbSortPath("bucketEntries.productOrder.jiraTicketKey");
        searchTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getPdoInputConverter());
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();

        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("mercurySample", "mercurySamples", "productOrderSamples", "productOrder" ));
        criteriaPath.setPropertyName("jiraTicketKey");
        criteriaPaths.add(criteriaPath);

        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                Set<String> results = null;
                LabVessel labVessel = (LabVessel) entity;
                for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                    for (ProductOrderSample productOrderSample : sampleInstanceV2.getAllProductOrderSamples() ) {
                        (results==null?results = new HashSet<>():results)
                                .add(productOrderSample.getProductOrder().getJiraTicketKey());
                    }
                }
                return results;
            }
        });
        searchTerms.add(searchTerm);

        // Count of buckets this vessel belongs to
        searchTerm = new SearchTerm();
        searchTerm.setName("Bucket Count");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("bucketEntriesCount");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setValueType(ColumnValueType.UNSIGNED);
        searchTerm.setSearchValueConversionExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, SearchContext context) {
                return Integer.valueOf( context.getSearchValueString());
            }
        } );
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Integer evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                return labVessel.getBucketEntriesCount();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Research Project");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                Set<String> results = null;
                LabVessel labVessel = (LabVessel) entity;
                for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                    for (ProductOrderSample productOrderSample : sampleInstanceV2.getAllProductOrderSamples() ) {
                        if( productOrderSample.getProductOrder().getResearchProject() != null
                                && productOrderSample.getProductOrder().getResearchProject().getName() != null) {
                            (results==null?results = new HashSet<>():results)
                                    .add(productOrderSample.getProductOrder().getResearchProject().getName());
                        }
                    }
                }
                return results;
            }
        });
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    private List<SearchTerm> buildLabVesselEvent() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("In-Place Event Type");
        // Results are too vague when vessels are in multiple events...
        // searchTerm.setDbSortPath("inPlaceLabEvents.labEventType");
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("inPlaceLabVesselId" /* LabEvent */));
        criteriaPath.setPropertyName("labEventType");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                List<String> labEventNames = new ArrayList<>();
                LabVessel labVessel = (LabVessel) entity;
                Set<LabEvent> labVesselInPlaceLabEvents = labVessel.getInPlaceLabEvents();
                for (LabEvent labVesselInPlaceLabEvent : labVesselInPlaceLabEvents) {
                    labEventNames.add(labVesselInPlaceLabEvent.getLabEventType().getName());
                }
                return labEventNames;
            }
        });
        searchTerm.setConstrainedValuesExpression(new SearchDefinitionFactory.EventTypeValuesExpression());
        searchTerm.setSearchValueConversionExpression(new SearchDefinitionFactory.EventTypeValueConversionExpression());
        searchTerms.add(searchTerm);

        class EventMaterialTypeEvaluator extends SearchTerm.Evaluator<Object> {

            private final MaterialType materialType;

            EventMaterialTypeEvaluator(MaterialType materialType) {
                this.materialType = materialType;
            }

            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;

                MaterialTypeEventCriteria criteria = new MaterialTypeEventCriteria(materialType);
                labVessel.evaluateCriteria(criteria, TransferTraverserCriteria.TraversalDirection.Descendants);

                List<String> barcodes = new ArrayList<>();
                for (LabVessel vessel : criteria.getLabVessels()) {
                    barcodes.add(vessel.getLabel());
                }
                return barcodes;
            }
        }
        searchTerm = new SearchTerm();
        searchTerm.setName("DNA Extracted Tube Barcode");
        searchTerm.setDisplayValueExpression(new EventMaterialTypeEvaluator(MaterialType.DNA));
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("RNA Extracted Tube Barcode");
        searchTerm.setDisplayValueExpression(new EventMaterialTypeEvaluator(MaterialType.RNA));
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Imported Sample Tube Barcode");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                Set<String> barcodes = null;

                VesselsForEventTraverserCriteria eval = new VesselsForEventTraverserCriteria(
                        Collections.singletonList(LabEventType.SAMPLE_IMPORT), true );
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

                for (Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                        : eval.getPositions().asMap().entrySet()) {
                    (barcodes==null?barcodes=new HashSet<>():barcodes)
                            .add(labVesselAndPositions.getKey().getLabel());
                }
                return barcodes;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Imported Sample ID");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                List<String> results = new ArrayList<>();
                LabVessel labVessel = (LabVessel) entity;
                Map<LabEvent, Set<LabVessel>> mapEventToVessels = labVessel.findVesselsForLabEventType(
                        LabEventType.SAMPLE_IMPORT, true);
                for (Map.Entry<LabEvent, Set<LabVessel>> eventVesselEntry : mapEventToVessels.entrySet()) {
                    for (LabVessel vessel : eventVesselEntry.getValue()) {
                        Collection<MercurySample> mercurySamples = vessel.getMercurySamples();
                        if (!mercurySamples.isEmpty()) {
                            results.add(mercurySamples.iterator().next().getSampleKey());
                        }
                    }
                }
                return results;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Imported Sample Position");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                Set<String> positions = null;

                VesselsForEventTraverserCriteria eval
                        = new VesselsForEventTraverserCriteria(Collections.singletonList(LabEventType.SAMPLE_IMPORT), true );
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

                for(Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                        : eval.getPositions().asMap().entrySet()) {
                    for( VesselPosition position : labVesselAndPositions.getValue() ) {
                        (positions==null?positions = new HashSet<>():positions)
                                .add(position.toString());
                    }
                }
                return positions;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Pond Sample Position");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                Set<String> positions = null;

                VesselsForEventTraverserCriteria eval
                        = new VesselsForEventTraverserCriteria(Collections.singletonList(LabEventType.POND_REGISTRATION) );
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

                for(Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                        : eval.getPositions().asMap().entrySet()) {
                    for( VesselPosition position : labVesselAndPositions.getValue() ) {
                        (positions==null?positions = new HashSet<>():positions)
                                .add(position.toString());
                    }
                }
                return positions;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Pond Tube Barcode");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                Set<String> barcodes = null;

                VesselsForEventTraverserCriteria eval = new VesselsForEventTraverserCriteria(
                        Collections.singletonList(LabEventType.POND_REGISTRATION) );
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

                for(Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                        : eval.getPositions().asMap().entrySet()) {
                    (barcodes==null?barcodes = new HashSet<>():barcodes)
                            .add(labVesselAndPositions.getKey().getLabel());
                }
                return barcodes;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Shearing Sample Position");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                Set<String> positions = null;

                VesselsForEventTraverserCriteria eval = new VesselsForEventTraverserCriteria(
                        Collections.singletonList(LabEventType.SHEARING_TRANSFER), false, false);
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

                for(Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                        : eval.getPositions().asMap().entrySet()) {
                    for( VesselPosition position : labVesselAndPositions.getValue() ) {
                        (positions==null?positions = new HashSet<>():positions)
                                .add(position.toString());
                    }
                }
                return positions;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Shearing Sample Barcode");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                Set<String> barcodes = null;

                VesselsForEventTraverserCriteria eval = new VesselsForEventTraverserCriteria(
                        Collections.singletonList(LabEventType.SHEARING_TRANSFER), false, false);
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

                for(Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                        : eval.getPositions().asMap().entrySet()) {
                    (barcodes==null?barcodes = new HashSet<>():barcodes)
                            .add(labVesselAndPositions.getKey().getLabel());
                }
                return barcodes;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Catch Sample Position");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {

                LabVessel labVessel = (LabVessel) entity;
                Set<String> positions = null;

                List<LabEventType> labEventTypes = new ArrayList<>();
                // ICE
                labEventTypes.add(LabEventType.ICE_CATCH_ENRICHMENT_CLEANUP);
                // Agilent
                labEventTypes.add(LabEventType.NORMALIZED_CATCH_REGISTRATION);

                VesselsForEventTraverserCriteria eval = new VesselsForEventTraverserCriteria(labEventTypes );
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

                for(Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                        : eval.getPositions().asMap().entrySet()) {
                    for( VesselPosition position : labVesselAndPositions.getValue() ) {
                        (positions==null?positions = new HashSet<>():positions)
                                .add(position.toString());
                    }
                }
                return positions;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Catch Tube Barcode");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {

                LabVessel labVessel = (LabVessel) entity;
                Set<String> barcodes = null;

                List<LabEventType> labEventTypes = new ArrayList<>();
                // ICE
                labEventTypes.add(LabEventType.ICE_CATCH_ENRICHMENT_CLEANUP);
                // Agilent
                labEventTypes.add(LabEventType.NORMALIZED_CATCH_REGISTRATION);

                VesselsForEventTraverserCriteria eval
                        = new VesselsForEventTraverserCriteria(labEventTypes );
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

                for(Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                        : eval.getPositions().asMap().entrySet()) {
                    (barcodes==null?barcodes = new HashSet<>():barcodes)
                            .add(labVesselAndPositions.getKey().getLabel());
                }
                return barcodes;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Flowcell Barcode");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {

                LabVessel labVessel = (LabVessel) entity;
                Set<String> barcodes = null;

                List<LabEventType> labEventTypes = new ArrayList<>();
                labEventTypes.add(LabEventType.FLOWCELL_TRANSFER);
                labEventTypes.add(LabEventType.DENATURE_TO_FLOWCELL_TRANSFER);
                labEventTypes.add(LabEventType.DILUTION_TO_FLOWCELL_TRANSFER);

                VesselsForEventTraverserCriteria eval
                        = new VesselsForEventTraverserCriteria( labEventTypes );
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

                for(Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                        : eval.getPositions().asMap().entrySet()) {
                    (barcodes==null?barcodes=new HashSet<String>():barcodes)
                            .add(labVesselAndPositions.getKey().getLabel());
                }
                return barcodes;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("EmergeVolumeTransfer SM-ID");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                List<String> results = new ArrayList<>();

                Map<LabEvent, Set<LabVessel>> mapEventToVessels = labVessel.findVesselsForLabEventType(
                        LabEventType.EMERGE_VOLUME_TRANSFER, true,
                        EnumSet.of(TransferTraverserCriteria.TraversalDirection.Descendants));
                for (Map.Entry<LabEvent, Set<LabVessel>> labEventSetEntry : mapEventToVessels.entrySet()) {
                    for (LabVessel vessel : labEventSetEntry.getValue()) {
                        for (SampleInstanceV2 sampleInstanceV2 : vessel.getSampleInstancesV2()) {
                            results.add(sampleInstanceV2.getNearestMercurySampleName());
                        }
                    }
                }
                return results;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Rack Barcode");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                List<String> results = new ArrayList<>();

                for (LabVessel vessel : labVessel.getContainers()) {
                    if (OrmUtil.proxySafeIsInstance(vessel, TubeFormation.class)) {
                        TubeFormation tubeFormation = OrmUtil.proxySafeCast(vessel, TubeFormation.class);
                        for (RackOfTubes rackOfTubes : tubeFormation.getRacksOfTubes()) {
                            results.add(rackOfTubes.getLabel());
                        }
                    }
                }
                return results;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Rack Position");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                List<String> results = new ArrayList<>();

                for (LabVessel container : labVessel.getContainers()) {
                    results.add(container.getContainerRole().getPositionOfVessel(labVessel).toString());
                }
                return results;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Downstream LCSET");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                Set<String> results = new HashSet<>();

                VesselBatchTraverserCriteria downstreamBatchFinder = new VesselBatchTraverserCriteria();
                if( labVessel.getContainerRole() != null ) {
                    labVessel.getContainerRole().applyCriteriaToAllPositions(
                            downstreamBatchFinder, TransferTraverserCriteria.TraversalDirection.Descendants);
                } else {
                    labVessel.evaluateCriteria(
                            downstreamBatchFinder, TransferTraverserCriteria.TraversalDirection.Descendants);
                }

                for ( LabBatch labBatch : downstreamBatchFinder.getLabBatches() ) {
                    if( labBatch.getLabBatchType() == LabBatch.LabBatchType.WORKFLOW
                            && labBatch.getBatchName().startsWith("LCSET-") ) {
                        results.add(labBatch.getBatchName());
                    }
                }
                return results;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Downstream XTR");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                Set<String> results = new HashSet<>();

                VesselBatchTraverserCriteria downstreamBatchFinder = new VesselBatchTraverserCriteria();
                if( labVessel.getContainerRole() != null ) {
                    labVessel.getContainerRole().applyCriteriaToAllPositions(
                            downstreamBatchFinder, TransferTraverserCriteria.TraversalDirection.Descendants);
                } else {
                    labVessel.evaluateCriteria(
                            downstreamBatchFinder, TransferTraverserCriteria.TraversalDirection.Descendants);
                }

                for ( LabBatch labBatch : downstreamBatchFinder.getLabBatches() ) {
                    if( labBatch.getLabBatchType() == LabBatch.LabBatchType.WORKFLOW
                            && labBatch.getBatchName().startsWith("XTR-") ) {
                        results.add(labBatch.getBatchName());
                    }
                }
                return results;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Downstream FCT");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                Set<String> results = new HashSet<>();

                VesselBatchTraverserCriteria downstreamBatchFinder = new VesselBatchTraverserCriteria();
                if( labVessel.getContainerRole() != null ) {
                    labVessel.getContainerRole().applyCriteriaToAllPositions(
                            downstreamBatchFinder, TransferTraverserCriteria.TraversalDirection.Descendants);
                } else {
                    labVessel.evaluateCriteria(
                            downstreamBatchFinder, TransferTraverserCriteria.TraversalDirection.Descendants);
                }

                for ( LabBatch labBatch : downstreamBatchFinder.getLabBatches() ) {
                    LabBatch.LabBatchType batchType = labBatch.getLabBatchType();
                    if(batchType == LabBatch.LabBatchType.FCT || batchType == LabBatch.LabBatchType.MISEQ ) {
                        results.add(labBatch.getBatchName());
                    }
                }
                return results;
            }
        });
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    /**
     * Build sample metadata search terms for lab vessels.
     * @return List of search terms/column definitions for lab vessel sample metadata
     */
    private List<SearchTerm> buildLabVesselMetadata() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Mercury Sample ID");
        searchTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("mercurySample", "mercurySamples"));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPath.setJoinFetch(Boolean.TRUE);
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Nearest Sample ID");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                List<String> values = new ArrayList<>();
                LabVessel labVessel = (LabVessel) entity;
                for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                    values.add(sampleInstanceV2.getNearestMercurySampleName());
                }
                return values;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Root Sample ID");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                List<String> values = new ArrayList<>();
                LabVessel labVessel = (LabVessel) entity;
                for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                    values.add(sampleInstanceV2.getRootOrEarliestMercurySampleName());
                }
                return values;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Mercury Sample Tube Barcode");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                List<String> values = new ArrayList<>();
                LabVessel labVessel = (LabVessel) entity;
                for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                    if (sampleInstanceV2.getInitialLabVessel() != null) {
                        values.add(sampleInstanceV2.getInitialLabVessel().getLabel());
                    }
                }
                return values;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Total ng at Initial Pico");
        searchTerm.setValueType(ColumnValueType.TWO_PLACE_DECIMAL);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public BigDecimal evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                List<LabMetric> labMetrics = new ArrayList<>(labVessel.getMetrics());
                for (Iterator<LabMetric> iter = labMetrics.iterator(); iter.hasNext(); ) {
                    if (iter.next().getName() != LabMetric.MetricType.INITIAL_PICO) {
                        iter.remove();
                    }
                }
                return labMetrics.size() > 0 ? LabVesselMetricPlugin.latestTubeMetric(labMetrics).getTotalNg() : null;
            }
        });
        searchTerm.setSearchValueConversionExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, SearchContext context) {
                return new BigDecimal( context.getSearchValueString());
            }
        });
        searchTerms.add(searchTerm);

        // ***** Build sample metadata child search term (the metadata value) ***** //
        List<SearchTerm> childSearchTerms = new ArrayList<>();
        searchTerm = new SearchTerm();
        searchTerm.setName("Metadata Value");
        // This is needed to show the selected metadata column term in the results.
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                String values = "";

                SearchInstance.SearchValue searchValue = context.getSearchValue();
                String header = searchValue.getParent().getValues().get(0);
                Metadata.Key key = Metadata.Key.valueOf(header);

                LabVessel labVessel = (LabVessel) entity;
                Collection<MercurySample> samples = labVessel.getMercurySamples();
                for (MercurySample sample : samples) {
                    Set<Metadata> metadataSet = sample.getMetadata();
                    for (Metadata meta : metadataSet) {
                        if (meta.getKey() == key) {
                            values += meta.getValue();
                            // Ignore possibility of multiples
                            break;
                        }
                    }
                }
                return values;
            }
        });
        searchTerm.setViewHeaderExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                String header;
                SearchInstance.SearchValue searchValue = context.getSearchValue();
                header = searchValue.getParent().getValues().get(0);
                Metadata.Key key = Metadata.Key.valueOf(header);
                if (key != null) {
                    return key.getDisplayName();
                } else {
                    return header;
                }
            }
        });
        searchTerm.setValueTypeExpression(new SearchTerm.Evaluator<ColumnValueType>() {
            @Override
            public ColumnValueType evaluate(Object entity, SearchContext context) {
                SearchInstance.SearchValue searchValue = context.getSearchValue();
                String metaName = searchValue.getParent().getValues().get(0);
                Metadata.Key key = Metadata.Key.valueOf(metaName);
                switch (key.getDataType()) {
                case STRING:
                    return ColumnValueType.STRING;
                case NUMBER:
                    return ColumnValueType.TWO_PLACE_DECIMAL;
                case DATE:
                    return ColumnValueType.DATE;
                }
                throw new RuntimeException("Unhandled data type " + key.getDataType());
            }
        });
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("mercurySample", "mercurySamples", "metadata" /* Metadata */));
        criteriaPath.setPropertyNameExpression(new SearchTerm.Evaluator<String>() {
            @Override
            // Defensive coding
            //   - as of 10/02/2014 sample metadata values are stored in value column (JPA aliased as "stringValue").
            public String evaluate(Object entity, SearchContext context) {
                SearchInstance.SearchValue searchValue = context.getSearchValue();
                String metaName = searchValue.getParent().getValues().get(0);
                Metadata.Key key = Metadata.Key.valueOf(metaName);
                switch (key.getDataType()) {
                case STRING:
                    return "stringValue";
                case NUMBER:
                    return "numberValue";
                case DATE:
                    return "dateValue";
                }
                throw new RuntimeException("Unhandled data type " + key.getDataType());
            }
        });
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        childSearchTerms.add(searchTerm);

        // *****  Build parent search term (the metadata name) ***** //
        searchTerm = new SearchTerm();
        searchTerm.setName("Sample Metadata");
        searchTerm.setConstrainedValuesExpression(new SearchTerm.Evaluator<List<ConstrainedValue>>() {
            @Override
            public List<ConstrainedValue> evaluate(Object entity, SearchContext context) {
                List<ConstrainedValue> constrainedValues = new ArrayList<>();
                for (Metadata.Key meta : Metadata.Key.values()) {
                    if (meta.getCategory() == Metadata.Category.SAMPLE) {
                        constrainedValues.add(new ConstrainedValue(meta.toString(), meta.getDisplayName()));
                    }
                }
                Collections.sort(constrainedValues);
                return constrainedValues;
            }
        });
        searchTerm.setSearchValueConversionExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, SearchContext context) {
                return Enum.valueOf(Metadata.Key.class, context.getSearchValueString());
            }
        });
        // Don't want this option in selectable columns
        searchTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerm.setDependentSearchTerms(childSearchTerms);
        searchTerm.setAddDependentTermsToSearchTermList(Boolean.TRUE);
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("mercurySample", "mercurySamples", "metadata"));
        criteriaPath.setPropertyName("key");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        // ******** Allow individual selectable result columns for each sample metadata value *******
        SearchDefinitionFactory.SampleMetadataDisplayExpression sampleMetadataDisplayExpression = new SearchDefinitionFactory.SampleMetadataDisplayExpression();
        for (Metadata.Key meta : Metadata.Key.values()) {
            if (meta.getCategory() == Metadata.Category.SAMPLE) {
                searchTerm = new SearchTerm();
                searchTerm.setName(meta.getDisplayName());
                searchTerm.setDisplayValueExpression(sampleMetadataDisplayExpression);
                searchTerms.add(searchTerm);
            }
        }

        return searchTerms;
    }

    /**
     * Build search terms to display details about a rack scan term
     * @return List of search terms/column definitions for lab vessel rack scan
     */
    private List<SearchTerm> buildRackScanTerms() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Scan Date");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                JSONObject scanData = context.getScanData();
                if( scanData == null ) {
                    return null;
                }
                try {
                    return scanData.getString("scanDate");
                } catch (JSONException e) {
                    throw new RuntimeException("Failure getting Scan Date from rack scan data");
                }
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Scanner Name");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                JSONObject scanData = context.getScanData();
                if( scanData == null ) {
                    return null;
                }
                try {
                    return scanData.getString("scannerName");
                } catch (JSONException e) {
                    throw new RuntimeException("Failure getting Scanner Name from rack scan data");
                }
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Scan User");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                JSONObject scanData = context.getScanData();
                if( scanData == null ) {
                    return null;
                }
                try {
                    return scanData.getString("scanUser");
                } catch (JSONException e) {
                    throw new RuntimeException("Failure getting Scan User from rack scan data");
                }
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Scan Rack Barcode");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                JSONObject scanData = context.getScanData();
                if( scanData == null ) {
                    return null;
                }
                try {
                    String rackBarcode = scanData.getString("rackBarcode");
                    return rackBarcode == null? "(Not Scanned)": rackBarcode;
                } catch (JSONException e) {
                    throw new RuntimeException("Failure getting Scan Rack Barcode from rack scan data");
                }
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Scan Position");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                JSONObject scanData = context.getScanData();
                if( scanData == null ) {
                    return null;
                }
                String vesselBarcode = ((LabVessel)entity).getLabel();
                try {
                    JSONArray scans = scanData.getJSONArray("scans");
                    JSONObject barcodeAndPosition;
                    for( int i = 0; i < scans.length(); i++ ){
                        barcodeAndPosition = (JSONObject) scans.get(i);
                        if( vesselBarcode.equals(barcodeAndPosition.getString("barcode")) ){
                            return barcodeAndPosition.getString("position");
                        }
                    }
                } catch (JSONException e) {
                    throw new RuntimeException("Failure getting Scan Position from rack scan data");
                }
                return null;
            }
        });
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    /**
     * Build search terms to display details for array chips and plates processing<br/>
     * <strong>Note: Search term names are dependent on links built in LabEventSearchDefinition#buildLabEventDrillDownLinks</strong>
     * @return List of search terms/column definitions for array processing
     */
    private List<SearchTerm> buildArrayTerms() {
        List<SearchTerm> searchTerms = new ArrayList<>();
        SearchTerm searchTerm;

        // Need a non-functional criteria path to make terms with alternate definitions visible in selection list
        List<SearchTerm.CriteriaPath> blankCriteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath blankCriteriaPath = new SearchTerm.CriteriaPath();
        blankCriteriaPath.setCriteria(new ArrayList<String>());
        blankCriteriaPaths.add(blankCriteriaPath);

        final List<LabEventType> platingEventTypes
                = Collections.singletonList(LabEventType.ARRAY_PLATING_DILUTION);
        final List<LabEventType> ampPlateEventTypes
                = Collections.singletonList(LabEventType.INFINIUM_AMPLIFICATION);
        final List<LabEventType> chipEventTypes
                = Collections.singletonList(LabEventType.INFINIUM_HYBRIDIZATION);

        // Not available in results - PDO should be used
        searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.INFINIUM_PDO.getTermRefName());
        searchTerm.setHelpText(
                "Infinium PDO term locates DNA array plate vessels associated with the PDO.  Use array drill-down option result columns to obtain details of Amp plates and chips.<br>"
                        + "Note: The Infinium PDO term is exclusive, no other terms can be selected.");
        searchTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getPdoInputConverter());
        searchTerm.setAlternateSearchDefinition(ARRAYS_ALT_SRCH_DEFINITION);
        searchTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerm.setCriteriaPaths(blankCriteriaPaths);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("DNA Plate Well");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabVessel vessel = (LabVessel)entity;

                // Ignore for all but Infinium DNA Plate wells as source vessel
                if(!isInfiniumSearch(context) || vessel.getType() != LabVessel.ContainerType.PLATE_WELL) {
                    return null;
                }

                return vessel.getContainers().iterator().next().getContainerRole().getPositionOfVessel(vessel).toString();
            }
        });

        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.INFINIUM_DNA_PLATE.getTermRefName());
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                return getInfiniumDnaPlateBarcode( (LabVessel)entity, context );
            }
        });
        searchTerm.setAlternateSearchDefinition(ARRAYS_ALT_SRCH_DEFINITION);
        searchTerm.setCriteriaPaths(blankCriteriaPaths);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Amp Plate Well");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                String result = null;
                LabVessel vessel = (LabVessel)entity;

                // Ignore for all but Infinium DNA Plate wells as source vessel
                if(!isInfiniumSearch(context) || vessel.getType() != LabVessel.ContainerType.PLATE_WELL) {
                    return null;
                }

                // Every event/vessel looks to descendant for amp plate barcode
                VesselsForEventTraverserCriteria infiniumAncestorCriteria = new VesselsForEventTraverserCriteria(
                        ampPlateEventTypes, true, true);
                vessel.evaluateCriteria(infiniumAncestorCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);

                for (Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                        : infiniumAncestorCriteria.getPositions().asMap().entrySet()) {
                    result = labVesselAndPositions.getValue().iterator().next().toString();
                    break;
                }

                return result;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.INFINIUM_AMP_PLATE.getTermRefName());
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                return getInfiniumAmpPlateBarcode((LabVessel)entity, context);
            }
        });
        searchTerm.setAlternateSearchDefinition(ARRAYS_ALT_SRCH_DEFINITION);
        searchTerm.setCriteriaPaths(blankCriteriaPaths);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Chip Well");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                String result = null;
                LabVessel vessel = (LabVessel)entity;

                // DNA plate well event/vessel looks to descendant for chip well (1:1)
                for (Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                        : getChipDetailsForDnaWell(vessel, chipEventTypes, context).asMap().entrySet()) {
                    result = labVesselAndPositions.getValue().iterator().next().toString();
                    break;
                }

                return result;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.INFINIUM_CHIP.getTermRefName());
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                String result = null;
                LabVessel vessel = (LabVessel)entity;

                for (Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                        : getChipDetailsForDnaWell(vessel, chipEventTypes, context ).asMap().entrySet()) {
                    result = labVesselAndPositions.getKey().getLabel();
                    break;
                }

                return result;
            }
        });
        searchTerm.setAlternateSearchDefinition(ARRAYS_ALT_SRCH_DEFINITION);
        searchTerm.setCriteriaPaths(blankCriteriaPaths);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Infinium Array Metrics");
        searchTerm.setPluginClass(LabVesselArrayMetricPlugin.class);
        searchTerms.add(searchTerm);

        // Build result columns which drill down to other searches
        // Note: Links are dependent on search term names built in LabVesselSearchDefinition#buildArrayTerms
        searchTerm = new SearchTerm();
        searchTerm.setName("Infinium DNA Plate Drill Down");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                return getInfiniumDnaPlateBarcode( (LabVessel)entity, context );

            }
        });
        searchTerm.setUiDisplayOutputExpression(new SearchTerm.Evaluator<String>() {
            final String drillDownSearchName = "GLOBAL|GLOBAL_LAB_VESSEL_SEARCH_INSTANCES|DNA Plate Drill Down";
            final String drillDownSearchTerm = MultiRefTerm.INFINIUM_DNA_PLATE.getTermRefName();

            @Override
            public String evaluate(Object value, SearchContext context) {
                String results = null;
                String barcode = (String)value;

                if( barcode == null || barcode.isEmpty() ) {
                    return results;
                }

                Map<String, String[]> terms = new HashMap<>();
                terms.put(drillDownSearchTerm, new String[]{barcode.toString()});
                return SearchDefinitionFactory.buildDrillDownLink(barcode.toString(), ColumnEntity.LAB_VESSEL, drillDownSearchName, terms, context);
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Infinium Amp Plate Drill Down");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                return getInfiniumAmpPlateBarcode((LabVessel)entity, context);
            }
        });
        searchTerm.setUiDisplayOutputExpression(new SearchTerm.Evaluator<String>() {
            final String drillDownSearchName = "GLOBAL|GLOBAL_LAB_VESSEL_SEARCH_INSTANCES|Amp Plate Drill Down";
            final String drillDownSearchTerm = MultiRefTerm.INFINIUM_AMP_PLATE.getTermRefName();

            @Override
            public String evaluate(Object value, SearchContext context) {
                String results = null;
                String barcode = (String)value;

                if( barcode == null || barcode.isEmpty() ) {
                    return results;
                }

                Map<String, String[]> terms = new HashMap<>();
                terms.put(drillDownSearchTerm, new String[]{barcode});
                return SearchDefinitionFactory.buildDrillDownLink(barcode, ColumnEntity.LAB_VESSEL, drillDownSearchName, terms, context);
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Infinium Chip Drill Down");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                String result = null;
                LabVessel vessel = (LabVessel)entity;

                for (Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                        : getChipDetailsForDnaWell(vessel, chipEventTypes, context ).asMap().entrySet()) {
                    result = labVesselAndPositions.getKey().getLabel();
                    break;
                }

                return result;
            }
        });
        searchTerm.setUiDisplayOutputExpression(new SearchTerm.Evaluator<String>() {
            final String drillDownSearchName = "GLOBAL|GLOBAL_LAB_VESSEL_SEARCH_INSTANCES|Infinium Chip Drill Down";
            final String drillDownSearchTerm = MultiRefTerm.INFINIUM_CHIP.getTermRefName();

            @Override
            public String evaluate(Object value, SearchContext context) {
                String results = null;
                String barcode = (String)value;

                if( barcode == null || barcode.isEmpty() ) {
                    return results;
                }

                Map<String, String[]> terms = new HashMap<>();
                terms.put(drillDownSearchTerm, new String[]{barcode});
                return SearchDefinitionFactory.buildDrillDownLink(barcode, ColumnEntity.LAB_VESSEL, drillDownSearchName, terms, context);
            }
        });
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    /**
     * Build an alternate search definition to query for array related lab vessels.
     * In the end, use programmatic logic to populate the lab vessel list with downstream Infinium vessels for a PDO
     * search or DNA plate well entities for Infinium vessel barcodes
     * @return
     */
    private ConfigurableSearchDefinition buildArraysAlternateSearchDefinition() {
        Map<String, List<SearchTerm>> mapGroupSearchTerms = new LinkedHashMap<>();
        List<SearchTerm> searchTerms = new ArrayList<>();

        // By Infinium PDO
        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.INFINIUM_PDO.getTermRefName());
        searchTerm.setIsExcludedFromResultColumns(Boolean.TRUE);

        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(
                Arrays.asList("mercurySample", "mercurySamples", "productOrderSamples", "productOrder"));
        criteriaPath.setPropertyName("jiraTicketKey");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        // Next 3 all use the same criteria
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("label");
        criteriaPaths.add(criteriaPath);

        searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.INFINIUM_DNA_PLATE.getTermRefName());
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.INFINIUM_AMP_PLATE.getTermRefName());
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.INFINIUM_CHIP.getTermRefName());
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        mapGroupSearchTerms.put("Never Seen", searchTerms);

        List<ConfigurableSearchDefinition.CriteriaProjection> criteriaProjections = new ArrayList<>();

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("mercurySample", "labVesselId",
                "mercurySamples", LabVessel.class));

        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                ColumnEntity.LAB_VESSEL,
                criteriaProjections,
                mapGroupSearchTerms);

        // Restrict results to strictly Infinium related vessels
        configurableSearchDefinition.addTraversalEvaluator(ConfigurableSearchDefinition.ALTERNATE_DEFINITION_ID
                , new InfiniumPlateSourceEvaluator() );

        return configurableSearchDefinition;
    }

    /**
     * Build search terms to display details about a rack scan term
     * @return List of search terms/column definitions for lab vessel rack scan
     */
    private List<SearchTerm> buildMetricsTerms(){
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Initial Pico");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                String value = "";

                LabVessel labVessel = (LabVessel)entity;
                // Pico is always ancestry?
                List<LabMetric> metrics = labVessel.getNearestMetricsOfType(LabMetric.MetricType.INITIAL_PICO);
                if( metrics != null && !metrics.isEmpty() ) {
                    LabMetric latestMetric = metrics.get(metrics.size()-1);
                    value = ColumnValueType.TWO_PLACE_DECIMAL.format(latestMetric.getValue(),"") + " " + latestMetric.getUnits().getDisplayName();
                }

                return value;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Proceed if OOS");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                Set<String> results = new HashSet<>();
                LabVessel labVessel = (LabVessel)entity;

                for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                    for (ProductOrderSample productOrderSample : sampleInstanceV2.getAllProductOrderSamples()) {
                        ProductOrderSample.ProceedIfOutOfSpec proceedIfOutOfSpec =
                                productOrderSample.getProceedIfOutOfSpec();
                        if (proceedIfOutOfSpec == null) {
                            proceedIfOutOfSpec = ProductOrderSample.ProceedIfOutOfSpec.NO;
                        }
                        results.add(proceedIfOutOfSpec.getDisplayName());
                    }
                }
                return results;
            }
        });
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    /**
     * Build multi column search terms for lab vessels.
     * @return List of search terms/column definitions for lab vessel multi-column data sets
     */
    private List<SearchTerm> buildLabVesselMultiCols() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("All Sample Metadata");
        searchTerm.setPluginClass(LabVesselMetadataPlugin.class);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Vessel Concentrations");
        searchTerm.setPluginClass(LabVesselMetricPlugin.class);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Vessel Latest Event");
        searchTerm.setPluginClass(LabVesselLatestEventPlugin.class);
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    /**
     * All Infinium related columns depend on search term being related to Infinium arrays.
     * @param context Used to determine if term has array alternate search definition attached
     * @return True if the search term is related to Infinium arrays
     */
    public static boolean isInfiniumSearch( SearchContext context ){
        ConfigurableSearchDefinition alternateSearchDefinition =
                context.getSearchInstance().getSearchValues().iterator().next().getSearchTerm().getAlternateSearchDefinition();
        return ARRAYS_ALT_SRCH_DEFINITION.equals(alternateSearchDefinition);
    }

    /**
     * Gets the DNA plate barcode for any Infinium vessel (DNA Plate well, amp plate, chip)
     * @param infiniumVessel Any vessel associated with infinium process
     * @param context Search context holding various shared objects
     * @return Always a single plate associated with any downstream vessel/position
     */
    private String getInfiniumDnaPlateBarcode(LabVessel infiniumVessel, SearchContext context ) {
        String result = null;
        if(!isInfiniumSearch(context)) {
            return null;
        }

        // All Infinium vessels look in ancestors for dna plate barcode
        VesselsForEventTraverserCriteria infiniumAncestorCriteria = new VesselsForEventTraverserCriteria(
                Collections.singletonList(LabEventType.ARRAY_PLATING_DILUTION), true, true);
        if( infiniumVessel.getContainerRole() == null ) {
            infiniumVessel.evaluateCriteria(infiniumAncestorCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors);
        } else {
            // This coordinates with the directly plated shortcut in InfiniumPlateSourceEvaluator.getAllInfiniumVessels
            boolean found = false;
            for (SectionTransfer sectionTransfer : infiniumVessel.getContainerRole().getSectionTransfersTo()) {
                if (sectionTransfer.getLabEvent().getLabEventType() == LabEventType.ARRAY_PLATING_DILUTION) {
                    infiniumAncestorCriteria.getPositions().put(infiniumVessel, VesselPosition.A01);
                    found = true;
                }
            }

            if (!found) {
                infiniumVessel.getContainerRole().applyCriteriaToAllPositions(infiniumAncestorCriteria,
                        TransferTraverserCriteria.TraversalDirection.Ancestors);
            }
        }

        for (Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                : infiniumAncestorCriteria.getPositions().asMap().entrySet()) {
            LabVessel plateVessel = labVesselAndPositions.getKey();
            if( plateVessel.getType() == LabVessel.ContainerType.PLATE_WELL ) {
                result = plateVessel.getContainers().iterator().next().getLabel();
                break;
            } else if( plateVessel.getType() == LabVessel.ContainerType.STATIC_PLATE ) {
                result = plateVessel.getLabel();
            }
        }

        return result;
    }

    public String getInfiniumAmpPlateBarcode(LabVessel infiniumVessel, SearchContext context) {
        String result = null;
        if(!isInfiniumSearch(context)) {
            return null;
        }

        // Infinium vessels look in ancestors and descendants for amp plate barcode
        VesselsForEventTraverserCriteria infiniumAncestorCriteria = new VesselsForEventTraverserCriteria(
                Collections.singletonList(LabEventType.INFINIUM_AMPLIFICATION), true, true);
        if( infiniumVessel.getContainerRole() == null ) {
            infiniumVessel.evaluateCriteria(infiniumAncestorCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
            // Try ancestors
            if( infiniumAncestorCriteria.getPositions().isEmpty() ) {
                infiniumAncestorCriteria.resetAllTraversed();
                infiniumVessel.evaluateCriteria(infiniumAncestorCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors);
            }
        } else {
            // This coordinates with the directly plated shortcut in InfiniumPlateSourceEvaluator.getAllInfiniumVessels
            boolean found = false;
            for (SectionTransfer sectionTransfer : infiniumVessel.getContainerRole().getSectionTransfersTo()) {
                if (sectionTransfer.getLabEvent().getLabEventType() == LabEventType.ARRAY_PLATING_DILUTION) {
                    for (SectionTransfer transfer : sectionTransfer.getTargetVesselContainer().getSectionTransfersFrom()) {
                        if (transfer.getLabEvent().getLabEventType() == LabEventType.INFINIUM_AMPLIFICATION) {
                            infiniumAncestorCriteria.getPositions().put(
                                    transfer.getTargetVesselContainer().getEmbedder(), VesselPosition.A01);
                            found = true;
                        }
                    }
                }
            }

            if (!found) {
                infiniumVessel.getContainerRole().applyCriteriaToAllPositions(infiniumAncestorCriteria,
                        TransferTraverserCriteria.TraversalDirection.Descendants);
                // Try ancestors
                if( infiniumAncestorCriteria.getPositions().isEmpty() ) {
                    infiniumAncestorCriteria.resetAllTraversed();
                    infiniumVessel.getContainerRole().applyCriteriaToAllPositions(infiniumAncestorCriteria,
                            TransferTraverserCriteria.TraversalDirection.Ancestors);
                }
            }
        }

        for(Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                : infiniumAncestorCriteria.getPositions().asMap().entrySet()) {
            result = labVesselAndPositions.getKey().getLabel();
            break;
        }

        return result;
    }


    /**
     * Given a LabVessel representing a DNA plate well, get details of the associated Infinium plate and position
     * @param dnaPlateWell The DNA plate well to get the downstream Infinium chip details for.
     * @param chipEventTypes The downstream event type(s) to capture to get Infinium chip details <br />
     *                       ( INFINIUM_HYBRIDIZATION, but allow for flexibility )
     * @param context SearchContext containing values associated with search instance
     * @return All downstream vessels and associated positions, if initial vessel not a plate well, ignore and return empty Map
     */
    private MultiValuedMap<LabVessel, VesselPosition> getChipDetailsForDnaWell( LabVessel dnaPlateWell, List<LabEventType> chipEventTypes, SearchContext context ) {
        if(!isInfiniumSearch(context) || dnaPlateWell.getType() != LabVessel.ContainerType.PLATE_WELL ) {
            return new HashSetValuedHashMap<>();
        }

        // Every Infinium event/vessel looks to descendant for chip barcode
        VesselsForEventTraverserCriteria infiniumDescendantCriteria = new VesselsForEventTraverserCriteria(
                chipEventTypes, false, true);

        if( dnaPlateWell.getContainerRole() == null ) {
            dnaPlateWell.evaluateCriteria(infiniumDescendantCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
        } else {
            dnaPlateWell.getContainerRole().applyCriteriaToAllPositions(infiniumDescendantCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
        }

        return infiniumDescendantCriteria.getPositions();
    }

    /**
     * Searches for lab vessel ancestor or descendant events of specific type(s)
     * Records barcode and position of all descendant event vessel(s)
     * Optional flags:
     *   Stop at first descendant event found (default is to continue on to all descendant events)
     *   Use source vessels (default is to use target vessels)
     */
    public static class VesselsForEventTraverserCriteria extends TransferTraverserCriteria {

        // Traversal logic flags
        private boolean stopTraverseAtFirstFind = false;
        private boolean useEventTarget = true;
        private Set<LabEventType> labEventTypes = new HashSet<>();

        // Traversal data accumulator
        private MultiValuedMap<LabVessel, VesselPosition> positions = new HashSetValuedHashMap<>();

        // Traversal state control
        private int previousHopCount = -1;
        private boolean stopTraversingBeforeNextHop = false;

        /**
         * Searches for descendant events of specific type(s).
         * @param labEventTypes List of event types to locate.
         *                      <strong>Note: This criteria does not handle in-place events</strong>
         */
        public VesselsForEventTraverserCriteria(List<LabEventType> labEventTypes ) {
            this.labEventTypes.addAll(labEventTypes);
        }

        /**
         * Searches for descendant events and allow for traversal to be stopped at the first child vessel.
         * Saves the traversal overhead when it's known that there will be no other rework events (e.g. sample import)
         * @param labEventTypes List of event types to locate.
         *                      <strong>Note: This criteria does not handle in-place events</strong>
         * @param stopTraverseAtFirstFind Stop traversing at first matching event type (defaults to false)
         */
        public VesselsForEventTraverserCriteria(List<LabEventType> labEventTypes, boolean stopTraverseAtFirstFind ) {
            this(labEventTypes);
            this.stopTraverseAtFirstFind = stopTraverseAtFirstFind;
        }

        /**
         * Searches for descendant events and allow for traversal to be stopped at the first child vessel.
         * Saves the traversal overhead when it's known that there will be no other rework events (e.g. sample import)
         *
         * @param labEventTypes List of event types to locate.
         *                      <strong>Note: This criteria does not handle in-place events</strong>
         * @param stopTraverseAtFirstFind Stop traversing at first matching event type (defaults to false)
         * @param useEventTarget Use to switch from default of true to false (use event source)
         */
        public VesselsForEventTraverserCriteria(List<LabEventType> labEventTypes, boolean stopTraverseAtFirstFind,
                                                boolean useEventTarget) {
            this(labEventTypes, stopTraverseAtFirstFind);
            this.useEventTarget = useEventTarget;
        }

        /**
         * Obtains the outcome of the traversal
         * @return A set of barcode-position pairs.
         * Note:  If the vessel in the event of interest is not in a container, the position value will be null.
         */
        public MultiValuedMap<LabVessel, VesselPosition> getPositions(){
            return positions;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(
                Context context ) {

            // State variable to handle configuration option to stop on first hit
            TraversalControl outcome = TraversalControl.ContinueTraversing;

            // There is no event at traversal starting vessel (hopcount = 0)
            if ( context.getHopCount() > 0 ) {

                // The stop has to happen after we've gathered all the vessels from a pooling type event
                if (context.getHopCount() > previousHopCount && stopTraversingBeforeNextHop) {
                    return TraversalControl.StopTraversing;
                } else {
                    previousHopCount = context.getHopCount();
                }

                LabVessel.VesselEvent eventNode = context.getVesselEvent();
                boolean catchThisVessel = false;

                if (labEventTypes.contains(eventNode.getLabEvent().getLabEventType())) {
                    Map.Entry<LabVessel,VesselPosition> vesselPositionEntry = getTraversalVessel(context);
                    positions.put(vesselPositionEntry.getKey(), vesselPositionEntry.getValue() );
                    catchThisVessel = true;
                } else {
                    // Try in-place events
                    Map.Entry<LabVessel,VesselPosition> vesselPositionEntry = getTraversalVessel(context);

                    for (LabEvent inPlaceEvent : vesselPositionEntry.getKey().getInPlaceLabEvents()) {
                        if (labEventTypes.contains(inPlaceEvent.getLabEventType())) {
                            positions.put(vesselPositionEntry.getKey(), vesselPositionEntry.getValue());
                            catchThisVessel = true;
                            break;
                        }
                    }
                }

                if (catchThisVessel) {
                    stopTraversingBeforeNextHop = stopTraverseAtFirstFind;
                }
            }

            return outcome;
        }

        /**
         * Gets vessel and position from the context, either target or source as configured in instance
         * @param context
         * @return Lab vessel and (optional) position of vessel
         */
        private Map.Entry<LabVessel,VesselPosition> getTraversalVessel(Context context) {

            LabVessel.VesselEvent contextVesselEvent = context.getVesselEvent();

            LabVessel eventVessel;
            VesselPosition position;
            if( useEventTarget ) {
                position = contextVesselEvent.getTargetPosition();
                if (contextVesselEvent.getTargetLabVessel() != null) {
                    eventVessel = contextVesselEvent.getTargetLabVessel();
                } else {
                    eventVessel = contextVesselEvent.getTargetVesselContainer().getEmbedder();
                }
            } else {
                position = contextVesselEvent.getSourcePosition();
                if (contextVesselEvent.getSourceLabVessel() != null) {
                    eventVessel = contextVesselEvent.getSourceLabVessel();
                } else {
                    eventVessel = contextVesselEvent.getSourceVesselContainer().getEmbedder();
                }
            }
            return new AbstractMap.SimpleEntry(eventVessel, position);
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }
    }

    /**
     * Searches for lab vessel ancestors and/or descendants and accumulates batches <br />
     * Note:  Looking into ancestry, if a vessel associated with an FCT batch dilution vessel is hit,
     *  any other FCT batches associated with ancestor denatured vessel are ignored.
     * This prevents collecting FCT batches (multiple) associated with denatured tubes
     * and only gets single FCT batches associated with the flowcell, strip tube, or dilution vessel.
     *
     */
    public static class VesselBatchTraverserCriteria extends TransferTraverserCriteria {

        public VesselBatchTraverserCriteria( ) { }

        private Set<LabBatch> labBatches = new HashSet<>();
        private LabVessel startingVessel = null;
        private boolean stopCollectingFctBatches = false;

        public Set<LabBatch> getLabBatches(){
            return labBatches;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(
                Context context ) {


            // Ignore descendant batches for the starting vessel (context.getHopCount() == 0)
            if ( context.getHopCount() == 0 ) {
                if( context.getContextVessel() != null ) {
                    startingVessel = context.getContextVessel();
                    // Examine ancestor vessel batches
                    if( context.getTraversalDirection() == TraversalDirection.Ancestors ) {
                        getVesselBatches(startingVessel, context);
                    }
                }
                if( startingVessel == null ) {
                    if( context.getTraversalDirection() == TraversalDirection.Ancestors ) {
                        getVesselBatches(startingVessel, context);
                        for( LabVessel containee : (Set<LabVessel>)context.getContextVesselContainer().getContainedVessels()) {
                            getVesselBatches(containee, context);
                        }
                    }
                }
            } else {

                LabVessel.VesselEvent contextVesselEvent = context.getVesselEvent();
                LabVessel labVessel = null;

                // Defend against no contextVesselEvent?  Should not happen if not at starting vessel
                if (contextVesselEvent == null) {
                    return TraversalControl.ContinueTraversing;
                }

                LabEvent contextEvent = contextVesselEvent.getLabEvent();
                boolean useEventTarget = contextEvent.getLabEventType().getPlasticToValidate() == LabEventType.PlasticToValidate.TARGET;

                if (useEventTarget) {
                    if (contextVesselEvent.getTargetLabVessel() != null) {
                        labVessel = contextVesselEvent.getTargetLabVessel();
                    } else {
                        labVessel = contextVesselEvent.getTargetVesselContainer().getEmbedder();
                    }
                }
                if( !useEventTarget || labVessel == null ) {
                    if (contextVesselEvent.getSourceLabVessel() != null) {
                        labVessel = contextVesselEvent.getSourceLabVessel();
                    } else {
                        labVessel = contextVesselEvent.getSourceVesselContainer().getEmbedder();
                    }
                }

                if( context.getTraversalDirection() == TraversalDirection.Ancestors || !labVessel.equals(startingVessel)) {
                    getVesselBatches(labVessel, context);
                }

            }

            return TraversalControl.ContinueTraversing;
        }

        private void getVesselBatches( LabVessel labVessel, Context context ) {

            if( labVessel == null ) {
                return;
            }

            // If vessel has LabBatchStartingVessel, don't bother with bucket entries
            boolean hadStartingVessels = false;

            if( labVessel.getDilutionReferences().isEmpty() ) {
                for (LabBatchStartingVessel labBatchStartingVessel : labVessel.getLabBatchStartingVessels()) {
                    LabBatch labBatch = labBatchStartingVessel.getLabBatch();
                    if (context.getTraversalDirection() == TraversalDirection.Descendants || labBatch.getLabBatchType() != LabBatch.LabBatchType.FCT || !stopCollectingFctBatches){
                        labBatches.add(labBatch);
                    }
                    hadStartingVessels = true;
                }
            } else {
                for (LabBatchStartingVessel labBatchStartingVessel : labVessel.getDilutionReferences()) {
                    LabBatch labBatch = labBatchStartingVessel.getLabBatch();
                    labBatches.add(labBatch);
                    hadStartingVessels = true;
                    stopCollectingFctBatches = true;
                }
            }

            if( !hadStartingVessels ) {
                for (BucketEntry bucketEntry : labVessel.getBucketEntries()) {
                    labBatches.add(bucketEntry.getLabBatch());
                }
            }
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }
    }


    private class MaterialTypeEventCriteria extends TransferTraverserCriteria {
        private MaterialType materialType;
        private Set<LabVessel> labVessels = new LinkedHashSet<>();

        private MaterialTypeEventCriteria(MaterialType materialType) {
            this.materialType = materialType;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            LabVessel.VesselEvent vesselEvent = context.getVesselEvent();
            // Starting vessel has no event
            if (vesselEvent != null) {
                MaterialType resultingMaterialType = 
                        vesselEvent.getLabEvent().getLabEventType().getResultingMaterialType();
                if (resultingMaterialType != null && resultingMaterialType == materialType) {
                    labVessels.add(vesselEvent.getTargetLabVessel());
                }
            }

            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        public List<LabVessel> getLabVessels() {
            return new ArrayList<>(labVessels);
        }
    }
}
