package org.broadinstitute.gpinformatics.infrastructure.search;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.columns.BspSampleSearchAddRowsListener;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.LabVesselLatestEventPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.LabVesselMetadataPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.LabVesselMetricPlugin;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

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
                        listeners.put(SearchInstance.CONTEXT_KEY_BSP_SAMPLE_SEARCH, new BspSampleSearchAddRowsListener());
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
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("bucketEntries", "labBatch"));
        criteriaPath.setPropertyName("batchName");
        criteriaPath.setJoinFetch(Boolean.TRUE);
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                Set<String> results = new HashSet<>();
                LabVessel labVessel = (LabVessel) entity;
                for (BucketEntry bucketEntry : labVessel.getBucketEntries()) {
                    if (bucketEntry.getLabBatch() != null) {
                        results.add(bucketEntry.getLabBatch().getBatchName());
                    }
                }
                if( results.isEmpty() ) {
                    // Try navigating back to sample instance batch
                    for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                        if( sampleInstanceV2.getSingleBatch() != null ) {
                            results.add( sampleInstanceV2.getSingleBatch().getBatchName());
                        }
                    }
                }
                if( results.isEmpty() ) {
                    // Try navigating back to sample bucket entries
                    for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                        for (BucketEntry bucketEntry : sampleInstanceV2.getAllBucketEntries()) {
                            LabBatch batch = bucketEntry.getLabBatch();
                            if( batch != null ) {
                                results.add( bucketEntry.getLabBatch().getBatchName());
                            }
                        }
                    }
                }
                return results;
            }
        });
        searchTerm.setValueConversionExpression(SearchDefinitionFactory.getLcsetInputConverter());
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Barcode");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("label");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabVessel labVessel = (LabVessel) entity;
                return labVessel.getLabel();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Vessel Type");
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabVessel labVessel = (LabVessel) entity;
                return SearchDefinitionFactory.findVesselType(labVessel);
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Vessel Volume");
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabVessel labVessel = (LabVessel) entity;
                BigDecimal volume = labVessel.getVolume();
                if( volume != null ) {
                    return MathUtils.scaleTwoDecimalPlaces(volume).toPlainString();
                } else {
                    return "";
                }
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Vessel Concentration");
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabVessel labVessel = (LabVessel) entity;
                BigDecimal conc = labVessel.getConcentration();
                if( conc != null ) {
                    return MathUtils.scaleTwoDecimalPlaces(conc).toPlainString();
                } else {
                    return "";
                }
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
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabVessel labVessel = (LabVessel) entity;
                Collection<MercurySample> mercurySamples = labVessel.getMercurySamples();
                if( !mercurySamples.isEmpty() ) {
                    MercurySample mercurySample = mercurySamples.iterator().next();
                    BspSampleSearchAddRowsListener bspColumns = (BspSampleSearchAddRowsListener) context.get(
                            SearchInstance.CONTEXT_KEY_BSP_SAMPLE_SEARCH);
                    return bspColumns.getColumn(mercurySample.getSampleKey(), bspSampleSearchColumn);
                } else {
                    return "";
                }
            }
        });
        searchTerm.setAddRowsListenerHelper(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
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
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
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
        searchTerm.setValueConversionExpression(SearchDefinitionFactory.getPdoInputConverter());
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        //new CriteriaProjection("bucketEntries", "labVesselId", "labVessel", BucketEntry.class));
        criteriaPath.setCriteria(Arrays.asList("bucketEntries", "productOrder"));
        criteriaPath.setPropertyName("jiraTicketKey");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                List<String> results = new ArrayList<>();
                LabVessel labVessel = (LabVessel) entity;
                for (BucketEntry bucketEntry : labVessel.getBucketEntries()) {
                    results.add(bucketEntry.getProductOrder().getJiraTicketKey());
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
        searchTerm.setValueConversionExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                return Integer.valueOf( (String) context.get(SearchInstance.CONTEXT_KEY_SEARCH_STRING));
            }
        } );
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
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
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "inPlaceLabVesselId" /* LabEvent */ ));
        criteriaPath.setPropertyName("labEventType");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                List<String> labEventNames = new ArrayList<>();
                LabVessel labVessel = (LabVessel) entity;
                Set<LabEvent> labVesselInPlaceLabEvents = labVessel.getInPlaceLabEvents();
                for (LabEvent labVesselInPlaceLabEvent : labVesselInPlaceLabEvents) {
                    labEventNames.add(labVesselInPlaceLabEvent.getLabEventType().getName());
                }
                return labEventNames;
            }
        });
        searchTerm.setValuesExpression( new SearchDefinitionFactory.EventTypeValuesExpression() );
        searchTerm.setValueConversionExpression( new SearchDefinitionFactory.EventTypeValueConversionExpression() );
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Imported Sample Tube Barcode");
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                return getEventLabel((LabVessel) entity
                        , Collections.singletonList(LabEventType.SAMPLE_IMPORT), true);
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Imported Sample ID");
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                List<String> results = new ArrayList<>();
                LabVessel labVessel = (LabVessel) entity;
                Map<LabEvent, Set<LabVessel>> mapEventToVessels = labVessel.findVesselsForLabEventType(
                        LabEventType.SAMPLE_IMPORT, true);
                for (Map.Entry<LabEvent, Set<LabVessel>> eventVesselEntry : mapEventToVessels.entrySet()) {
                    for (LabVessel vessel : eventVesselEntry.getValue()) {
                        Collection<MercurySample> mercurySamples = vessel.getMercurySamples();
                        if( !mercurySamples.isEmpty() ) {
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
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            //            @Override
//            public Object evaluate(Object entity, Map<String, Object> context) {
//                // Uses InitialTare to return position in rack (CollaboratorTransfer event has no position)
//                return getEventPosition((LabVessel) entity
//                        , Collections.singletonList(LabEventType.SAMPLE_IMPORT), false);
//            }
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabVessel ves = (LabVessel) entity;
                VesselDescendantTraverserCriteria eval
                        = new VesselDescendantTraverserCriteria(Collections.singletonList(LabEventType.SAMPLE_IMPORT));
                ves.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);
                if (!eval.getPosition().isEmpty()) {
                    return eval.getPosition();
                } else {
                    // In place vessel is a container where event has no transfers
                    LabVessel inPlaceVessel = null;
                    LabEvent labEvent = null;
                    if (eval.getEvent() != null) {
                        labEvent = eval.getEvent();
                        inPlaceVessel = labEvent.getInPlaceLabVessel();
                    }
                    if (inPlaceVessel != null) {
                        VesselContainer vesselContainer = inPlaceVessel.getContainerRole();
                        if (vesselContainer == null) {
                            // SAMPLE_IMPORT event is in-place on tube but no transfers so can't get the tube formation
                            for (VesselContainer container : inPlaceVessel.getContainers()) {
                                if (container.getEmbedder() instanceof TubeFormation) {
                                    for (LabEvent inPlaceEvent : container.getEmbedder().getInPlaceLabEvents()) {
                                        if (inPlaceEvent.getLabEventType() == labEvent.getLabEventType()) {
                                            return container.getPositionOfVessel(ves);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return null;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Pond Sample Position");
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
//            @Override
//            public Object evaluate(Object entity, Map<String, Object> context) {
//                return getEventPosition((LabVessel) entity
//                        , Collections.singletonList(LabEventType.POND_REGISTRATION), true);
//            }
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabVessel ves = (LabVessel) entity;
                VesselDescendantTraverserCriteria eval
                        = new VesselDescendantTraverserCriteria(Collections.singletonList(LabEventType.POND_REGISTRATION));
                ves.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants );
                return eval.getPosition();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Pond Tube Barcode");
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                return getEventLabel((LabVessel) entity
                        , Collections.singletonList(LabEventType.POND_REGISTRATION), true);
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Shearing Sample Position");
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
           @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabVessel ves = (LabVessel) entity;
                VesselDescendantTraverserCriteria eval
                        = new VesselDescendantTraverserCriteria(Collections.singletonList(LabEventType.SHEARING_TRANSFER));
                ves.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants );
                return eval.getPosition();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Shearing Sample Barcode");
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                return getEventLabel((LabVessel) entity
                        , Collections.singletonList(LabEventType.SHEARING_TRANSFER), false);
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Catch Sample Position");
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
//            @Override
//            public Object evaluate(Object entity, Map<String, Object> context) {
//                List<LabEventType> labEventTypes = new ArrayList<>();
//                // ICE
//                labEventTypes.add(LabEventType.ICE_CATCH_ENRICHMENT_CLEANUP);
//                // Agilent
//                labEventTypes.add(LabEventType.NORMALIZED_CATCH_REGISTRATION);
//
//                return getEventPosition((LabVessel) entity, labEventTypes, true);
//            }
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabVessel ves = (LabVessel) entity;
                List<LabEventType> labEventTypes = new ArrayList<>();
                // ICE
                labEventTypes.add(LabEventType.ICE_CATCH_ENRICHMENT_CLEANUP);
                // Agilent
                labEventTypes.add(LabEventType.NORMALIZED_CATCH_REGISTRATION);
                VesselDescendantTraverserCriteria eval
                        = new VesselDescendantTraverserCriteria(labEventTypes);
                ves.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants );
                return eval.getPosition();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Catch Tube Barcode");
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                List<LabEventType> labEventTypes = new ArrayList<>();
                // ICE
                labEventTypes.add(LabEventType.ICE_CATCH_ENRICHMENT_CLEANUP);
                // Agilent
                labEventTypes.add(LabEventType.NORMALIZED_CATCH_REGISTRATION);

                return getEventLabel((LabVessel) entity, labEventTypes, true );
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
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                List<String> values = new ArrayList<String>();
                LabVessel labVessel = (LabVessel) entity;
                for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                    values.add(sampleInstanceV2.getMercuryRootSampleName());
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
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
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
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                String value = "";
                LabVessel labVessel = (LabVessel) entity;
                Set<LabMetric> labMetrics = labVessel.getMetrics();
                for (LabMetric labMetric : labMetrics) {
                    if (labMetric.getName() == LabMetric.MetricType.INITIAL_PICO) {
                        BigDecimal ng = labMetric.getTotalNg();
                        if (ng != null) {
                            value = MathUtils.scaleTwoDecimalPlaces(ng).toPlainString();
                        }
                        break;
                    }
                }
                return value;

            }
        });
        searchTerm.setValueConversionExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                return new BigDecimal((String) context.get(SearchInstance.CONTEXT_KEY_SEARCH_STRING));
            }
        });
        searchTerms.add(searchTerm);

        // ***** Build sample metadata child search term (the metadata value) ***** //
        List<SearchTerm> childSearchTerms = new ArrayList<>();
        searchTerm = new SearchTerm();
        searchTerm.setName("Metadata Value");
        // This is needed to show the selected metadata column term in the results.
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                String values = "";

                SearchInstance.SearchValue searchValue =
                        (SearchInstance.SearchValue) context.get(SearchInstance.CONTEXT_KEY_SEARCH_VALUE);
                String header = searchValue.getParent().getValues().get(0);
                Metadata.Key key = Metadata.Key.valueOf(header);

                LabVessel labVessel = (LabVessel) entity;
                Collection<MercurySample> samples = labVessel.getMercurySamples();
                for (MercurySample sample : samples) {
                    Set<Metadata> metadataSet = sample.getMetadata();
                    for (Metadata meta : metadataSet) {
                        if (meta.getKey() == key) {
                            values += meta.getValue();
                            break;
                        }
                    }
                }
                return values;
            }
        });
        searchTerm.setViewHeader(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                String header;
                SearchInstance.SearchValue searchValue =
                        (SearchInstance.SearchValue) context.get(SearchInstance.CONTEXT_KEY_SEARCH_VALUE);
                header = searchValue.getParent().getValues().get(0);
                Metadata.Key key = Metadata.Key.valueOf(header);
                if (key != null) {
                    return key.getDisplayName();
                } else {
                    return header;
                }
            }
        });
        searchTerm.setTypeExpression(new SearchTerm.Evaluator<String>() {
            @Override
            public String evaluate(Object entity, Map<String, Object> context) {
                SearchInstance.SearchValue searchValue =
                        (SearchInstance.SearchValue) context.get(SearchInstance.CONTEXT_KEY_SEARCH_VALUE);
                String metaName = searchValue.getParent().getValues().get(0);
                Metadata.Key key = Metadata.Key.valueOf(metaName);
                switch (key.getDataType()) {
                case STRING:
                    return String.class.getSimpleName();
                case NUMBER:
                    return BigDecimal.class.getSimpleName();
                case DATE:
                    return Date.class.getSimpleName();
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
            public String evaluate(Object entity, Map<String, Object> context) {
                SearchInstance.SearchValue searchValue =
                        (SearchInstance.SearchValue) context.get(SearchInstance.CONTEXT_KEY_SEARCH_VALUE);
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
        searchTerm.setValuesExpression(new SearchTerm.Evaluator<List<ConstrainedValue>>() {
            @Override
            public List<ConstrainedValue> evaluate(Object entity, Map<String, Object> context) {
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
        searchTerm.setValueConversionExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                return Enum.valueOf(Metadata.Key.class, (String) context.get(SearchInstance.CONTEXT_KEY_SEARCH_STRING));
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
                searchTerm.setDisplayExpression(new SearchDefinitionFactory.SampleMetadataDisplayExpression());
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
     * Traverse lab vessel events looking for specific types and extract the vessel barcode
     * @param labVessel
     * @param labEventTypes Mutually exclusive event types
     * @param useTargetContainer Should target container (vs. source container) be used for positions?
     * @return
     */
    private List<String> getEventLabel(LabVessel labVessel, List<LabEventType> labEventTypes, boolean useTargetContainer) {
        List<String> results = new ArrayList<>();
        for( Pair<VesselPosition,String> pair : getSamplePositionForEvent(labVessel, labEventTypes, useTargetContainer) ) {
            results.add(pair.getRight());
        }
        return results;
    }

    private VesselContainer getEventVesselContainer( LabEvent labEvent, boolean useTargetContainer ) {
        VesselContainer vesselContainer = null;

        // In place vessel is a container where event has no transfers
        LabVessel inPlaceVessel = labEvent.getInPlaceLabVessel();
        if( inPlaceVessel != null ) {
            vesselContainer = inPlaceVessel.getContainerRole();
            if( vesselContainer == null ) {
                // SAMPLE_IMPORT event is in-place on tube but no transfers so can't get the tube formation
                for( VesselContainer container : inPlaceVessel.getContainers() ) {
                    if( container.getEmbedder() instanceof TubeFormation ) {
                        for( LabEvent inPlaceEvent : container.getEmbedder().getInPlaceLabEvents() ) {
                            if( inPlaceEvent.getLabEventType() == labEvent.getLabEventType() ){
                                return container;
                            }
                        }
                    }
                }
            }
        }

        // Transfer
        if( vesselContainer == null ){
            if( useTargetContainer ) {
                for (LabVessel srcVessel : labEvent.getTargetLabVessels()) {
                    vesselContainer = srcVessel.getContainerRole();
                    if( vesselContainer != null ) {
                        break;
                    }
                }
            } else {
                for (LabVessel srcVessel : labEvent.getSourceLabVessels()) {
                    vesselContainer = srcVessel.getContainerRole();
                    if( vesselContainer != null ) {
                        break;
                    }
                }
            }
        }
        return vesselContainer;
    }

    /**
     * Shared logic to traverse lab vessel events looking for specific types and extract
     *  the vessel position and barcode for a given sample
     * @param labVessel The sample tube
     * @param labEventTypes Mutually exclusive event types
     * @param useTargetContainer Should target container (vs. source container) be used for positions?
     * @return
     */
    private List<Pair<VesselPosition,String>> getSamplePositionForEvent(LabVessel labVessel,
                                                                        List<LabEventType> labEventTypes,
                                                                        boolean useTargetContainer) {

        List<Pair<VesselPosition,String>> results = new ArrayList<>();

        // Look for in-place event low hanging fruit
        Set<LabEvent> inPlaceEvents = labVessel.getInPlaceEventsWithContainers();
        for( LabEvent event : inPlaceEvents ) {
            if( labEventTypes.contains(event.getLabEventType()) ){
                VesselContainer container = event.getInPlaceLabVessel().getContainerRole();
                VesselPosition position = container.getPositionOfVessel(labVessel);
                if( position != null ) {
                    results.add( Pair.of(position, labVessel.getLabel() ) );
                }
            }
        }

        if( results.size() > 0 ) {
            return results;
        }

        // Dig through event descendants for specific event types
        Map<LabEvent, Set<LabVessel>> mapEventToVessels
                = labVessel.findDescendantVesselsForLabEventTypes(labEventTypes, useTargetContainer);

        if( mapEventToVessels.isEmpty() ) {
            return results;
        }

        for( Map.Entry<LabEvent, Set<LabVessel>> eventMapEntry : mapEventToVessels.entrySet() ){
            boolean foundPositionForEvent = false;
            LabEvent labEvent = eventMapEntry.getKey();
            Set<LabVessel> descendantLabVessels = eventMapEntry.getValue();

            VesselContainer vesselContainer = getEventVesselContainer( labEvent, useTargetContainer );

            if( vesselContainer == null || vesselContainer.getMapPositionToVessel().isEmpty() ) {
                continue;
            }

            VesselPosition position = vesselContainer.getPositionOfVessel(labVessel);
            if( position != null) {
                results.add( Pair.of(position, labVessel.getLabel() ) );
                foundPositionForEvent = true;
            }

            for (LabVessel descendantVessel : descendantLabVessels) {
                position = vesselContainer.getPositionOfVessel(descendantVessel);
                if( position != null) {
                    results.add( Pair.of(position, descendantVessel.getLabel()) );
                    foundPositionForEvent = true;
                }
            }

            if( !foundPositionForEvent ){
                // Dig through  samples to get a match
                boolean foundSample = false;
                for( LabVessel containedVessel : (Set<LabVessel>)vesselContainer.getContainedVessels() ) {
                    for (SampleInstanceV2 sampleInstanceV2 : containedVessel.getSampleInstancesV2()) {
                        MercurySample sample = sampleInstanceV2.getRootOrEarliestMercurySample();
                        if (sample != null) {
                            for( LabVessel sampleVessel : sample.getLabVessel() ) {
                                if( labVessel.getLabel().equals( sampleVessel.getLabel() ) ) {
                                    position = vesselContainer.getPositionOfVessel(containedVessel);
                                    if( position != null) {
                                        results.add( Pair.of(position, containedVessel.getLabel() ) );
                                        foundSample = true;
                                        // Stop parsing vessels for this sample
                                        break;
                                    }
                                }
                            }
                        }
                        if( foundSample ) {
                            // Stop parsing samples for this entire container and return results
                            return results;
                        }
                    }
                }
            }

        }

        return results;
    }

    /**
     * Traverse lab vessel events looking for specific types and extract the vessel position
     * @param labVessel
     * @param labEventTypes Mutually exclusive event types
     * @param useTargetContainer Should target container (vs. source container) be used for positions?
     * @return
     */
    private List<String> getEventPosition(LabVessel labVessel, List<LabEventType> labEventTypes, boolean useTargetContainer ) {
        List<String> results = new ArrayList<>();
        for( Pair<VesselPosition,String> pair : getSamplePositionForEvent(labVessel, labEventTypes, useTargetContainer) ) {
            results.add(pair.getLeft().toString());
        }
        return results;
    }

    private class VesselDescendantTraverserCriteria implements TransferTraverserCriteria {

        public VesselDescendantTraverserCriteria( List<LabEventType> labEventTypes ) {
            this.labEventTypes = labEventTypes;
        }

        private List<LabEventType> labEventTypes;

        private Set<String> positions = new HashSet<>();
        private Set<String> barcodes = new HashSet<>();
        private LabEvent labEvent;

        public Set<String> getPosition(){
            return positions;
        }

        public Set<String> getBarcodes(){
            return barcodes;
        }

        public LabEvent getEvent(){
            return labEvent;
        }

        @Override
        public TransferTraverserCriteria.TraversalControl evaluateVesselPreOrder(
                TransferTraverserCriteria.Context context ) {

            TransferTraverserCriteria.TraversalControl outcome
                    = TransferTraverserCriteria.TraversalControl.ContinueTraversing;

            if( context.getEvent() != null && labEventTypes.contains( context.getEvent().getLabEventType() ) ) {
                labEvent = context.getEvent();
                if( context.getLabVessel() != null ) {
                    barcodes.add( context.getLabVessel().getLabel() );
                }
                if( context.getVesselPosition() != null ) {
                    positions.add(context.getVesselPosition().toString());
                }
                //outcome = TransferTraverserCriteria.TraversalControl.StopTraversing;
            }

            return outcome;
        }

        @Override
        public void evaluateVesselInOrder(TransferTraverserCriteria.Context context) {
        }

        @Override
        public void evaluateVesselPostOrder(TransferTraverserCriteria.Context context) {
        }
    }


}
