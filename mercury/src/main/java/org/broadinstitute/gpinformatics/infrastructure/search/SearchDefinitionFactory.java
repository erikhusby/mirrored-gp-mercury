package org.broadinstitute.gpinformatics.infrastructure.search;

import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.columns.BspSampleSearchAddRowsListener;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.EventVesselSourcePositionPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.EventVesselTargetPositionPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.LabVesselLatestEventPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.LabVesselMetadataPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.LabVesselMetricPlugin;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configurable search definitions for various entities.
 */
@SuppressWarnings("FeatureEnvy")
public class SearchDefinitionFactory {

    // State of ConfigurableSearchDefinition does not change once created.
    private static Map<String, ConfigurableSearchDefinition> MAP_NAME_TO_DEF = new HashMap<>();

    public static final String CONTEXT_KEY_BSP_USER_LIST = "bspUserList";
    public static final String CONTEXT_KEY_COLUMN_SET_TYPE = "columnSetType";
    public static final String CONTEXT_KEY_SEARCH_VALUE = "searchValue";
    public static final String CONTEXT_KEY_SEARCH_TERM = "searchTerm";
    public static final String CONTEXT_KEY_SEARCH_STRING = "searchString";
    public static final String CONTEXT_KEY_BSP_SAMPLE_SEARCH = "BSPSampleSearchService";
    public static final String CONTEXT_KEY_OPTION_VALUE_DAO = "OptionValueDao";

    /**
     * Convenience to allow user to just enter the number of the LCSET.
     */
    private SearchTerm.Evaluator<Object> lcsetConverter = new SearchTerm.Evaluator<Object>() {
        @Override
        public Object evaluate(Object entity, Map<String, Object> context) {
            String value = (String) context.get(CONTEXT_KEY_SEARCH_STRING);
            if( value.matches("[0-9]*")){
                value = "LCSET-" + value;
            }
            return value;
        }
    };

    private SearchDefinitionFactory(){}

    static {
        SearchDefinitionFactory fact = new SearchDefinitionFactory();
        fact.buildLabEventSearchDef();
        fact.buildLabVesselSearchDef();
    }

    public static ConfigurableSearchDefinition getForEntity(String entity) {
        return MAP_NAME_TO_DEF.get(entity);
    }

