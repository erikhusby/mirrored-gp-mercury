package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.broadinstitute.gpinformatics.infrastructure.columns.EventVesselSourcePositionPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.EventVesselTargetPositionPlugin;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToVesselTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

        // Need references to source and destination nested table search terms to add parent terms to handle
        SearchTerm sourceLayoutTerm = new SearchTerm();
        sourceLayoutTerm.setName("Source Layout");
        sourceLayoutTerm.setIsNestedParent(Boolean.TRUE);
        sourceLayoutTerm.setPluginClass(EventVesselSourcePositionPlugin.class);

        SearchTerm destinationLayoutTerm = new SearchTerm();
        destinationLayoutTerm.setName("Destination Layout");
        destinationLayoutTerm.setIsNestedParent(Boolean.TRUE);
        destinationLayoutTerm.setPluginClass(EventVesselTargetPositionPlugin.class);

        List<SearchTerm> searchTerms = buildLabEventBatch(sourceLayoutTerm, destinationLayoutTerm);
        mapGroupSearchTerms.put("Lab Batch", searchTerms);

        searchTerms = buildLabEventIds();
        mapGroupSearchTerms.put("IDs", searchTerms);

        searchTerms = buildLabEventVessel();
        mapGroupSearchTerms.put("Lab Vessel", searchTerms);

        searchTerms = buildLabEventReagents();
        mapGroupSearchTerms.put("Reagents", searchTerms);

        searchTerms = buildEventSampleOptions( sourceLayoutTerm, destinationLayoutTerm );
        mapGroupSearchTerms.put("Sample Metadata", searchTerms);

        searchTerms = buildLabEventNestedTables();
        searchTerms.add(sourceLayoutTerm);
        searchTerms.add(destinationLayoutTerm);
        mapGroupSearchTerms.put("Nested Data", searchTerms);

        List<ConfigurableSearchDefinition.CriteriaProjection> criteriaProjections = new ArrayList<>();

        // This only works for 'In-Place Vessel Barcode' search term
        // 'Event Vessel Barcode' search term produces a large OR clause of event ids,
        //    the addition of an in place vessel id blows up Oracle CBO (badly)
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

        // Used in a nested subquery to pick the vessel containers out of a lab vessels
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("vesselContainer", "container.labVesselId",
                "containers", "container", LabVessel.class));

        // The parent projection for labEventId subqueries required to provide labEventId clauses for all criteria paths
        // (See Oracle CBO notes elsewhere)
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("eventById", "labEventId",
                "labEventId", LabEvent.class));

        // Used in a nested subquery to pick the inPlaceLabEvents out of lab vessels
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
        searchTerm.setDbSortPath("labEventId");
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("labEventId");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Long evaluate(Object entity, SearchContext context) {
                LabEvent labEvent = (LabEvent) entity;
                return labEvent.getLabEventId();
            }
        });
        searchTerm.setValueType( ColumnValueType.UNSIGNED);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("EventDate");
        searchTerm.setDbSortPath("eventDate");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("eventDate");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Date evaluate(Object entity, SearchContext context) {
                LabEvent labEvent = (LabEvent) entity;
                return labEvent.getEventDate();
            }
        });
        searchTerm.setValueType( ColumnValueType.DATE_TIME);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("EventLocation");
        searchTerm.setDbSortPath("eventLocation");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("eventLocation");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabEvent labEvent = (LabEvent) entity;
                return labEvent.getEventLocation();
            }
        });
        searchTerm.setConstrainedValuesExpression(new SearchTerm.Evaluator<List<ConstrainedValue>>() {
            @Override
            public List<ConstrainedValue> evaluate(Object entity, SearchContext context) {
                ConstrainedValueDao constrainedValueDao = context.getOptionValueDao();
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
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                BSPUserList bspUserList = context.getBspUserList();
                LabEvent labEvent = (LabEvent) entity;
                Long userId = labEvent.getEventOperator();
                BspUser bspUser = bspUserList.getById(userId);
                if (bspUser == null) {
                    return "Unknown user - ID: " + userId;
                }

                return bspUser.getFullName();
            }
        });
        searchTerm.setConstrainedValuesExpression(new SearchTerm.Evaluator<List<ConstrainedValue>>() {
            // Pick actual users out of lab events
            @Override
            public List<ConstrainedValue> evaluate(Object entity, SearchContext context) {
                ConstrainedValueDao constrainedValueDao = context.getOptionValueDao();
                return constrainedValueDao.getLabEventUserNameList();
            }
        });
        searchTerm.setSearchValueConversionExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Long evaluate(Object entity, SearchContext context) {
                return Long.valueOf( context.getSearchValueString());
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("EventType");
        searchTerm.setDbSortPath("labEventType");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("labEventType");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabEvent labEvent = (LabEvent) entity;
                return labEvent.getLabEventType().getName();
            }
        });
        searchTerm.setConstrainedValuesExpression(new SearchDefinitionFactory.EventTypeValuesExpression());
        searchTerm.setSearchValueConversionExpression( new SearchDefinitionFactory.EventTypeValueConversionExpression() );
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Program Name");
        searchTerm.setDbSortPath("programName");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("programName");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabEvent labEvent = (LabEvent) entity;
                String programName = labEvent.getProgramName();
                return (programName == null ? "" : programName);

            }
        });
        searchTerm.setConstrainedValuesExpression(new SearchTerm.Evaluator<List<ConstrainedValue>>() {
            @Override
            public List<ConstrainedValue> evaluate(Object entity, SearchContext context) {
                ConstrainedValueDao constrainedValueDao = context.getOptionValueDao();
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
        parentSearchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Collection<Reagent> evaluate(Object entity, SearchContext context) {
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
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
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
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
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
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Date evaluate(Object entity, SearchContext context) {
                Reagent reagent = (Reagent) entity;
                return reagent.getExpiration();
            }
        });
        searchTerm.setValueType(ColumnValueType.DATE);
        parentSearchTerm.addNestedEntityColumn(searchTerm);

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
        searchTerm.setDisplayValueExpression( new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
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
        searchTerm.setDisplayValueExpression( new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
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
        searchTerm.setValueType( ColumnValueType.DATE );
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("reagent", "reagents"));
        criteriaPath.setPropertyName("expiration");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression( new SearchTerm.Evaluator<Object>() {
            @Override
            public List<Date> evaluate(Object entity, SearchContext context) {
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

    /**
     * Mercury sample metadata values
     * These are displayed in both results row and source and/or destination layouts (if selected)
     *
     * @return List of search terms/column definitions for lab event vessel samples
     */
    private List<SearchTerm> buildEventSampleOptions( SearchTerm... nestedTableTerms ) {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Sample Tube Barcode");
        searchTerm.setHelpText("Value(s) will appear in both the result row and source and/or destination layout positions.");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                // Has to handle LabEvent from parent term and LabVessel from nested table
                LabVessel labVessel = null;
                LabEvent labEvent;

                Set<String> results = new HashSet<>();

                if( OrmUtil.proxySafeIsInstance( entity, LabEvent.class ) ) {
                    labEvent = OrmUtil.proxySafeCast(entity, LabEvent.class);
                    labVessel = labEvent.getInPlaceLabVessel();
                    if (labVessel == null) {
                        for( LabVessel srcVessel : labEvent.getSourceLabVessels() ) {
                            addSampleLabelsFromVessel( srcVessel, results );
                        }
                        return results;
                    }
                } else if( OrmUtil.proxySafeIsInstance( entity, LabVessel.class ) ) {
                    labVessel = OrmUtil.proxySafeCast(entity, LabVessel.class);
                } else {
                    throw new RuntimeException("Unhandled display value type for 'Sample Tube Barcode': "
                                               + OrmUtil.getProxyObjectClass(entity).getSimpleName());
                }

                if (labVessel != null) {
                    addSampleLabelsFromVessel( labVessel, results );
                }

                return results;
            }

            private void addSampleLabelsFromVessel( LabVessel labVessel, Set<String> results ){
                if (labVessel != null) {
                    for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                        if(sampleInstanceV2.getRootOrEarliestMercurySample() != null){
                            for (LabVessel rootSampleVessel : sampleInstanceV2.getRootOrEarliestMercurySample()
                                    .getLabVessel()) {
                                results.add(rootSampleVessel.getLabel());
                            }
                        }
                    }
                }
            }
        });
        // Sample Tube Barcode also handled by source/destination layout nested table cell display
        for( SearchTerm nestedTableTerm : nestedTableTerms ) {
            nestedTableTerm.addParentTermHandledByChild(searchTerm);
        }
        searchTerms.add(searchTerm);

        SearchDefinitionFactory.SampleMetadataDisplayExpression sampleMetadataDisplayExpression = new SearchDefinitionFactory.SampleMetadataDisplayExpression();
        for (Metadata.Key meta : Metadata.Key.values()) {
            if (meta.getCategory() == Metadata.Category.SAMPLE) {
                searchTerm = new SearchTerm();
                searchTerm.setName(meta.getDisplayName());
                searchTerm.setHelpText("Value(s) will appear in both the result row and source and/or destination layout positions.");
                searchTerm.setDisplayValueExpression(sampleMetadataDisplayExpression);
                // These also handled by source/destination layout nested table cell display
                for( SearchTerm nestedTableTerm : nestedTableTerms ) {
                    nestedTableTerm.addParentTermHandledByChild(searchTerm);
                }
                searchTerms.add(searchTerm);
            }
        }

        return searchTerms;
    }

    private List<SearchTerm> buildLabEventBatch( SearchTerm... nestedTableTerms ) {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("PDO");
        searchTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getPdoInputConverter());
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(
                Arrays.asList(/* LabEvent*/ "inPlaceLabEvents", /* LabVessel */ "bucketEntries", /* BucketEntry */
                        "productOrder" /* ProductOrder */));
        criteriaPath.setPropertyName("jiraTicketKey");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                LabEvent labEvent = (LabEvent) entity;
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

                return productNames;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("LCSET");
        searchTerm.setHelpText(
                "The LCSET term will only locate events associated with bucket entries (e.g. CollaboratorTransfer and PicoPlatingBucket). "
                + "Traversal option(s) should be selected if chain of custody events are desired.<br>"
                + "Note: Adding other terms may exclude initial event(s) from the chain of custody traversal(s).");
        searchTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getLcsetInputConverter());
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
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                LabEvent labEvent = (LabEvent) entity;
                Set<String> lcSetNames = new HashSet<>();

                for (LabBatch labBatch : labEvent.getComputedLcSets()) {
                    lcSetNames.add(labBatch.getBatchName());
                }
                return lcSetNames;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Mercury Sample ID");
        searchTerm.setHelpText(
                "This term searches for events that involve vessels with a sample barcode. Use traversal checkboxes to see other events in the chain of custody.");
        for( SearchTerm nestedTableTerm : nestedTableTerms ) {
            nestedTableTerm.addParentTermHandledByChild(searchTerm);
        }
        criteriaPaths = new ArrayList<>();

        // Search by in place lab vessel mercury samples
        // Need a nested criteria path to get a lab event list for a tube barcode
        // (see criteria projection for reason)
        SearchTerm.CriteriaPath inPlaceNestedCriteriaPath = new SearchTerm.CriteriaPath();
        inPlaceNestedCriteriaPath.setCriteria(Arrays.asList("inPlaceLabEventSubQuery", "mercurySamples"));

        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("eventById", "inPlaceLabVesselId"));
        criteriaPath.setNestedCriteriaPath(inPlaceNestedCriteriaPath);
        criteriaPath.setPropertyName("sampleKey");
        criteriaPaths.add(criteriaPath);

        // Search by section transfer target lab vessel
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("sectXfer", "targetVessel", "mercurySamples"));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPaths.add(criteriaPath);

        // Search by section transfer source lab vessel
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("sectXfer", "sourceVessel", "mercurySamples"));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPaths.add(criteriaPath);

        // Search by cherry pick transfer target lab vessel
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "cherryPickXfer", "targetVessel", "mercurySamples" ));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPaths.add(criteriaPath);

        // Search by cherry pick transfer source lab vessel
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("cherryPickXfer", "sourceVessel", "mercurySamples"));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPaths.add(criteriaPath);

        // Search by vessel to section transfer target lab vessel
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "vessSectXfer", "targetVessel", "mercurySamples" ));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPaths.add(criteriaPath);

        // Search by vessel to section transfer source lab vessel
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "vessSectXfer", "sourceVessel", "mercurySamples" ));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPaths.add(criteriaPath);

        // Search by vessel to vessel transfer target lab vessel
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "vessVessXfer", "targetVessel", "mercurySamples" ));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPaths.add(criteriaPath);

        // Search by vessel to vessel transfer source lab vessel
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "vessVessXfer", "sourceVessel", "mercurySamples" ));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPaths.add(criteriaPath);

        // **** Logic to find and use the container of a vessel (TubeFormation) in an event source/target ****
        // Shared nested criteria path to get a vessel container list for a tube barcode
        SearchTerm.CriteriaPath containerNestedCriteriaPath = new SearchTerm.CriteriaPath();
        containerNestedCriteriaPath.setCriteria(Arrays.asList("vesselContainer", "mercurySamples"));

        // Search by in place lab vessel container is double nested:
        //       [event id by in place vessel] <-- [containers by lab vessel]
        // Need a nested criteria path to get a lab event list for a tube barcode
        // (see criteria projection for reason)
        SearchTerm.CriteriaPath inPlaceNestedContainerCriteriaPath = new SearchTerm.CriteriaPath();
        inPlaceNestedContainerCriteriaPath.setCriteria(Arrays.asList( "inPlaceLabEventSubQuery", "inPlaceLabEvents" ) );
        inPlaceNestedContainerCriteriaPath.setNestedCriteriaPath(containerNestedCriteriaPath);

        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("eventById", "inPlaceLabVesselId" ));
        criteriaPath.setNestedCriteriaPath(inPlaceNestedContainerCriteriaPath);
        criteriaPath.setPropertyName("sampleKey");
        criteriaPaths.add(criteriaPath);

        // Search by cherry pick transfer target lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "cherryPickXfer", "targetVessel" ));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPath.setNestedCriteriaPath(containerNestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        // Search by cherry pick transfer source lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "cherryPickXfer", "sourceVessel" ));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPath.setNestedCriteriaPath(containerNestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        // Search by section transfer source lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "sectXfer", "sourceVessel" ));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPath.setNestedCriteriaPath(containerNestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        // Search by section transfer target lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "sectXfer", "targetVessel" ));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPath.setNestedCriteriaPath(containerNestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        // Search by vessel to section transfer target lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "vessSectXfer", "targetVessel" ));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPath.setNestedCriteriaPath(containerNestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        // Search by vessel to section transfer source lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "vessSectXfer", "sourceVessel" ));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPath.setNestedCriteriaPath(containerNestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        // Search by vessel to vessel transfer target lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "vessVessXfer", "targetVessel" ));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPath.setNestedCriteriaPath(containerNestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        // Search by vessel to vessel transfer source lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "vessVessXfer", "sourceVessel" ));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPath.setNestedCriteriaPath(containerNestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                Set<String> results = new HashSet<>();
                LabVessel labVessel = null;
                // Has to handle LabEvent from parent term, LabVessel from nested table,
                //  and sample data from nested table.
                if( OrmUtil.proxySafeIsInstance( entity, LabEvent.class ) ) {
                    LabEvent labEvent = OrmUtil.proxySafeCast(entity, LabEvent.class);
                    labVessel = labEvent.getInPlaceLabVessel();
                    if (labVessel == null) {
                        for( LabVessel srcVessel : labEvent.getSourceLabVessels() ) {
                            for( SampleInstanceV2 sample : srcVessel.getSampleInstancesV2()) {
                                results.add(sample.getRootOrEarliestMercurySampleName());
                            }
                        }
                        return results;
                    }
                } else if( OrmUtil.proxySafeIsInstance( entity, MercurySample.class ) ) {
                        MercurySample mercurySample = OrmUtil.proxySafeCast(entity, MercurySample.class);
                        results.add(mercurySample.getSampleKey());
                        return results;
                } else if( OrmUtil.proxySafeIsInstance( entity, LabVessel.class ) ) {
                    labVessel = OrmUtil.proxySafeCast(entity, LabVessel.class);
                } else {
                    throw new RuntimeException("Unhandled display value type for 'Mercury Sample ID': "
                                               + OrmUtil.getProxyObjectClass(entity).getSimpleName());
                }
                // Shared for event in place vessel and lab vessel entity logic
                if( labVessel != null ) {
                    for( SampleInstanceV2 sample : labVessel.getSampleInstancesV2()) {
                        results.add(sample.getRootOrEarliestMercurySampleName());
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

        // Shared nested criteria path to get a vessel container list for a tube barcode
        SearchTerm.CriteriaPath containerNestedCriteriaPath = new SearchTerm.CriteriaPath();
        containerNestedCriteriaPath.setCriteria(Arrays.asList( "vesselContainer" ) );

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("In-Place Vessel Barcode");
        searchTerm.setDbSortPath("inPlaceLabVessel.label");
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();

        // In place vessel barcode
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("inPlaceLabEvents"));
        criteriaPath.setPropertyName("label");
        criteriaPaths.add(criteriaPath);

        // In place vessel barcode in a container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("inPlaceLabEvents"));
        criteriaPath.setNestedCriteriaPath(containerNestedCriteriaPath);
        criteriaPath.setPropertyName("label");
        criteriaPaths.add(criteriaPath);

        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                LabEvent labEvent = (LabEvent) entity;
                List<String> results = new ArrayList<>();
                LabVessel inPlaceLabVessel = labEvent.getInPlaceLabVessel();
                if (inPlaceLabVessel == null) {
                    return results;
                } else {
                    if( OrmUtil.proxySafeIsInstance( inPlaceLabVessel, TubeFormation.class )) {
                        getLabelFromTubeFormation( labEvent, inPlaceLabVessel, results );
                    } else {
                        results.add( inPlaceLabVessel.getLabel() );
                    }
                }
                return results;
            }
        });
        searchTerms.add(searchTerm);


        // ***********************************************************
        // ************** Any vessel barcode in an event *************
        searchTerm = new SearchTerm();
        searchTerm.setName("Event Vessel Barcode");
        // Do not show in output - redundant with source/target layouts
        searchTerm.setIsExcludedFromResultColumns( Boolean.TRUE);
        criteriaPaths = new ArrayList<>();

        // Search by in place lab vessel
        // Need a nested criteria path to get a lab event list for a tube barcode
        // (see criteria projection for reason)
        SearchTerm.CriteriaPath inPlaceNestedCriteriaPath = new SearchTerm.CriteriaPath();
        inPlaceNestedCriteriaPath.setCriteria(Arrays.asList( "inPlaceLabEventSubQuery" ) );

        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("eventById", "inPlaceLabVesselId" ));
        criteriaPath.setNestedCriteriaPath(inPlaceNestedCriteriaPath);
        criteriaPath.setPropertyName("label");
        criteriaPaths.add(criteriaPath);

        // Search by cherry pick transfer target lab vessel
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "cherryPickXfer", "targetVessel" ));
        criteriaPath.setPropertyName("label");
        criteriaPaths.add(criteriaPath);

        // Search by cherry pick transfer source lab vessel
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("cherryPickXfer", "sourceVessel"));
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

        // **** Logic to find and use the container of a vessel (TubeFormation) in an event source/target ****

        // Search by in place lab vessel container is double nested:
        //       [event id by in place vessel] <-- [containers by lab vessel]
        // Need a nested criteria path to get a lab event list for a tube barcode
        // (see criteria projection for reason)
        SearchTerm.CriteriaPath inPlaceNestedContainerCriteriaPath = new SearchTerm.CriteriaPath();
        inPlaceNestedContainerCriteriaPath.setCriteria(Arrays.asList( "inPlaceLabEventSubQuery", "inPlaceLabEvents" ) );
        inPlaceNestedContainerCriteriaPath.setNestedCriteriaPath(containerNestedCriteriaPath);

        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("eventById", "inPlaceLabVesselId" ));
        criteriaPath.setNestedCriteriaPath(inPlaceNestedContainerCriteriaPath);
        criteriaPath.setPropertyName("label");
        criteriaPaths.add(criteriaPath);

        // Search by cherry pick transfer target lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "cherryPickXfer", "targetVessel" ));
        criteriaPath.setPropertyName("label");
        criteriaPath.setNestedCriteriaPath(containerNestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        // Search by cherry pick transfer source lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "cherryPickXfer", "sourceVessel" ));
        criteriaPath.setPropertyName("label");
        criteriaPath.setNestedCriteriaPath(containerNestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        // Search by section transfer source lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "sectXfer", "sourceVessel" ));
        criteriaPath.setPropertyName("label");
        criteriaPath.setNestedCriteriaPath(containerNestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        // Search by section transfer target lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "sectXfer", "targetVessel" ));
        criteriaPath.setPropertyName("label");
        criteriaPath.setNestedCriteriaPath(containerNestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        // Search by vessel to section transfer target lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "vessSectXfer", "targetVessel" ));
        criteriaPath.setPropertyName("label");
        criteriaPath.setNestedCriteriaPath(containerNestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        // Search by vessel to section transfer source lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "vessSectXfer", "sourceVessel" ));
        criteriaPath.setPropertyName("label");
        criteriaPath.setNestedCriteriaPath(containerNestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        // Search by vessel to vessel transfer target lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "vessVessXfer", "targetVessel" ));
        criteriaPath.setPropertyName("label");
        criteriaPath.setNestedCriteriaPath(containerNestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        // Search by vessel to vessel transfer source lab vessel container
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList( "vessVessXfer", "sourceVessel" ));
        criteriaPath.setPropertyName("label");
        criteriaPath.setNestedCriteriaPath(containerNestedCriteriaPath);
        criteriaPaths.add(criteriaPath);

        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        // **** End of any vessel barcode in an event search term ****
        // ***********************************************************

        searchTerm = new SearchTerm();
        searchTerm.setName("Source Lab Vessel Type");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                return findEventSourceContainerType((LabEvent) entity);
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Source Barcode");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
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
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                return findEventTargetContainerType( (LabEvent) entity );
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Destination Barcode");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
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

        });
        searchTerms.add(searchTerm);

        return searchTerms;
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
        } else if ( labEvent.getCherryPickTransfers().iterator().hasNext() ) {
            rack = labEvent.getCherryPickTransfers().iterator().next().getAncillaryTargetVessel();
        } else if ( labEvent.getVesselToSectionTransfers().iterator().hasNext() ) {
            rack = labEvent.getVesselToSectionTransfers().iterator().next().getAncillaryTargetVessel();
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
