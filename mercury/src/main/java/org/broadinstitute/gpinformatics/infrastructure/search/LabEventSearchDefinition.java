package org.broadinstitute.gpinformatics.infrastructure.search;

import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.EventVesselSourcePositionPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.EventVesselTargetPositionPlugin;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToVesselTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds ConfigurableSearchDefinition for lab event user defined search logic
 */
public class LabEventSearchDefinition {

    public LabEventSearchDefinition(){}

    public ConfigurableSearchDefinition buildSearchDefinition() {
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

        // This only works for in place vessel barcode search term
        // A LabEvent OR clause with mix of event ids and in place vessel ids blows up Oracle CBO
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("inPlaceLabEvents", "inPlaceLabVesselId",
                "inPlaceLabEvents", LabVessel.class));

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("reagent", "labEventId",
                "reagents", LabEvent.class));

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("mercurySample", "inPlaceLabVesselId",
                "mercurySamples", LabVessel.class));

        // LabVessel to transfer
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("sectXfer", "labEventId",
                "labEvent", SectionTransfer.class));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("vessSectXfer", "labEventId",
                "labEvent", VesselToSectionTransfer.class));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("vessVessXfer", "labEventId",
                "labEvent", VesselToVesselTransfer.class));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("cherryPickXfer", "labEventId",
                "labEvent", CherryPickTransfer.class));

        // Pick the containers out of a lab vessel subquery to find vessel transfers
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("vesselContainer", "container.labVesselId",
                "containers", "container", LabVessel.class));

        // Put the query for Event Vessel Barcode in a sub query
        // Blows up Oracle CBO if a mix of event OR columns: (8) labEventId clauses with (1) inPlaceLabVesselId
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("eventById", "labEventId",
                "labEventId", LabEvent.class));
        // Pick the inPlaceLabEvents out of lab vessel subquery to find lab events
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("inPlaceLabEventSubQuery", "event.inPlaceLabVesselId",
                "inPlaceLabEvents", "event", LabVessel.class));

        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                ColumnEntity.LAB_EVENT, 100, criteriaProjections, mapGroupSearchTerms);

        // Allow user to search ancestor and/or descendant events
        configurableSearchDefinition.addTraversalEvaluator("ancestorOptionEnabled"
                , new LabEventTraversalEvaluator.AncestorTraversalEvaluator());
        configurableSearchDefinition.addTraversalEvaluator("descendantOptionEnabled"
                , new LabEventTraversalEvaluator.DescendantTraversalEvaluator());

        return configurableSearchDefinition;
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
                ConstrainedValueDao constrainedValueDao = (ConstrainedValueDao) context.get( SearchInstance.CONTEXT_KEY_OPTION_VALUE_DAO);
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
                BSPUserList bspUserList = (BSPUserList)context.get(SearchInstance.CONTEXT_KEY_BSP_USER_LIST);
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
                ConstrainedValueDao constrainedValueDao = (ConstrainedValueDao) context.get(SearchInstance.CONTEXT_KEY_OPTION_VALUE_DAO);
                return constrainedValueDao.getLabEventUserNameList();
            }
        });
        searchTerm.setValueConversionExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                return Long.valueOf( (String) context.get(SearchInstance.CONTEXT_KEY_SEARCH_STRING));
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
        searchTerm.setValuesExpression( new SearchDefinitionFactory.EventTypeValuesExpression() );
        searchTerm.setValueConversionExpression( new SearchDefinitionFactory.EventTypeValueConversionExpression() );
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
                ConstrainedValueDao constrainedValueDao = (ConstrainedValueDao) context.get(SearchInstance.CONTEXT_KEY_OPTION_VALUE_DAO);
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
        criteriaPath.setCriteria(Arrays.asList("reagent", "reagents"));
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
        searchTerm.setValueConversionExpression(SearchDefinitionFactory.getPdoInputConverter());
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
        searchTerm.setValueConversionExpression(SearchDefinitionFactory.getLcsetInputConverter());
        criteriaPaths = new ArrayList<>();
        // Non-reworks
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList(/* LabEvent*/ "inPlaceLabEvents", /* LabVessel */ "labBatches",
                /* LabBatchStartingVessel */ "labBatch" /* LabBatch */));
        criteriaPath.setPropertyName("batchName");
        criteriaPaths.add(criteriaPath);
        // Reworks
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList(/* LabEvent*/ "inPlaceLabEvents" /* LabVessel */,
                "reworkLabBatches" /* LabBatch */));
        criteriaPath.setPropertyName("batchName");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabEvent labEvent = (LabEvent) entity;
                Set<String> lcSetNames = new HashSet<>();

                for (LabBatch labBatch : labEvent.getComputedLcSets()) {
                    lcSetNames.add(labBatch.getBatchName());
                }

                // todo jmt improve getComputedLcSets so this logic is not necessary
                if (lcSetNames.isEmpty()) {
                    LabVessel labVessel = labEvent.getInPlaceLabVessel();
                    if (labVessel != null) {
                        if( labVessel.getContainerRole() != null ) {
                            for( LabVessel containedVessel : labVessel.getContainerRole().getContainedVessels() ) {
                                for( LabBatch batch : containedVessel.getLabBatches() ) {
                                    if (batch.getLabBatchType() == LabBatch.LabBatchType.WORKFLOW) {
                                        lcSetNames.add(batch.getBatchName());
                                    }
                                }
                                if( lcSetNames.isEmpty()){
                                    for( LabEvent xferEvent : containedVessel.getTransfersTo() ) {
                                        for (LabBatch labBatch : xferEvent.getComputedLcSets()) {
                                            lcSetNames.add(labBatch.getBatchName());
                                        }
                                    }
                                }
                            }
                        }
                        // In place vessel is not a container (could also be static plate)
                        for (LabBatch batch : labVessel.getLabBatches()) {
                            if (batch.getLabBatchType() == LabBatch.LabBatchType.WORKFLOW) {
                                lcSetNames.add(batch.getBatchName());
                            }
                        }
                        if( lcSetNames.isEmpty()){
                            for( LabEvent xferEvent : labVessel.getTransfersTo() ) {
                                for (LabBatch labBatch : xferEvent.getComputedLcSets()) {
                                    lcSetNames.add(labBatch.getBatchName());
                                }
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
                if (labVessel != null) {
                    for (MercurySample sample : labVessel.getMercurySamples()) {
                        results.add(sample.getSampleKey());
                    }
                }
                return results;
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
        searchTerm.setName("In-Place Vessel Barcode");
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("inPlaceLabEvents"));
        criteriaPath.setPropertyName("label");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabEvent labEvent = (LabEvent) entity;
                LabVessel labVessel = labEvent.getInPlaceLabVessel();
                if (labVessel != null) {
                    return labVessel.getLabel();
                }
                return "";
            }
        });
        searchTerms.add(searchTerm);

        // Any vessel barcode in an event
        searchTerm = new SearchTerm();
        searchTerm.setName("Event Vessel Barcode");
        // Do not show in output - redundant with source/target layouts
        searchTerm.setIsExcludedFromResultColumns( Boolean.TRUE);
        criteriaPaths = new ArrayList<>();

        // Search by in place lab vessel
        // Need a nested criteria path to get a lab event list for a tube barcode
        // (see criteria projection for reason)
        SearchTerm.CriteriaPath nestedCriteriaPath = new SearchTerm.CriteriaPath();
        nestedCriteriaPath.setCriteria(Arrays.asList( "inPlaceLabEventSubQuery" ) );

        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("eventById", "inPlaceLabVesselId" ));
        criteriaPath.setNestedCriteriaPath(nestedCriteriaPath);
        criteriaPath.setPropertyName("label");
        criteriaPaths.add(criteriaPath);

        // Search by section transfer target lab vessel
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "sectXfer", "targetVessel" ));
        criteriaPath.setPropertyName("label");
        criteriaPaths.add(criteriaPath);

        // Search by section transfer source lab vessel
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "sectXfer", "sourceVessel" ));
        criteriaPath.setPropertyName("label");
        criteriaPaths.add(criteriaPath);

        // Search by vessel to section transfer target lab vessel
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "vessSectXfer", "targetVessel" ));
        criteriaPath.setPropertyName("label");
        criteriaPaths.add(criteriaPath);

        // Search by vessel to section transfer source lab vessel
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "vessSectXfer", "sourceVessel" ));
        criteriaPath.setPropertyName("label");
        criteriaPaths.add(criteriaPath);

        // Search by vessel to vessel transfer target lab vessel
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "vessVessXfer", "targetVessel" ));
        criteriaPath.setPropertyName("label");
        criteriaPaths.add(criteriaPath);

        // Search by vessel to vessel transfer source lab vessel
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "vessVessXfer", "sourceVessel" ));
        criteriaPath.setPropertyName("label");
        criteriaPaths.add(criteriaPath);

        // **** Logic to find and use the container of a vessel (TubeFormation)  in an event source/target ****

        // Need a nested criteria path to get a vessel container list for a tube barcode
        nestedCriteriaPath = new SearchTerm.CriteriaPath();
        nestedCriteriaPath.setCriteria(Arrays.asList( "vesselContainer" ) );

        // Search by cherry pick transfer target lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "cherryPickXfer", "targetVessel" ));
        criteriaPath.setPropertyName("label");
        criteriaPath.setNestedCriteriaPath(nestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        // Search by cherry pick transfer source lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "cherryPickXfer", "sourceVessel" ));
        criteriaPath.setPropertyName("label");
        criteriaPath.setNestedCriteriaPath(nestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        // Search by section transfer source lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "sectXfer", "sourceVessel" ));
        criteriaPath.setPropertyName("label");
        criteriaPath.setNestedCriteriaPath(nestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        // Search by section transfer target lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "sectXfer", "targetVessel" ));
        criteriaPath.setPropertyName("label");
        criteriaPath.setNestedCriteriaPath(nestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        // Search by vessel to section transfer target lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "vessSectXfer", "targetVessel" ));
        criteriaPath.setPropertyName("label");
        criteriaPath.setNestedCriteriaPath(nestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        // Search by vessel to section transfer source lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "vessSectXfer", "sourceVessel" ));
        criteriaPath.setPropertyName("label");
        criteriaPath.setNestedCriteriaPath(nestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        // Search by vessel to vessel transfer target lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "vessVessXfer", "targetVessel" ));
        criteriaPath.setPropertyName("label");
        criteriaPath.setNestedCriteriaPath(nestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        // Search by vessel to vessel transfer source lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "vessVessXfer", "sourceVessel" ));
        criteriaPath.setPropertyName("label");
        criteriaPath.setNestedCriteriaPath(nestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
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
                    if( OrmUtil.proxySafeIsInstance(vessel, TubeFormation.class)) {
                        TubeFormation tubes = OrmUtil.proxySafeCast(vessel, TubeFormation.class);
                        LabVessel rack = null;
                        if( labEvent.getSectionTransfers().iterator().hasNext() ) {
                            rack = labEvent.getSectionTransfers().iterator().next().getAncillarySourceVessel();
                        }
                        if( rack != null ) {
                            results.add(rack.getLabel());
                        } else {
                            // Ancillary vessel logic was added around Aug 2014.  This handles any earlier cases
                            for ( LabVessel oldLogicRack : tubes.getRacksOfTubes()) {
                                results.add(oldLogicRack.getLabel());
                            }
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
                    if( OrmUtil.proxySafeIsInstance( inPlaceLabVessel, TubeFormation.class )) {
                        getLabelFromTubeFormation( labEvent, inPlaceLabVessel, results );
                    } else {
                        results.add( inPlaceLabVessel.getLabel() );
                    }
                    return results;
                }

                for (LabVessel vessel : labEvent.getTargetLabVessels()) {
                    if( OrmUtil.proxySafeIsInstance( vessel, TubeFormation.class )) {
                        getLabelFromTubeFormation( labEvent, vessel, results );
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

            /**
             * Shared barcode logic for in place and section transfer events targeting tube formations
             * @param labEvent
             * @param vessel
             * @param results
             */
            private void getLabelFromTubeFormation( LabEvent labEvent, LabVessel vessel, List<String> results ){
                TubeFormation tubes = OrmUtil.proxySafeCast(vessel, TubeFormation.class);
                LabVessel rack = null;
                if( labEvent.getSectionTransfers().iterator().hasNext() ) {
                    rack = labEvent.getSectionTransfers().iterator().next().getAncillaryTargetVessel();
                }
                if( rack != null ) {
                    results.add(rack.getLabel());
                } else {
                    // Ancillary vessel logic was added around Aug 2014.  This handles any earlier cases
                    for ( LabVessel oldLogicRack : tubes.getRacksOfTubes()) {
                        results.add(oldLogicRack.getLabel());
                    }
                }
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
            vesselTypeName = SearchDefinitionFactory.findVesselType( vessel );
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
            vesselTypeName = SearchDefinitionFactory.findVesselType( vessel );
        }

        return vesselTypeName;
    }
}