    private void buildLabVesselSearchDef() {
        Map<String, List<SearchTerm>> mapGroupSearchTerms = new LinkedHashMap<>();

        List<SearchTerm> searchTerms = buildLabVesselIds();
        mapGroupSearchTerms.put("IDs", searchTerms);

        // Are there alternatives to search terms that aren't searchable?  Should they be in a different structure, then merged with search terms for display?

        // XX version - from workflow? 3.2 doesn't seem to be in XML
        // Start date - LabBatch.createdOn? usually 1 day before "scheduled to start"
        // Due date - LabBatch.dueDate is transient!
        searchTerms = buildLabVesselBsp();
        mapGroupSearchTerms.put("BSP", searchTerms);

        searchTerms = buildLabVesselMetadata();
        mapGroupSearchTerms.put("Mercury Metadata", searchTerms);

        searchTerms = buildLabVesselBuckets();
        mapGroupSearchTerms.put("Buckets", searchTerms);

        searchTerms = buildLabVesselEvent();
        mapGroupSearchTerms.put("Events", searchTerms);

        searchTerms = buildLabVesselMultiCols();
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
                "labVessel", BucketEntry.class.getName()));

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("labMetric", "labVesselId",
                "labMetrics", LabVessel.class.getName()));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("labMetrics", "labMetrics",
                "labMetricId", LabMetric.class.getName()));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("labMetricId", "labMetrics",
                "id", Metadata.class.getName()));


        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("mercurySample", "labVesselId",
                "mercurySamples", LabVessel.class.getName()));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("mercurySamples", "mercurySamples",
                "mercurySampleId", MercurySample.class.getName()));

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("inPlaceLabVesselId", "labVesselId",
                "inPlaceLabVesselId", LabEvent.class.getName()));

        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                "LabVessel", LabVessel.class.getName(), "label", 100, criteriaProjections, mapGroupSearchTerms);

        // TODO configurableSearchDefinition.addRowsListener( CONTEXT_KEY_BSP_SAMPLE_SEARCH, new BspSampleSearchAddRowsListener() );

        MAP_NAME_TO_DEF.put(ColumnEntity.LAB_VESSEL.getEntityName(), configurableSearchDefinition);
    }

    private void buildLabEventSearchDef() {
        Map<String, List<SearchTerm>> mapGroupSearchTerms = new LinkedHashMap<>();

        List<SearchTerm> searchTerms = buildLabEventBatch();
        mapGroupSearchTerms.put("Lab Batch", searchTerms);

        searchTerms = buildLabEventIds();
        mapGroupSearchTerms.put("IDs", searchTerms);

        searchTerms = buildLabEventVessel();
        mapGroupSearchTerms.put("Lab Vessel", searchTerms);

        searchTerms = buildLabEventReagents();
        mapGroupSearchTerms.put("Reagents", searchTerms);

        searchTerms = buildLabEventNestedTables();
        mapGroupSearchTerms.put("Nested Data", searchTerms);

        List<ConfigurableSearchDefinition.CriteriaProjection> criteriaProjections = new ArrayList<>();

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("inPlaceLabEvents", "inPlaceLabVesselId",
                "inPlaceLabEvents", LabVessel.class.getName()));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("bucketEntries", "labVesselId",
                "labVessel", BucketEntry.class.getName()));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection( "labBatch", "bucketEntries",
                "bucketEntry", LabBatch.class.getName()));

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection( "productOrderId", "bucketEntries",
                "bucketEntry", LabBatch.class.getName()));

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("reagent", "labEventId",
                "reagents", LabEvent.class.getName()));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("reagents", "reagents",
                "reagentId", Reagent.class.getName()));

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("mercurySample", "inPlaceLabVesselId",
                "mercurySamples", LabVessel.class.getName()));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("mercurySamples", "mercurySamples",
                "mercurySampleId", MercurySample.class.getName()));

        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                "LabEvent", LabEvent.class.getName(), "labEventId", 100, criteriaProjections, mapGroupSearchTerms);

        // Allow user to search ancestor and/or descendant events
        configurableSearchDefinition.setTraversalEvaluator(new LabEventTraversalEvaluator());

        MAP_NAME_TO_DEF.put(ColumnEntity.LAB_EVENT.getEntityName(), configurableSearchDefinition);
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
                List<String> results = new ArrayList<>();
                LabVessel labVessel = (LabVessel) entity;
                for (BucketEntry bucketEntry : labVessel.getBucketEntries()) {
                    if (bucketEntry.getLabBatch() != null) {
                        results.add(bucketEntry.getLabBatch().getBatchName());
                    }
                }
                return results;
            }
        });
        searchTerm.setValueConversionExpression(lcsetConverter);
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
                return findVesselType(labVessel);
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

    private SearchTerm buildLabVesselBspTerm(final BSPSampleSearchColumn bspSampleSearchColumn) {
        return buildLabVesselBspTerm(bspSampleSearchColumn, bspSampleSearchColumn.columnName());
    }

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
                            BspSampleSearchAddRowsListener.BSP_LISTENER);
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
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
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
                return Integer.valueOf( (String) context.get(CONTEXT_KEY_SEARCH_STRING));
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
        searchTerm.setValuesExpression( new EventTypeValuesExpression() );
        searchTerm.setValueConversionExpression( new EventTypeValueConversionExpression() );
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Imported Sample Position");
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                List<String> results = new ArrayList<>();
                LabVessel labVessel = (LabVessel) entity;
                Map<LabEvent, Set<LabVessel>> mapEventToVessels = labVessel.findVesselsForLabEventType(
                        LabEventType.SAMPLE_IMPORT, true);
                for (Map.Entry<LabEvent, Set<LabVessel>> eventVesselEntry : mapEventToVessels.entrySet()) {
                    for (LabVessel vessel : eventVesselEntry.getValue()) {
                        Set<VesselContainer<?>> containers = vessel.getContainers();
                        VesselContainer<?> container = null;
                        if (containers.size() > 1) {
                            for (VesselContainer<?> vesselContainer : containers) {
                                // todo jmt this comparison is not safe
                                if (vesselContainer.getEmbedder().getCreatedOn().getTime() / 1000L ==
                                        eventVesselEntry.getKey().getEventDate().getTime() / 1000L) {
                                    container = vesselContainer;
                                }
                            }
                        } else {
                            container = containers.iterator().next();
                        }
                        if (container != null) {
                            results.add(container.getPositionOfVessel(vessel).toString());
                        }
                    }
                }
                return results;
            }
        });
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
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                return getEventPosition((LabVessel) entity
                        , Collections.singletonList(LabEventType.INITIAL_TARE), false);
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Pond Sample Position");
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                return getEventPosition((LabVessel) entity
                        , Collections.singletonList(LabEventType.POND_REGISTRATION), true);
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
                return getEventPosition((LabVessel) entity
                        , Collections.singletonList(LabEventType.SHEARING_TRANSFER), false);
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
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                List<LabEventType> labEventTypes = new ArrayList<>();
                // ICE
                labEventTypes.add(LabEventType.ICE_CATCH_ENRICHMENT_CLEANUP);
                // Agilent
                labEventTypes.add(LabEventType.NORMALIZED_CATCH_REGISTRATION);

                return getEventPosition((LabVessel) entity, labEventTypes, true);
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
     * Traverse lab vessel events looking for specific types
     * @param labVessel
     * @param labEventTypes Mutually exclusive event types
     * @param useTargetContainer Should target container (vs. source container) be used for positions?
     * @return
     */
    private Object getEventPosition(LabVessel labVessel, List<LabEventType> labEventTypes, boolean useTargetContainer ) {

        List<String> results = new ArrayList<>();

        // Look for in-place
        Set<LabEvent> inPlaceEvents = labVessel.getInPlaceEventsWithContainers();
        for( LabEvent event : inPlaceEvents ) {
            if( labEventTypes.contains(event.getLabEventType()) ){
                VesselContainer container = event.getInPlaceLabVessel().getContainerRole();
                VesselPosition position = container.getPositionOfVessel(labVessel);
                if( position != null ) {
                    results.add(position.toString());
                }
            }
        }

        if( results.size() > 0 ) {
            return results;
        }

        // Lab event types
        Map<LabEvent, Set<LabVessel>> mapEventToVessels
                = labVessel.findVesselsForLabEventTypes( labEventTypes, useTargetContainer );

        if( mapEventToVessels.isEmpty() ) {
            return "";
        }

        LabEvent labEvent = mapEventToVessels.entrySet().iterator().next().getKey();
        Set<LabVessel> descendantLabVessels = mapEventToVessels.entrySet().iterator().next().getValue();
        VesselContainer vesselContainer = null;

        // In place vessel is a container where event has no transfers
        LabVessel inPlaceVessel = labEvent.getInPlaceLabVessel();
        if( inPlaceVessel != null ) {
            vesselContainer = inPlaceVessel.getContainerRole();
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

        if( vesselContainer == null || vesselContainer.getMapPositionToVessel().isEmpty() ) {
            return "";
        }

        VesselPosition position = vesselContainer.getPositionOfVessel(labVessel);
        results.add(position == null ? "" : position.toString());

        for (LabVessel descendantVessel : descendantLabVessels) {
            position = vesselContainer.getPositionOfVessel(descendantVessel);
            results.add(position == null ? "" : position.toString());
        }

        return results;
    }

    private List<String> getEventLabel(LabVessel labVessel, List<LabEventType> labEventTypes, boolean useTargetContainer) {
        List<String> results = new ArrayList<>();
        Map<LabEvent, Set<LabVessel>> mapEventToVessels = labVessel.findVesselsForLabEventTypes(labEventTypes, useTargetContainer);
        for (Map.Entry<LabEvent, Set<LabVessel>> eventVesselEntry : mapEventToVessels.entrySet()) {
            for (LabVessel vessel : eventVesselEntry.getValue()) {
                results.add(vessel.getLabel());
            }
        }
        return results;
    }

    private List<SearchTerm> buildLabEventIds() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("LabEventId");
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("labEventId");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabEvent labEvent = (LabEvent) entity;
                return labEvent.getLabEventId();
            }
        });
        searchTerm.setTypeExpression(new SearchTerm.Evaluator<String>() {
            @Override
            public String evaluate(Object entity, Map<String, Object> context) {
                return "Long";
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("EventDate");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("eventDate");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            private FastDateFormat dateFormat = FastDateFormat.getInstance( "MM/dd/yyyy HH:mm:ss");
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabEvent labEvent = (LabEvent) entity;
                return dateFormat.format( labEvent.getEventDate());
            }
        });
        searchTerm.setTypeExpression(new SearchTerm.Evaluator<String>() {
            @Override
            public String evaluate(Object entity, Map<String, Object> context) {
                return "Date";
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("EventLocation");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("eventLocation");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabEvent labEvent = (LabEvent) entity;
                return labEvent.getEventLocation();
            }
        });
        searchTerm.setValuesExpression(new SearchTerm.Evaluator<List<ConstrainedValue>>() {
            @Override
            public List<ConstrainedValue> evaluate(Object entity, Map<String, Object> context) {
                ConstrainedValueDao constrainedValueDao = (ConstrainedValueDao) context.get( CONTEXT_KEY_OPTION_VALUE_DAO);
                return constrainedValueDao.getLabEventLocationOptionList();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("EventOperator");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("eventOperator");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                BSPUserList bspUserList = (BSPUserList)context.get(CONTEXT_KEY_BSP_USER_LIST);
                LabEvent labEvent = (LabEvent) entity;
                Long userId = labEvent.getEventOperator();
                BspUser bspUser = bspUserList.getById(userId);
                if (bspUser == null) {
                    return "Unknown user - ID: " + userId;
                }

                return bspUser.getFullName();
            }
        });
        searchTerm.setValuesExpression(new SearchTerm.Evaluator<List<ConstrainedValue>>() {
            // Pick actual users out of lab events
            @Override
            public List<ConstrainedValue> evaluate(Object entity, Map<String, Object> context) {
                ConstrainedValueDao constrainedValueDao = (ConstrainedValueDao) context.get(CONTEXT_KEY_OPTION_VALUE_DAO);
                return constrainedValueDao.getLabEventUserNameList();
            }
        });
        searchTerm.setValueConversionExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                return Long.valueOf( (String) context.get(CONTEXT_KEY_SEARCH_STRING));
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("EventType");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("labEventType");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabEvent labEvent = (LabEvent) entity;
                return labEvent.getLabEventType().getName();
            }
        });
        searchTerm.setValuesExpression( new EventTypeValuesExpression() );
        searchTerm.setValueConversionExpression( new EventTypeValueConversionExpression() );
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Program Name");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("programName");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            //private BSPUserList bspUserList = ServiceAccessUtility.getBean(BSPUserList.class);

            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabEvent labEvent = (LabEvent) entity;
                String programName = labEvent.getProgramName();
                return (programName == null ? "" : programName);

            }
        });
        searchTerm.setValuesExpression(new SearchTerm.Evaluator<List<ConstrainedValue>>() {
            @Override
            public List<ConstrainedValue> evaluate(Object entity, Map<String, Object> context) {
                ConstrainedValueDao constrainedValueDao = (ConstrainedValueDao) context.get(CONTEXT_KEY_OPTION_VALUE_DAO);
                return constrainedValueDao.getLabEventProgramNameList();
            }
        });
        searchTerms.add(searchTerm);

        return searchTerms;
    }


    /**
     * Top down of reagent nested table
     * @return List of search terms/column definitions for lab event reagent
     */
    private List<SearchTerm> buildLabEventNestedTables() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm parentSearchTerm = new SearchTerm();
        parentSearchTerm.setName("Lab Event Reagents");
        parentSearchTerm.setIsNestedParent(Boolean.TRUE);
        parentSearchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabEvent labEvent = (LabEvent) entity;
                return labEvent.getReagents();
            }
        });
        searchTerms.add(parentSearchTerm);

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Reagent Type");
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("name");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                Reagent reagent = (Reagent) entity;
                return ( reagent.getName()==null?"":reagent.getName() );
            }
        });
        parentSearchTerm.addNestedEntityColumn(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Reagent Lot");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("lot");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                Reagent reagent = (Reagent) entity;
                return ( reagent.getLot()==null?"":reagent.getLot() );
            }
        });
        parentSearchTerm.addNestedEntityColumn(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Reagent Expiration");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("expiration");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                Reagent reagent = (Reagent) entity;
                return ( reagent.getExpiration()==null?"":reagent.getExpiration());
            }
        });
        parentSearchTerm.addNestedEntityColumn(searchTerm);

        parentSearchTerm = new SearchTerm();
        parentSearchTerm.setName("Source Layout");
        parentSearchTerm.setIsNestedParent(Boolean.TRUE);
        parentSearchTerm.setPluginClass(EventVesselSourcePositionPlugin.class);
        parentSearchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabEvent labEvent = (LabEvent) entity;
                return labEvent.getReagents();
            }
        });
        searchTerms.add(parentSearchTerm);

        parentSearchTerm = new SearchTerm();
        parentSearchTerm.setName("Destination Layout");
        parentSearchTerm.setIsNestedParent(Boolean.TRUE);
        parentSearchTerm.setPluginClass(EventVesselTargetPositionPlugin.class);
        parentSearchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabEvent labEvent = (LabEvent) entity;
                return labEvent.getReagents();
            }
        });
        searchTerms.add(parentSearchTerm);


        return searchTerms;
    }

    /**
     * Searchable reagent fields
     * @return List of search terms/column definitions for lab event reagent
     */
    private List<SearchTerm> buildLabEventReagents() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Reagent Type");
        searchTerm.setDisplayExpression( new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabEvent labEvent = (LabEvent)entity;
                List<String> reagents = new ArrayList<>();
                for (Reagent reagent : labEvent.getReagents()) {
                    reagents.add(reagent.getName());
                }
                return reagents;
            }
        });
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "reagent", "reagents" ));
        criteriaPath.setPropertyName("name");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);

        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Reagent Lot");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("reagent", "reagents"));
        criteriaPath.setPropertyName("lot");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression( new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabEvent labEvent = (LabEvent)entity;
                List<String> reagents = new ArrayList<>();
                for (Reagent reagent : labEvent.getReagents()) {
                    reagents.add(reagent.getLot());
                }
                return reagents;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Reagent Expiration");
        searchTerm.setTypeExpression(new SearchTerm.Evaluator<String>() {
            @Override
            public String evaluate(Object entity, Map<String, Object> context) {
                return "Date";
            }
        });
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("reagent", "reagents"));
        criteriaPath.setPropertyName("expiration");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression( new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabEvent labEvent = (LabEvent)entity;
                List<Date> reagents = new ArrayList<>();
                for (Reagent reagent : labEvent.getReagents()) {
                    reagents.add(reagent.getExpiration());
                }
                return reagents;
            }
        });
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    private List<SearchTerm> buildLabEventBatch() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("PDO");
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList(/* LabEvent*/ "inPlaceLabEvents", /* LabVessel */ "bucketEntries", /* BucketEntry */ "productOrder" /* ProductOrder */));
        criteriaPath.setPropertyName("jiraTicketKey");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabEvent labEvent = (LabEvent) entity;
                labEvent.getProgramName();
                Set<String> productNames = new HashSet<String>();
                LabVessel labVessel = labEvent.getInPlaceLabVessel();

                // Test req'd, DB columns are nullable
                if (labVessel != null) {
                    Set<BucketEntry> bucketEntries = labVessel.getBucketEntries();
                    if ( bucketEntries != null && !bucketEntries.isEmpty() ) {
                        Iterator<BucketEntry> iterator = bucketEntries.iterator();
                        BucketEntry bucketEntry = iterator.next();
                        productNames.add( bucketEntry.getProductOrder().getJiraTicketKey() );
                    }
                }

                return new ArrayList( productNames);
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("LCSET");
        searchTerm.setValueConversionExpression(lcsetConverter);
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();

        criteriaPath.setCriteria(
                Arrays.asList(/* LabEvent*/ "inPlaceLabEvents", /* LabVessel */ "bucketEntries", /* BucketEntry */
                        "labBatch" /* LabBatch */));
        criteriaPath.setPropertyName("batchName");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabEvent labEvent = (LabEvent) entity;
                List<String> lcSetNames = new ArrayList<>();

                for (LabBatch labBatch : labEvent.getComputedLcSets()) {
                    lcSetNames.add(labBatch.getBatchName());
                }

                if (lcSetNames.isEmpty()) {
                    LabVessel labVessel = labEvent.getInPlaceLabVessel();
                    // Test req'd, DB columns are nullable
                    if (labVessel != null) {
                        Set<BucketEntry> bucketEntries = labVessel.getBucketEntries();
                        if (bucketEntries != null && !bucketEntries.isEmpty()) {
                            Iterator<BucketEntry> iterator = bucketEntries.iterator();
                            BucketEntry bucketEntry = iterator.next();
                            LabBatch batch = bucketEntry.getLabBatch();
                            if (batch != null) {
                                lcSetNames.add(batch.getBatchName());
                            }
                        }
                    }
                }

                return lcSetNames;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Mercury Sample ID");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("mercurySample", "mercurySamples"));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabEvent labEvent = (LabEvent) entity;
                List<String> results = new ArrayList<>();

                LabVessel labVessel = labEvent.getInPlaceLabVessel();
                // Test req'd, DB columns are nullable
                if (labVessel != null) {
                    for (MercurySample sample : labVessel.getMercurySamples()) {
                        results.add(sample.getSampleKey());
                    }
                }
                return results;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("In-Place Vessel Barcode");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("inPlaceLabEvents"));
        criteriaPath.setPropertyName("label");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabEvent labEvent = (LabEvent) entity;

                LabVessel labVessel = labEvent.getInPlaceLabVessel();
                // Test req'd, DB columns are nullable
                if (labVessel != null) {
                    return labVessel.getLabel();
                }
                return "";
            }
        });
        searchTerms.add(searchTerm);


        return searchTerms;
    }

    /**
     * @return
     */
    private List<SearchTerm> buildLabEventVessel() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Source Lab Vessel Type");
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                return findEventSourceContainerType((LabEvent) entity);
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Source Barcode");
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                List<String> results = new ArrayList<>();
                LabEvent labEvent = (LabEvent) entity;

                LabVessel inPlaceLabVessel = labEvent.getInPlaceLabVessel();

                // Ignore source vessels for in-place events
                if( inPlaceLabVessel != null ) {
                    return results;
                }

                for (LabVessel vessel : labEvent.getSourceLabVessels() ) {
                    if( OrmUtil.proxySafeIsInstance( vessel, TubeFormation.class )) {
                        TubeFormation tubes = OrmUtil.proxySafeCast(vessel, TubeFormation.class);
                        for ( RackOfTubes rack : tubes.getRacksOfTubes()) {
                            results.add(rack.getLabel());
                        }
                    } else {
                        results.add(vessel.getLabel());
                    }
                }
                return results;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Destination Lab Vessel Type");
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                return findEventTargetContainerType( (LabEvent) entity );
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Destination Barcode");
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                List<String> results = new ArrayList<>();
                LabEvent labEvent = (LabEvent) entity;

                LabVessel inPlaceLabVessel = labEvent.getInPlaceLabVessel();
                if( inPlaceLabVessel != null ) {
                    // This will do for now, but looking at ancillary vessel in VesselTransfer is more reliable.
                    if( OrmUtil.proxySafeIsInstance( inPlaceLabVessel, TubeFormation.class )) {
                        TubeFormation tubes = OrmUtil.proxySafeCast(inPlaceLabVessel, TubeFormation.class);
                        for ( RackOfTubes rack : tubes.getRacksOfTubes()) {
                            results.add(rack.getLabel());
                        }
                    } else {
                        results.add( inPlaceLabVessel.getLabel() );
                    }
                    return results;
                }

                for (LabVessel vessel : labEvent.getTargetLabVessels()) {
                    if( OrmUtil.proxySafeIsInstance( vessel, TubeFormation.class )) {
                        TubeFormation tubes = OrmUtil.proxySafeCast(vessel, TubeFormation.class);
                        for ( RackOfTubes rack : tubes.getRacksOfTubes()) {
                            results.add(rack.getLabel());
                        }
                    } else {
                        results.add(vessel.getLabel());
                    }
                }
                if( results.isEmpty() && labEvent.getInPlaceLabVessel() != null ) {
                    if( labEvent.getInPlaceLabVessel().getContainerRole() != null ) {
                        results.add( labEvent.getInPlaceLabVessel().getContainerRole().getEmbedder().getLabel() );
                    } else {
                        results.add( labEvent.getInPlaceLabVessel().getLabel() );
                    }
                }

                return results;
            }
        });
        searchTerms.add(searchTerm);

        return searchTerms;
    }


    /**
     * Find the source vessel type name associated with an event, container name if a container
     */
    private String findEventSourceContainerType( LabEvent labEvent ) {
        String vesselTypeName = "";

        LabVessel vessel = labEvent.getInPlaceLabVessel();

        if( vessel == null ){
            for (LabVessel srcVessel : labEvent.getSourceLabVessels()) {
                if( srcVessel.getContainerRole() != null ) {
                    vessel = srcVessel.getContainerRole().getEmbedder();
                } else {
                    vessel = srcVessel;
                }
                break;
            }
        } else {
            // Ignore source vessels for in-place events
            return vesselTypeName;
        }

        if( vessel != null ) {
            vesselTypeName = findVesselType( vessel );
        }

        return vesselTypeName;
    }

    /**
     * Find the target vessel type name associated with an event, container name if a container
     */
    private String findEventTargetContainerType( LabEvent labEvent ) {
        String vesselTypeName = "";

        LabVessel vessel = labEvent.getInPlaceLabVessel();

        if( vessel == null ){
            for (LabVessel srcVessel : labEvent.getTargetLabVessels()) {
                if( srcVessel.getContainerRole() != null ) {
                    vessel = srcVessel.getContainerRole().getEmbedder();
                } else {
                    vessel = srcVessel;
                }
                break;
            }
        }

        if( vessel != null ) {
            vesselTypeName = findVesselType( vessel );
        }

        return vesselTypeName;
    }

    private String findVesselType( LabVessel vessel ) {
        String vesselTypeName;
        switch( vessel.getType() ) {
        case STATIC_PLATE:
            StaticPlate p = OrmUtil.proxySafeCast(vessel, StaticPlate.class );
            vesselTypeName = p.getPlateType()==null?"":p.getPlateType().getAutomationName();
            break;
        case TUBE_FORMATION:
            TubeFormation tf = OrmUtil.proxySafeCast(vessel, TubeFormation.class );
            vesselTypeName = tf.getRackType()==null?"":tf.getRackType().getDisplayName();
            break;
        case RACK_OF_TUBES:
            RackOfTubes rot = OrmUtil.proxySafeCast(vessel, RackOfTubes.class );
            vesselTypeName = rot.getRackType()==null?"":rot.getRackType().getDisplayName();
            break;
        default:
            // Not sure of others for in-place vessels
            vesselTypeName = vessel.getType()==null?"":vessel.getType().getName();
        }
        return vesselTypeName;
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
        criteriaPath.setCriteria(Arrays.asList( "mercurySample", "mercurySamples" ));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPath.setJoinFetch(Boolean.TRUE);
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);

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
                    BigDecimal ng = labMetric.getTotalNg();
                    if( ng != null ) {
                        value = MathUtils.scaleTwoDecimalPlaces(ng).toPlainString();
                    }
                    break;
                }
                return value;

            }
        });
        searchTerm.setValueConversionExpression( new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                return new BigDecimal( (String) context.get(CONTEXT_KEY_SEARCH_STRING));
            }
        });
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "labMetric" /* LabVessel */, "labMetrics" /* LabMetric */, "metadataSet" /* Metadata */ ));
        criteriaPath.setPropertyName("numberValue");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);

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
                        (SearchInstance.SearchValue) context.get(CONTEXT_KEY_SEARCH_VALUE);
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
                SearchInstance.SearchValue searchValue = (SearchInstance.SearchValue) context.get(CONTEXT_KEY_SEARCH_VALUE);
                header = searchValue.getParent().getValues().get(0);
                Metadata.Key key = Metadata.Key.valueOf(header);
                if( key != null ) {
                    return key.getDisplayName();
                } else {
                    return header;
                }
            }
        });
        searchTerm.setTypeExpression(new SearchTerm.Evaluator<String>() {
            @Override
            public String evaluate(Object entity, Map<String, Object> context) {
                SearchInstance.SearchValue searchValue = (SearchInstance.SearchValue) context.get(CONTEXT_KEY_SEARCH_VALUE);
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
                SearchInstance.SearchValue searchValue = (SearchInstance.SearchValue) context.get(CONTEXT_KEY_SEARCH_VALUE);
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
                return Enum.valueOf(Metadata.Key.class, (String) context.get(CONTEXT_KEY_SEARCH_STRING));
            }
        });
        // Don't want this option in selectable columns
        searchTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerm.setDependentSearchTerms(childSearchTerms);
        searchTerm.setAddDependentTermsToSearchTermList(Boolean.TRUE);
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "mercurySample", "mercurySamples", "metadata" ));
        criteriaPath.setPropertyName("key");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        // ******** Allow individual selectable result columns for each sample metadata value *******
        SampleMetadataDisplayExpression sampleMetadataDisplayExpression = new SampleMetadataDisplayExpression();
        for (Metadata.Key meta : Metadata.Key.values()) {
            if (meta.getCategory() == Metadata.Category.SAMPLE) {
                searchTerm = new SearchTerm();
                searchTerm.setName(meta.getDisplayName());
                searchTerm.setDisplayExpression(sampleMetadataDisplayExpression);
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
     * Shared value list of all lab event types.
     */
    private class EventTypeValuesExpression extends SearchTerm.Evaluator<List<ConstrainedValue>> {
        @Override
        public List<ConstrainedValue> evaluate(Object entity, Map<String, Object> context) {
            List<ConstrainedValue> constrainedValues = new ArrayList<>();
            for (LabEventType labEventType : LabEventType.values()) {
                constrainedValues.add(new ConstrainedValue(labEventType.toString(), labEventType.getName()));
            }
            Collections.sort(constrainedValues);
            return constrainedValues;
        }
    }

    /**
     * Shared conversion of input String to LabEventType enumeration value
     */
    private class EventTypeValueConversionExpression extends SearchTerm.Evaluator<Object> {
        @Override
        public Object evaluate(Object entity, Map<String, Object> context) {
            return Enum.valueOf(LabEventType.class, (String) context.get(CONTEXT_KEY_SEARCH_STRING));
        }
    }

    /**
     * Shared display expression for sample metadata
     */
    private class SampleMetadataDisplayExpression extends SearchTerm.Evaluator<Object> {

        // Put a quick way to lookup key by display name in place
        // TODO: With only this one use-case, should this be part of Metadata.Key?
        private Map<String,Metadata.Key> keyMap = new HashMap<>();

        public SampleMetadataDisplayExpression(){
            for(Metadata.Key key : Metadata.Key.values() ){
                if( key.getCategory() == Metadata.Category.SAMPLE ) {
                    keyMap.put(key.getDisplayName(), key);
                }
            }
        }

        @Override
        public Object evaluate(Object entity, Map<String, Object> context) {
            SearchTerm searchTerm = (SearchTerm) context.get(SearchDefinitionFactory.CONTEXT_KEY_SEARCH_TERM);
            String metaName = searchTerm.getName();
            String value = "";
            LabVessel labVessel = (LabVessel) entity;

            for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                MercurySample sample = sampleInstanceV2.getRootOrEarliestMercurySample();
                Set<Metadata> metadata = sample.getMetadata();
                if( metadata != null && !metadata.isEmpty() ) {
                    Metadata.Key key = keyMap.get(metaName);
                    for( Metadata meta : metadata){
                        if( meta.getKey() == key ) {
                            value = value + meta.getValue() + " ";
                            // Assume only one metadata type (e.g. Gender, Sample ID) per vessel.
                            break;
                        }
                    }
                }
            }

            return value;
        }
    }

}
