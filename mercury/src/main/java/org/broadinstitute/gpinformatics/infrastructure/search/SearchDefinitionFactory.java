package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.columns.BspSampleSearchAddRowsListener;
import org.broadinstitute.gpinformatics.infrastructure.columns.EventVesselSourcePositionPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.EventVesselTargetPositionPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.LabVesselMetricPlugin;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.util.ArrayList;
import java.util.Arrays;
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

    private Map<String, ConfigurableSearchDefinition> mapNameToDef = new HashMap<>();

    public static final String CONTEXT_KEY_BSP_USER_LIST = "bspUserList";
    public static final String CONTEXT_KEY_COLUMN_SET_TYPE = "columnSetType";
    public static final String CONTEXT_KEY_SEARCH_VALUE = "searchValue";
    public static final String CONTEXT_KEY_SEARCH_STRING = "searchString";
    public static final String CONTEXT_KEY_BSP_SAMPLE_SEARCH = "BSPSampleSearchService";
    public static final String CONTEXT_KEY_OPTION_VALUE_DAO = "OptionValueDao";

    public ConfigurableSearchDefinition getForEntity(String entity) {
        if (mapNameToDef.isEmpty()) {
            ConfigurableSearchDefinition configurableSearchDefinition = buildLabVesselSearchDef();
            mapNameToDef.put(configurableSearchDefinition.getName(), configurableSearchDefinition);
            configurableSearchDefinition = buildLabEventSearchDef();
            mapNameToDef.put(configurableSearchDefinition.getName(), configurableSearchDefinition);
        }
        return mapNameToDef.get(entity);
    }

    public ConfigurableSearchDefinition buildLabVesselSearchDef() {
        Map<String, List<SearchTerm>> mapGroupSearchTerms = new LinkedHashMap<>();

        List<SearchTerm> searchTerms = buildLabVesselIds();
        mapGroupSearchTerms.put("IDs", searchTerms);

        // Are there alternatives to search terms that aren't searchable?  Should they be in a different structure, then merged with search terms for display?

        // XX version - from workflow? 3.2 doesn't seem to be in XML
        // Start date - LabBatch.createdOn? usually 1 day before "scheduled to start"
        // Due date - LabBatch.dueDate is transient!
        searchTerms = buildLabVesselBsp();
        mapGroupSearchTerms.put("BSP", searchTerms);

        searchTerms = buildLabVesselBuckets();
        mapGroupSearchTerms.put("Buckets", searchTerms);

        searchTerms = buildLabVesselEvent();
        mapGroupSearchTerms.put("Events", searchTerms);

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
        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                "LabVessel", LabVessel.class.getName(), "label", 100, criteriaProjections, mapGroupSearchTerms);
        mapNameToDef.put(configurableSearchDefinition.getName(), configurableSearchDefinition);
        return configurableSearchDefinition;
    }

    public ConfigurableSearchDefinition buildLabEventSearchDef() {
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


        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                "LabEvent", LabEvent.class.getName(), "labEventId", 100, criteriaProjections, mapGroupSearchTerms);
        mapNameToDef.put(configurableSearchDefinition.getName(), configurableSearchDefinition);
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
        searchTerm.setName("Lab Metrics");
        searchTerm.setPluginClass(LabVesselMetricPlugin.class);
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    private List<SearchTerm> buildLabVesselBsp() {
        List<SearchTerm> searchTerms = new ArrayList<>();
        // Non-searchable data from BSP
        searchTerms.add(buildLabVesselBspTerm(BSPSampleSearchColumn.STOCK_SAMPLE, "Stock Sample ID"));
        searchTerms.add(buildLabVesselBspTerm(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID));
        searchTerms.add(buildLabVesselBspTerm(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID ));
        searchTerms.add(buildLabVesselBspTerm(BSPSampleSearchColumn.SAMPLE_TYPE));
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
                Set<MercurySample> mercurySamples = labVessel.getMercurySamples();
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

        // Original LCSET - if rework, get non-rework bucket entry

        return searchTerms;
    }

    private List<SearchTerm> buildLabVesselEvent() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Imported Sample Well");
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                List<String> results = new ArrayList<>();
                LabVessel labVessel = (LabVessel) entity;
                Map<LabEvent, Set<LabVessel>> mapEventToVessels = labVessel.findVesselsForLabEventType(
                        LabEventType.SAMPLE_IMPORT);
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
                return getEventLabel((LabVessel) entity, LabEventType.SAMPLE_IMPORT);
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
                        LabEventType.SAMPLE_IMPORT);
                for (Map.Entry<LabEvent, Set<LabVessel>> eventVesselEntry : mapEventToVessels.entrySet()) {
                    for (LabVessel vessel : eventVesselEntry.getValue()) {
                        Set<MercurySample> mercurySamples = vessel.getMercurySamples();
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
        searchTerm.setName("Pond Sample Well");
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                return getEventPosition((LabVessel) entity, LabEventType.POND_REGISTRATION);
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Pond Tube Barcode");
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                return getEventLabel((LabVessel) entity, LabEventType.POND_REGISTRATION);
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Catch Sample Well");
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                return getEventPosition((LabVessel) entity, LabEventType.NORMALIZED_CATCH_REGISTRATION);
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Catch Tube Barcode");
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                return getEventLabel((LabVessel) entity, LabEventType.NORMALIZED_CATCH_REGISTRATION);
            }
        });
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    private Object getEventPosition(LabVessel labVessel, LabEventType labEventType) {
        List<String> results = new ArrayList<>();
        Map<LabEvent, Set<LabVessel>> mapEventToVessels = labVessel.findVesselsForLabEventType(
                labEventType);
        for (Map.Entry<LabEvent, Set<LabVessel>> eventVesselEntry : mapEventToVessels.entrySet()) {
            for (LabVessel vessel : eventVesselEntry.getValue()) {
                Set<SectionTransfer> sectionTransfers = eventVesselEntry.getKey().getSectionTransfers();
                if( !sectionTransfers.isEmpty() ) {
                    VesselContainer container = sectionTransfers.iterator().next().getTargetVesselContainer();
                    if( container != null ) {
                        VesselPosition position = container.getPositionOfVessel(vessel);
                        results.add(position==null?"":position.toString());
                    }
                }
            }
        }
        return results;
    }

    private List<String> getEventLabel(LabVessel labVessel, LabEventType labEventType) {
        List<String> results = new ArrayList<>();
        Map<LabEvent, Set<LabVessel>> mapEventToVessels = labVessel.findVesselsForLabEventType(labEventType);
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
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabEvent labEvent = (LabEvent) entity;
                return labEvent.getEventDate();
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
        searchTerm.setValuesExpression(new SearchTerm.Evaluator<List<ConstrainedValue>>() {
            @Override
            public List<ConstrainedValue> evaluate(Object entity, Map<String, Object> context) {
                List<ConstrainedValue> constrainedValues = new ArrayList<>();
                for (LabEventType labEventType : LabEventType.values()) {
                    constrainedValues.add(new ConstrainedValue(labEventType.toString(), labEventType.getName()));
                }
                Collections.sort(constrainedValues);
                return constrainedValues;
            }
        });
        searchTerm.setValueConversionExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                return Enum.valueOf(LabEventType.class, (String) context.get(CONTEXT_KEY_SEARCH_STRING));
            }
        });
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
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();

        criteriaPath.setCriteria(Arrays.asList(/* LabEvent*/ "inPlaceLabEvents", /* LabVessel */ "bucketEntries", /* BucketEntry */ "labBatch" /* LabBatch */));
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

                if( lcSetNames.isEmpty() ) {
                    LabVessel labVessel = labEvent.getInPlaceLabVessel();
                    // Test req'd, DB columns are nullable
                    if (labVessel != null) {
                        Set<BucketEntry> bucketEntries = labVessel.getBucketEntries();
                        if ( bucketEntries != null && !bucketEntries.isEmpty() ) {
                            Iterator<BucketEntry> iterator = bucketEntries.iterator();
                            BucketEntry bucketEntry = iterator.next();
                            LabBatch batch = bucketEntry.getLabBatch();
                            if( batch != null ) {
                                lcSetNames.add( batch.getBatchName() );
                            }
                        }
                    }
                }

                return lcSetNames;
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

                for (LabVessel vessel : labEvent.getSourceLabVessels()) {
                    if( vessel instanceof TubeFormation) {
                        TubeFormation tubes = (TubeFormation) vessel;
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

                for (LabVessel vessel : labEvent.getTargetLabVessels()) {
                    if( vessel instanceof TubeFormation) {
                        TubeFormation tubes = (TubeFormation) vessel;
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

}
