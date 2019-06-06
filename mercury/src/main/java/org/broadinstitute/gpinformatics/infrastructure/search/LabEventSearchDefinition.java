package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.DisplayExpression;
import org.broadinstitute.gpinformatics.infrastructure.columns.EventVesselSourcePositionPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.EventVesselTargetPositionPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.SampleDataFetcherAddRowsListener;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds ConfigurableSearchDefinition for lab event user defined search logic
 */
@SuppressWarnings("ReuseOfLocalVariable")
public class LabEventSearchDefinition {

    /**
     * This alternate definition handles querying for events by their lab vessels
     * and expanding the list of events by optional ancestor and/or descendant traversal. <br />
     * Shared by terms in multiple groups (batch and vessel)
     */
    private final ConfigurableSearchDefinition eventByVesselSearchDefinition;

    // These search term and/or result column names need to be referenced multiple places during processing.
    // Use an enum rather than having to reference via String values of term names
    // TODO: JMS Create a shared interface that this implements then use this as a registry of all term names
    public enum MultiRefTerm {
        LCSET("Lab Batch");

        MultiRefTerm(String termRefName ) {
            this.termRefName = termRefName;
            if( termNameReference.put(termRefName, this) != null ) {
                throw new RuntimeException( "Attempt to add a term with a duplicate name [" + termRefName + "]." );
            }
        }

        private String termRefName;
        private Map<String, MultiRefTerm> termNameReference = new HashMap<>();

        public String getTermRefName() {
            return termRefName;
        }

        public boolean isNamed(String termName ) {
            return termRefName.equals(termName);
        }
    }

    public LabEventSearchDefinition(){
        eventByVesselSearchDefinition = buildAlternateSearchDefByVessel();
    }

    /**
     * Available user selectable traversal options for lab event search
     * (as opposed to non-optional alternate search definitions attached to certain terms)
     * Terms with alternate search definitions have to access user selected state of these options.
     */
    public enum TraversalEvaluatorName {
        ANCESTORS("ancestorOptionEnabled"),
        DESCENDANTS("descendantOptionEnabled");

        private final String id;

        TraversalEvaluatorName(String id ) {
            this.id = id;
        }

        public String getId(){
            return id;
        }
    }

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

        searchTerms = LabVesselSearchDefinition.buildBsp();
        mapGroupSearchTerms.put("BSP", searchTerms);

        searchTerms = buildEventSampleOptions( sourceLayoutTerm, destinationLayoutTerm );
        mapGroupSearchTerms.put("Sample Metadata", searchTerms);

        searchTerms = buildEventMetadata();
        mapGroupSearchTerms.put("Event Metadata", searchTerms);

        searchTerms = buildLabEventNestedTables();
        searchTerms.add(sourceLayoutTerm);
        searchTerms.add(destinationLayoutTerm);
        mapGroupSearchTerms.put("Nested Data", searchTerms);

