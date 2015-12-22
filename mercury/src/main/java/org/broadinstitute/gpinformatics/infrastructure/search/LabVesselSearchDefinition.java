package org.broadinstitute.gpinformatics.infrastructure.search;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.columns.BspSampleSearchAddRowsListener;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.LabVesselLatestEventPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.LabVesselMetadataPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.LabVesselMetricPlugin;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds ConfigurableSearchDefinition for lab vessel user defined search logic
 */
public class LabVesselSearchDefinition {
    public LabVesselSearchDefinition(){}

    public ConfigurableSearchDefinition buildSearchDefinition(){

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
                ColumnEntity.LAB_VESSEL, 100, criteriaProjections, mapGroupSearchTerms);

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

        // todo jmt look at search inputs, to show only LCSET that was searched on?
        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("LCSET");
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        // Non-reworks
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("labBatches", "labBatch"));
        criteriaPath.setPropertyName("batchName");
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
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                Set<String> results = new HashSet<>();
                LabVessel labVessel = (LabVessel) entity;

                // Navigate back to sample(s)
                for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
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
        searchTerm.setName("Barcode");
        searchTerm.setIsDefaultResultColumn(Boolean.TRUE);
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
                Set<String> results = new HashSet<>();
                LabVessel labVessel = (LabVessel) entity;
                for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                    for (ProductOrderSample productOrderSample : sampleInstanceV2.getAllProductOrderSamples() ) {
                        results.add(productOrderSample.getProductOrder().getJiraTicketKey());
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

                VesselDescendantTraverserCriteria eval = new VesselDescendantTraverserCriteria(
                        Collections.singletonList(LabEventType.SAMPLE_IMPORT), true );
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

                if (!eval.getPositions().isEmpty()) {
                    barcodes = new HashSet<>();
                    for (Pair<String, VesselPosition> positionPair : eval.getPositions()) {
                        barcodes.add(positionPair.getLeft());
                    }
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

                VesselDescendantTraverserCriteria eval
                        = new VesselDescendantTraverserCriteria(Collections.singletonList(LabEventType.SAMPLE_IMPORT), true );
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

                if (!eval.getPositions().isEmpty()) {
                    positions = new HashSet<>();
                    for( Pair<String,VesselPosition> positionPair : eval.getPositions() ) {
                        if( positionPair.getRight() != null ) {
                            positions.add(positionPair.getRight().toString());
                        }
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

                VesselDescendantTraverserCriteria eval
                        = new VesselDescendantTraverserCriteria(Collections.singletonList(LabEventType.POND_REGISTRATION) );
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

                if (!eval.getPositions().isEmpty()) {
                    positions = new HashSet<>();
                    for( Pair<String,VesselPosition> positionPair : eval.getPositions() ) {
                        if( positionPair.getRight() != null ) {
                            positions.add(positionPair.getRight().toString());
                        }
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

                VesselDescendantTraverserCriteria eval = new VesselDescendantTraverserCriteria(
                        Collections.singletonList(LabEventType.POND_REGISTRATION) );
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

                if (!eval.getPositions().isEmpty()) {
                    barcodes = new HashSet<>();
                    for (Pair<String, VesselPosition> positionPair : eval.getPositions()) {
                        barcodes.add(positionPair.getLeft());
                    }
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

                VesselDescendantTraverserCriteria eval = new VesselDescendantTraverserCriteria(
                        Collections.singletonList(LabEventType.SHEARING_TRANSFER), false, false);
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

                if (!eval.getPositions().isEmpty()) {
                    positions = new HashSet<>();
                    for (Pair<String, VesselPosition> positionPair : eval.getPositions()) {
                        if (positionPair.getRight() != null) {
                            positions.add(positionPair.getRight().toString());
                        }
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

                VesselDescendantTraverserCriteria eval = new VesselDescendantTraverserCriteria(
                        Collections.singletonList(LabEventType.SHEARING_TRANSFER), false, false);
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

                if (!eval.getPositions().isEmpty()) {
                    barcodes = new HashSet<>();
                    for (Pair<String, VesselPosition> positionPair : eval.getPositions()) {
                        barcodes.add(positionPair.getLeft());
                    }
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

                VesselDescendantTraverserCriteria eval = new VesselDescendantTraverserCriteria(labEventTypes );
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

                if (!eval.getPositions().isEmpty()) {
                    positions = new HashSet<>();
                    for (Pair<String, VesselPosition> positionPair : eval.getPositions()) {
                        if (positionPair.getRight() != null) {
                            positions.add(positionPair.getRight().toString());
                        }
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

                VesselDescendantTraverserCriteria eval
                        = new VesselDescendantTraverserCriteria(labEventTypes );
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

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

                VesselDescendantTraverserCriteria eval
                        = new VesselDescendantTraverserCriteria(labEventTypes );
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

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

    /**
     * Build sample metadata search terms for lab vessels.
     * @return List of search terms/column definitions for lab vessel sample metadata
     */
    private List<SearchTerm> buildLabVesselMetadata() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Mercury Sample ID");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                List<String> values = new ArrayList<String>();
                LabVessel labVessel = (LabVessel) entity;
                for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                    values.add(sampleInstanceV2.getNearestMercurySampleName());
                }
                return values;
            }
        });
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("mercurySample", "mercurySamples"));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPath.setJoinFetch(Boolean.TRUE);
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
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
                Set<LabMetric> labMetrics = labVessel.getMetrics();
                for (LabMetric labMetric : labMetrics) {
                    if (labMetric.getName() == LabMetric.MetricType.INITIAL_PICO) {
                        return labMetric.getTotalNg();
                    }
                }
                return null;
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
     * Searches for lab vessel descendant events of specific type(s)
     * Records barcode and position of all descendant event vessel(s)
     * Optional flags:
     *   Stop at first descendant event found (default is to continue on to all descendant events)
     *   Use source vessels (default is to use target vessels)
     */
    public static class VesselDescendantTraverserCriteria extends TransferTraverserCriteria {

        // Optional flags
        private boolean stopTraverseAtFirstFind = false;
        private boolean useEventTarget = true;

        private List<LabEventType> labEventTypes;

        private Set<Pair<String, VesselPosition>> positions = new HashSet<>();

        /**
         * Searches for descendant events of specific type(s).
         * @param labEventTypes List of event types to locate.
         *                      <strong>Note: This criteria does not handle in-place events</strong>
         */
        public VesselDescendantTraverserCriteria( List<LabEventType> labEventTypes ) {
            this.labEventTypes = labEventTypes;
        }

        /**
         * Searches for descendant events and allow for traversal to be stopped at the first child vessel.
         * Saves the traversal overhead when it's known that there will be no other rework events (e.g. sample import)
         * @param labEventTypes List of event types to locate.
         *                      <strong>Note: This criteria does not handle in-place events</strong>
         * @param stopTraverseAtFirstFind Stop traversing at first matching event type (defaults to false)
         */
        public VesselDescendantTraverserCriteria( List<LabEventType> labEventTypes, boolean stopTraverseAtFirstFind ) {
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
        public VesselDescendantTraverserCriteria( List<LabEventType> labEventTypes, boolean stopTraverseAtFirstFind,
                                                  boolean useEventTarget) {
            this(labEventTypes, stopTraverseAtFirstFind);
            this.useEventTarget = useEventTarget;
        }

        /**
         * Obtains the outcome of the traversal
         * @return A set of barcode-position pairs.
         * Note:  If the vessel in the event of interest is not in a container, the position value will be null.
         */
        public Set<Pair<String, VesselPosition>> getPositions(){
            return positions;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(
                Context context ) {

            // This handles descendant traversals only!
            if( context.getTraversalDirection() != TraversalDirection.Descendants ) {
                throw new IllegalStateException( "VesselDescendantTraverserCriteria handles descendant traversal only.");
            }

            // State variable to handle configuration option to stop on first hit
            TraversalControl outcome = TraversalControl.ContinueTraversing;

            // There is no event at traversal starting vessel
            if ( context.getHopCount() == 0 ) {
                // We may be starting on a vessel,
                LabVessel labVessel = context.getContextVessel();
                // or a container
                if(labVessel == null ) {
                    labVessel = context.getContextVesselContainer().getEmbedder();
                }

                // Examine transfers to (handles the case where the event of interest is where the current vessel is at)
                if( labVessel != null ) {
                    for(LabEvent labEvent : labVessel.getTransfersTo() ) {
                        if( labEventTypes.contains( labEvent.getLabEventType() ) ) {
                            // In-place has no vessel position
                            positions.add(Pair.of(labVessel.getLabel(), (VesselPosition)null));
                            if( stopTraverseAtFirstFind ) {
                                outcome = TraversalControl.StopTraversing;
                                break;
                            }
                        }
                    }

                    if( outcome != TraversalControl.StopTraversing ) {
                        boolean foundOne = examineInPlaceEvents( context );
                        if( foundOne && stopTraverseAtFirstFind ) {
                            outcome = TraversalControl.StopTraversing;
                        }
                    }
                }
            } else {
                // We're on a traversal node
                boolean foundOne = examineTraversalVessel(context);
                if( foundOne && stopTraverseAtFirstFind ) {
                    outcome = TraversalControl.StopTraversing;
                }
            }

            return outcome;
        }

        private boolean examineTraversalVessel(Context context) {

            boolean foundOne = false;

            LabVessel.VesselEvent contextVesselEvent = context.getVesselEvent();

            // Defend against no contextVesselEvent?  Should not happen if not at starting vessel
            if ( contextVesselEvent == null ) {
                return foundOne;
            }

            LabEvent contextEvent = contextVesselEvent.getLabEvent();

            if( labEventTypes.contains( contextEvent.getLabEventType() ) ) {
                foundOne = true;
                String barcode;
                VesselPosition position;
                // Searching descendants uses default of target container
                if( useEventTarget ) {
                    position = contextVesselEvent.getTargetPosition();
                    if (contextVesselEvent.getTargetLabVessel() != null) {
                        barcode = contextVesselEvent.getTargetLabVessel().getLabel();
                    } else {
                        barcode = contextVesselEvent.getTargetVesselContainer().getEmbedder().getLabel();
                    }
                    positions.add(Pair.of(barcode, position));
                } else {
                    position = contextVesselEvent.getSourcePosition();
                    if (contextVesselEvent.getSourceLabVessel() != null) {
                        barcode = contextVesselEvent.getSourceLabVessel().getLabel();
                    } else {
                        barcode = contextVesselEvent.getSourceVesselContainer().getEmbedder().getLabel();
                    }
                    positions.add(Pair.of(barcode, position));
                }
            }

            if( foundOne && stopTraverseAtFirstFind ) {
                // Stop if flagged
                return foundOne;
            } else {
                // Look for in-place events
                return foundOne || examineInPlaceEvents(context);
            }
        }

        private boolean examineInPlaceEvents( Context context ) {
            boolean foundOne = false;

            // In place event may be on a vessel,
            LabVessel labVessel = context.getContextVessel();
            // or a container
            if(labVessel == null ) {
                labVessel = context.getContextVesselContainer().getEmbedder();
            }

            if( labVessel != null ) {
                for( LabEvent inPlaceEvent : labVessel.getInPlaceLabEvents() ) {
                    if( labEventTypes.contains(inPlaceEvent.getLabEventType())) {
                        // In-place has no vessel position
                        positions.add(Pair.of(labVessel.getLabel(), (VesselPosition)null));
                        foundOne = true;
                        if( stopTraverseAtFirstFind ) {
                            return foundOne;
                        }
                    }
                }
            }
            return foundOne;
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