        List<ConfigurableSearchDefinition.CriteriaProjection> criteriaProjections = new ArrayList<>();

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("reagent", "labEventId",
                "labEventReagents", LabEvent.class));

        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                ColumnEntity.LAB_EVENT, criteriaProjections, mapGroupSearchTerms);

        // Allow user to search ancestor and/or descendant events
        // Note:  Terms with alternate search definitions ignore these evaluators and contain logic
        //    to access user selected state of these options.
        configurableSearchDefinition.addTraversalEvaluator(TraversalEvaluatorName.ANCESTORS.getId()
                , new LabEventTraversalEvaluator.AncestorTraversalEvaluator());
        configurableSearchDefinition.addTraversalEvaluator(TraversalEvaluatorName.DESCENDANTS.getId()
                , new LabEventTraversalEvaluator.DescendantTraversalEvaluator());

        configurableSearchDefinition.setAddRowsListenerFactory(
                new ConfigurableSearchDefinition.AddRowsListenerFactory() {
                    @Override
                    public Map<String, ConfigurableList.AddRowsListener> getAddRowsListeners() {
                        Map<String, ConfigurableList.AddRowsListener> listeners = new HashMap<>();
                        listeners.put(SampleDataFetcherAddRowsListener.class.getSimpleName(), new SampleDataFetcherAddRowsListener());
                        return listeners;
                    }
                });

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
        searchTerm.setIsDefaultResultColumn(Boolean.TRUE);
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
        searchTerm.setIsDefaultResultColumn(Boolean.TRUE);
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
                if( labEvent.getWorkflowQualifier() != null ) {
                    return labEvent.getLabEventType().getName() + " (" + labEvent.getWorkflowQualifier() + ")";
                } else {
                    return labEvent.getLabEventType().getName();
                }
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

        searchTerm = new SearchTerm();
        searchTerm.setName("SRS Location");
        searchTerm.setHelpText("Storage location for a check-in/check-out event");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabEvent labEvent = (LabEvent) entity;
                StorageLocation storageLocation = labEvent.getStorageLocation();
                if( storageLocation != null ) {
                    return storageLocation.buildLocationTrail();
                } else {
                    return "";
                }
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
        criteriaPath.setCriteria(Arrays.asList("reagent", "labEventReagents", "reagent"));
        criteriaPath.setPropertyName("name");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);

        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Reagent Lot");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("reagent", "labEventReagents", "reagent"));
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
        criteriaPath.setCriteria(Arrays.asList("reagent", "labEventReagents", "reagent"));
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
        // todo jmt rename to Root Tube Barcode?
        searchTerm.setName("Sample Tube Barcode");
        searchTerm.setHelpText("Value(s) will appear in both the result row and source and/or destination layout positions.");
        searchTerm.setDisplayExpression(DisplayExpression.ROOT_TUBE_BARCODE);
        searchTerms.add(searchTerm);

        for (Metadata.Key meta : Metadata.Key.values()) {
            if (meta.getCategory() == Metadata.Category.SAMPLE) {
                searchTerm = new SearchTerm();
                searchTerm.setName(meta.getDisplayName());
                searchTerm.setHelpText("Value(s) will appear in both the result row and source and/or destination layout positions.");
                searchTerm.setDisplayExpression(DisplayExpression.METADATA);
                searchTerms.add(searchTerm);
            }
        }

        return searchTerms;
    }

    /**
     * Event metadata values.
     *
     * @return List of search terms/column definitions for lab event metadata
     */
    private List<SearchTerm> buildEventMetadata() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        for (LabEventMetadata.LabEventMetadataType labEventMetadataType : LabEventMetadata.LabEventMetadataType.values()) {
            SearchTerm searchTerm = new SearchTerm();
            searchTerm.setName(labEventMetadataType.getDisplayName());
            searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
                @Override
                public Set<String> evaluate(Object entity, SearchContext context) {
                    LabEvent labEvent = (LabEvent) entity;
                    Set<String> results = new HashSet<>();
                    for (LabEventMetadata labEventMetadata : labEvent.getLabEventMetadatas()) {
                        if (labEventMetadata.getLabEventMetadataType().getDisplayName().equals(searchTerm.getName())) {
                            if (labEventMetadata.getLabEventMetadataType() ==
                                    LabEventMetadata.LabEventMetadataType.SimulationMode) {
                                results.add(Boolean.valueOf(labEventMetadata.getValue()) ? "Yes" : "No");
                            } else {
                                results.add(labEventMetadata.getValue());
                            }
                            break;
                        }
                    }

                    return results;
                }
            });
            searchTerms.add(searchTerm);
        }

        return searchTerms;
    }

    private List<SearchTerm> buildLabEventBatch( SearchTerm... nestedTableTerms ) {
        List<SearchTerm> searchTerms = new ArrayList<>();

        // Need a non-functional criteria path to make terms with alternate definitions visible in selection list
        List<SearchTerm.CriteriaPath> blankCriteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath blankCriteriaPath = new SearchTerm.CriteriaPath();
        blankCriteriaPath.setCriteria(new ArrayList<String>());
        blankCriteriaPaths.add(blankCriteriaPath);


        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("PDO");
        searchTerm.setHelpText(
                "PDO term will only locate events associated with bucket entries (e.g. CollaboratorTransfer and PicoPlatingBucket) and PDO sample vessels. "
                + "Traversal option(s) should be selected if chain of custody events are desired.<br>"
                + "Note: The PDO term is exclusive, no other terms can be selected.");
        searchTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getPdoInputConverter());
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                LabEvent labEvent = (LabEvent) entity;
                Set<String> productNames = new HashSet<>();

                Set<LabVessel> eventVessels = labEvent.getTargetLabVessels();
                if( labEvent.getInPlaceLabVessel() != null ) {
                    eventVessels.add(labEvent.getInPlaceLabVessel());
                }

                for( LabVessel labVessel : eventVessels ) {
                    for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                        for (ProductOrderSample productOrderSample : sampleInstanceV2.getAllProductOrderSamples() ) {
                            productNames.add(productOrderSample.getProductOrder().getBusinessKey());
                        }
                    }
                }
                return productNames;
            }
        });
        searchTerm.setAlternateSearchDefinition(eventByVesselSearchDefinition);
        searchTerm.setCriteriaPaths(blankCriteriaPaths);
        searchTerms.add(searchTerm);

        // Product
        searchTerm = new SearchTerm();
        searchTerm.setName("Product");
        searchTerm.setDisplayExpression(DisplayExpression.PRODUCT_NAME);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.LCSET.getTermRefName());
        searchTerm.setHelpText(
                "Lab Batch term will only locate events associated with batch or rework batch vessels.<br>"
                + "Traversal option(s) should be selected if chain of custody events are desired.<br>"
                + "Note: The Lab Batch term is exclusive, no other terms can be selected.");
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
        searchTerm.setAlternateSearchDefinition(eventByVesselSearchDefinition);
        searchTerm.setCriteriaPaths(blankCriteriaPaths);

        SearchTerm lcsetEventTerm = new SearchTerm();
        lcsetEventTerm.setName("Lab batch event type");
        lcsetEventTerm.setConstrainedValuesExpression(new SearchDefinitionFactory.EventTypeValuesExpression());
        lcsetEventTerm.setTraversalFilterExpression(new SearchTerm.Evaluator<Boolean>() {
            @Override
            public Boolean evaluate(Object entity, SearchContext context) {
                for (SearchInstance.SearchValue searchValue : context.getSearchInstance().getSearchValues()) {
                    if (searchValue.getSearchTerm().getName().equals(MultiRefTerm.LCSET.getTermRefName())) {
                        for (String eventTypeName : searchValue.getChildren().iterator().next().getValues()) {
                            if (eventTypeName.equals(((LabEvent)entity).getLabEventType().name())) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            }
        });
        List<SearchTerm> dependentSearchTerms = new ArrayList<>();
        dependentSearchTerms.add(lcsetEventTerm);
        searchTerm.setDependentSearchTerms(dependentSearchTerms);

        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Mercury Sample ID");
        searchTerm.setHelpText(
                "Mercury Sample ID term searches for events that involve vessels with a sample barcode.<br>"
                + "Traversal option(s) should be selected if chain of custody events are desired.<br>"
                + "Note: This term is exclusive, no other terms can be selected.");
        searchTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerm.setAlternateSearchDefinition(eventByVesselSearchDefinition);
        searchTerm.setCriteriaPaths(blankCriteriaPaths);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Nearest Sample ID");
        searchTerm.setDisplayExpression(DisplayExpression.NEAREST_SAMPLE_ID);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Root Sample ID");
        searchTerm.setDisplayExpression(DisplayExpression.ROOT_SAMPLE_ID);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Molecular Index");
        searchTerm.setDisplayExpression(DisplayExpression.MOLECULAR_INDEX);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Unique Molecular Identifier");
        searchTerm.setDisplayExpression(DisplayExpression.UNIQUE_MOLECULAR_IDENTIFIER);
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    /**
     * @return
     */
    private List<SearchTerm> buildLabEventVessel() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Event Vessel Barcode");
        // Do not show in output - redundant with source/target layouts
        searchTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerm.setRackScanSupported(Boolean.TRUE);
        searchTerm.setAlternateSearchDefinition(eventByVesselSearchDefinition);
        // Need a non-functional criteria path to make terms with alternate definitions visible in selection list
        List<SearchTerm.CriteriaPath> blankCriteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath blankCriteriaPath = new SearchTerm.CriteriaPath();
        blankCriteriaPath.setCriteria(new ArrayList<String>());
        blankCriteriaPaths.add(blankCriteriaPath);
        searchTerm.setCriteriaPaths(blankCriteriaPaths);
        searchTerms.add(searchTerm);

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
                        return results;
                    }
                }

                for (LabVessel vessel : labEvent.getTargetLabVessels()) {
                    if( OrmUtil.proxySafeIsInstance( vessel, TubeFormation.class )) {
                        getLabelFromTubeFormation( labEvent, vessel, results );
                    } else {
                        results.add(vessel.getLabel());
                    }
                }
                if( results.isEmpty() && inPlaceLabVessel != null ) {
                    if( inPlaceLabVessel.getContainerRole() != null ) {
                        results.add( inPlaceLabVessel.getContainerRole().getEmbedder().getLabel() );
                    } else {
                        results.add( inPlaceLabVessel.getLabel() );
                    }
                }

                return results;
            }
        });
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    /**
     * Build an alternate search definition to query for lab vessels
     *    and use programmatic logic to populate the lab event list
     * These terms are mapped to user selectable terms by name.
     * @return
     */
    private ConfigurableSearchDefinition buildAlternateSearchDefByVessel() {
        Map<String, List<SearchTerm>> mapGroupSearchTerms = new LinkedHashMap<>();
        List<SearchTerm> searchTerms = new ArrayList<>();

        // By PDO
        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("PDO");

        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(
                Arrays.asList("mercurySample", "mercurySamples", "productOrderSamples", "productOrder"));
        criteriaPath.setPropertyName("jiraTicketKey");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        // By lab batch
        searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.LCSET.getTermRefName());

        criteriaPaths = new ArrayList<>();

        // Mercury only cares about workflow batches
        SearchTerm.ImmutableTermFilter workflowOnlyFilter = new SearchTerm.ImmutableTermFilter(
                "labBatchType", SearchInstance.Operator.EQUALS, LabBatch.LabBatchType.WORKFLOW);

        // Non-reworks
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList(/* LabVessel */ "labBatches", /* LabBatchStartingVessel */
                "labBatch" /* LabBatch */));
        criteriaPath.setPropertyName("batchName");
        criteriaPath.addImmutableTermFilter(workflowOnlyFilter);
        criteriaPaths.add(criteriaPath);

        // Reworks
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList(/* LabVessel */ "reworkLabBatches", /* LabBatch */ "reworkLabBatches"));
        criteriaPath.setPropertyName("batchName");
        criteriaPath.addImmutableTermFilter(workflowOnlyFilter);
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        // By MercurySample
        searchTerm = new SearchTerm();
        searchTerm.setName("Mercury Sample ID");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("mercurySample", "mercurySamples"));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        // In-place vessel barcode
        searchTerm = new SearchTerm();
        searchTerm.setName("In-Place Vessel Barcode");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("label");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        // Vessel barcode
        searchTerm = new SearchTerm();
        searchTerm.setName("Event Vessel Barcode");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("label");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        List<ConfigurableSearchDefinition.CriteriaProjection> criteriaProjections = new ArrayList<>();

        // These access LabVessels
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("labBatches", "labVesselId",
                "labVessel", LabBatchStartingVessel.class));

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("reworkLabBatches", "labVesselId",
                "reworkLabBatches", LabVessel.class));

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("mercurySample", "labVesselId",
                "mercurySamples", LabVessel.class));

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("bucketEntries", "labVesselId",
                "labVessel", BucketEntry.class));

        mapGroupSearchTerms.put("Never Seen", searchTerms);

        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                ColumnEntity.LAB_VESSEL, criteriaProjections, mapGroupSearchTerms);

        // Mandatory to convert a list of LabVessel entities to LabEvent entities
        configurableSearchDefinition.addTraversalEvaluator(ConfigurableSearchDefinition.ALTERNATE_DEFINITION_ID
                , new LabEventVesselTraversalEvaluator() );

        return configurableSearchDefinition;
    }

    /**
     * Shared barcode logic for in place and section transfer events targeting tube formations
     * @param labEvent
     * @param vessel
     * @param results
     */
    private void getLabelFromTubeFormation( LabEvent labEvent, LabVessel vessel, List<String> results ){
        TubeFormation tubes = OrmUtil.proxySafeCast(vessel, TubeFormation.class);
        Set<LabVessel> racks = new HashSet<>();
        if( labEvent.getAncillaryInPlaceVessel() != null ) {
            racks.add( labEvent.getAncillaryInPlaceVessel() );
        } else if( labEvent.getSectionTransfers().iterator().hasNext() ) {
            racks.add( labEvent.getSectionTransfers().iterator().next().getAncillaryTargetVessel() );
        } else if ( labEvent.getCherryPickTransfers().iterator().hasNext() ) {
            // Possibly more than one in cherry picks
            for(CherryPickTransfer xfer : labEvent.getCherryPickTransfers() ) {
                racks.add(xfer.getAncillaryTargetVessel());
            }
        } else if ( labEvent.getVesselToSectionTransfers().iterator().hasNext() ) {
            racks.add( labEvent.getVesselToSectionTransfers().iterator().next().getAncillaryTargetVessel() );
        }
        if( !racks.isEmpty() ) {
            for( LabVessel rack : racks ) {
                results.add(rack.getLabel());
            }
        } else {
            // Ancillary vessel logic was added to transfers around Aug 2014, in-place mid Jan 2019.
            // This handles any earlier cases
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
