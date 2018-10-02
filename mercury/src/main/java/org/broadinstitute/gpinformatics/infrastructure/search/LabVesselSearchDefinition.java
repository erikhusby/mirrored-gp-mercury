package org.broadinstitute.gpinformatics.infrastructure.search;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.SearchInstanceNameCache;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.DisplayExpression;
import org.broadinstitute.gpinformatics.infrastructure.columns.LabVesselArrayMetricPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.LabVesselLatestEventPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.LabVesselLatestPositionPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.LabVesselMetadataPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.LabVesselMetricPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.SampleDataFetcherAddRowsListener;
import org.broadinstitute.gpinformatics.infrastructure.columns.VesselLayoutPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.VesselMetricDetailsPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.VolumeHistoryAddRowsListener;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.AbandonVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
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
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Builds ConfigurableSearchDefinition for lab vessel user defined search logic
 */
@SuppressWarnings("ReuseOfLocalVariable")
public class LabVesselSearchDefinition {

    private static final List<LabEventType> POND_LAB_EVENT_TYPES = Arrays.asList(LabEventType.POND_REGISTRATION,
            LabEventType.PCR_FREE_POND_REGISTRATION, LabEventType.PCR_PLUS_POND_REGISTRATION);

    public static final List<LabEventType> CHIP_EVENT_TYPES = Collections.singletonList(
            LabEventType.INFINIUM_HYBRIDIZATION);

    public static final List<LabEventType> FLOWCELL_LAB_EVENT_TYPES = new ArrayList<>();
    static {
        FLOWCELL_LAB_EVENT_TYPES.add(LabEventType.FLOWCELL_TRANSFER);
        FLOWCELL_LAB_EVENT_TYPES.add(LabEventType.DENATURE_TO_FLOWCELL_TRANSFER);
        FLOWCELL_LAB_EVENT_TYPES.add(LabEventType.DILUTION_TO_FLOWCELL_TRANSFER);
        FLOWCELL_LAB_EVENT_TYPES.add(LabEventType.REAGENT_KIT_TO_FLOWCELL_TRANSFER);
    }

    // These search term and/or result column names need to be referenced multiple places during processing.
    // Use an enum rather than having to reference via String values of term names
    // TODO: JMS Create a shared interface that this implements then use this as a registry of all term names
    public enum MultiRefTerm {
        INFINIUM_DNA_PLATE("DNA Array Plate Barcode"),
        INFINIUM_AMP_PLATE("Amp Plate Barcode"),
        INFINIUM_CHIP("Infinium Chip Barcode"),
        INITIAL_VOLUME("Initial Volume"),
        VOLUME_HISTORY("Volume History");

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

        LabVesselSearchDefinition srchDef = new LabVesselSearchDefinition();
        Map<String, List<SearchTerm>> mapGroupSearchTerms = new LinkedHashMap<>();
        // One or more option groups have mouseover popup help text
        Map<String,String> mapGroupHelpText = new HashMap<>();

        SearchTerm layoutTerm = new SearchTerm();
        layoutTerm.setName("Layout");
        layoutTerm.setIsNestedParent(Boolean.TRUE);
        layoutTerm.setPluginClass(VesselLayoutPlugin.class);

        List<SearchTerm> searchTerms = srchDef.buildLabVesselIds();
        mapGroupSearchTerms.put("IDs", searchTerms);

        searchTerms = srchDef.buildLabVesselBatchTypes();
        mapGroupSearchTerms.put("Lab Batch by Type", searchTerms);

        // Are there alternatives to search terms that aren't searchable?  Should they be in a different structure, then merged with search terms for display?

        // XX version - from workflow? 3.2 doesn't seem to be in XML
        // Start date - LabBatch.createdOn? usually 1 day before "scheduled to start"
        // Due date - LabBatch.dueDate is transient!
        searchTerms = buildBsp();
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

        searchTerms = buildVesselMetricDetailCols();
        mapGroupSearchTerms.put("Quant Details", searchTerms);
        mapGroupHelpText.put("Quant Details", "This group of columns traverses ancestor and descendant vessels to locate" +
                " all metrics types for the selected column.  Data is displayed in separate column sets for each metric run name oldest to newest from left to right.");

        searchTerms = srchDef.buildArrayTerms();
        mapGroupSearchTerms.put("Arrays", searchTerms);

        searchTerms = srchDef.buildLabVesselMultiCols();
        mapGroupSearchTerms.put("Multi-Columns", searchTerms);

        mapGroupSearchTerms.put("Nested Data", Collections.singletonList(layoutTerm));

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

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection( "sequencingRun", "labVesselId",
                "runCartridge", SequencingRun.class));

        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                ColumnEntity.LAB_VESSEL, criteriaProjections, mapGroupSearchTerms);

        configurableSearchDefinition.addTraversalEvaluator(
                LabEventSearchDefinition.TraversalEvaluatorName.ANCESTORS.getId(),
                new LabVesselTraversalEvaluator.AncestorTraversalEvaluator());
        configurableSearchDefinition.addTraversalEvaluator(
                LabEventSearchDefinition.TraversalEvaluatorName.DESCENDANTS.getId(),
                new LabVesselTraversalEvaluator.DescendantTraversalEvaluator());

        // Configure custom traversal evaluators
        configurableSearchDefinition.addCustomTraversalOption( InfiniumVesselTraversalEvaluator.DNA_PLATE_INSTANCE );
        configurableSearchDefinition.addCustomTraversalOption( InfiniumVesselTraversalEvaluator.DNA_PLATEWELL_INSTANCE );
        configurableSearchDefinition.addCustomTraversalOption( new TubeStripTubeFlowcellTraversalEvaluator() );
        configurableSearchDefinition.addCustomTraversalOption( new VesselByEventTypeTraversalEvaluator() );

        configurableSearchDefinition.setAddRowsListenerFactory(
                new ConfigurableSearchDefinition.AddRowsListenerFactory() {
                    @Override
                    public Map<String, ConfigurableList.AddRowsListener> getAddRowsListeners() {
                        Map<String, ConfigurableList.AddRowsListener> listeners = new HashMap<>();
                        listeners.put(SampleDataFetcherAddRowsListener.class.getSimpleName(), new SampleDataFetcherAddRowsListener());
                        listeners.put(VolumeHistoryAddRowsListener.class.getSimpleName(), new VolumeHistoryAddRowsListener());
                        return listeners;
                    }
                });

        // Add user popup note to an entire option group
        configurableSearchDefinition.addColumnGroupHelpText(mapGroupHelpText);

        return configurableSearchDefinition;
    }

    private List<SearchTerm> buildLabVesselIds() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Barcode");
        searchTerm.setIsDefaultResultColumn(Boolean.TRUE);
        searchTerm.setRackScanSupported(Boolean.TRUE);
        searchTerm.setDbSortPath("label");
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
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
        searchTerm.setName("Plate Name");
        searchTerm.setDbSortPath("name");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("name");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                if(labVessel.getType() == LabVessel.ContainerType.PLATE_WELL) {
                    labVessel = ((PlateWell)labVessel).getPlate();
                }
                return labVessel==null?"":labVessel.getName();
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

        searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.INITIAL_VOLUME.getTermRefName());
        searchTerm.setValueType(ColumnValueType.STRING);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                String value = "";
                LabVessel labVessel = (LabVessel) entity;
                VolumeHistoryAddRowsListener volumeHistoryAddRowsListener = (VolumeHistoryAddRowsListener)
                        context.getRowsListener( VolumeHistoryAddRowsListener.class.getSimpleName() );
                Triple<Date, BigDecimal, String> initialVolumeData = volumeHistoryAddRowsListener.getInitialVolumeData(labVessel.getLabel());
                if( initialVolumeData != null ) {
                    value = ColumnValueType.TWO_PLACE_DECIMAL.format(initialVolumeData.getMiddle(), "")
                            + " - " + ColumnValueType.DATE.format(initialVolumeData.getLeft(), "")
                            + " - " + initialVolumeData.getRight() ;
                }
                return value;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.VOLUME_HISTORY.getTermRefName());
        searchTerm.setValueType(ColumnValueType.STRING);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                String value = "";
                LabVessel labVessel = (LabVessel) entity;
                VolumeHistoryAddRowsListener volumeHistoryAddRowsListener = (VolumeHistoryAddRowsListener)
                        context.getRowsListener( VolumeHistoryAddRowsListener.class.getSimpleName() );
                Collection<Triple<Date, BigDecimal, String>> volumeHistoryData = volumeHistoryAddRowsListener.getVolumeHistoryData(labVessel.getLabel());
                if( volumeHistoryData != null && volumeHistoryData.size() > 0 ) {
                    StringBuilder valueBuilder = new StringBuilder();
                    for( Triple<Date, BigDecimal, String> initialVolumeData : volumeHistoryData ) {
                        valueBuilder.append(ColumnValueType.TWO_PLACE_DECIMAL.format(initialVolumeData.getMiddle() , ""))
                                .append(" - ")
                                .append(  ColumnValueType.DATE_TIME.format(initialVolumeData.getLeft(), "") )
                                .append(" - ")
                                .append(initialVolumeData.getRight())
                        .append(", ");
                    }
                    value = valueBuilder.substring(0, valueBuilder.length() - 2);
                }
                return value;

            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Starting Barcode");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                return (String) context.getPagination().getIdExtraInfo().get(labVessel.getLabel());
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Vessel Drill Downs");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                String label = ((LabVessel) entity).getLabel();

                ResultParamValues columnParams = context.getColumnParams();
                if( columnParams == null ) {
                    return "(Params required)";
                }

                String drillDownString = null;
                for(ResultParamValues.ParamValue value : columnParams.getParamValues() ) {
                    if (value.getName().equals("drillDown")) {
                        drillDownString = value.getValue();
                        break;
                    }
                }

                if( drillDownString == null ) {
                    return "(No drill down selected)";
                }

                SearchInstanceNameCache.DrillDownOption drillDownOption = SearchInstanceNameCache.DrillDownOption.buildFromString(drillDownString);

                Map<String,String[]> terms = new HashMap<>();
                String[] values = {label};
                terms.put(drillDownOption.getSearchTermName(), values);

                return SearchDefinitionFactory.buildDrillDownLink("", drillDownOption.getTargetEntity(), drillDownOption.getPreferenceScope().name() + "|" + drillDownOption.getPreferenceName() + "|" + drillDownOption.getSearchName(), terms, context);
            }
        });
        searchTerm.setResultParamConfigurationExpression(
            new SearchTerm.Evaluator<ResultParamConfiguration>() {

                @Override
                public ResultParamConfiguration evaluate(Object entity, SearchContext context) {
                    ResultParamConfiguration drillDownConfiguration = new ResultParamConfiguration();
                    ResultParamConfiguration.ParamInput searchNameInput =
                            new ResultParamConfiguration.ParamInput("drillDown", ResultParamConfiguration.InputType.PICKLIST, "Select drill down search:");
                    List<ConstrainedValue> options = new ArrayList<>();
                    SearchInstanceNameCache instanceNameCache = ServiceAccessUtility.getBean(SearchInstanceNameCache.class);
                    for( SearchInstanceNameCache.DrillDownOption drillDown : instanceNameCache.getDrillDowns(ColumnEntity.LAB_VESSEL.getEntityName()) ) {
                        options.add(new ConstrainedValue(drillDown.toString(), drillDown.getSearchName() ));
                    }
                    if( options.size() == 0 ) {
                        options.add(new ConstrainedValue("", "(None Available)" ));
                    }
                    searchNameInput.setOptionItems(options);
                    drillDownConfiguration.addParamInput(searchNameInput);

                    return drillDownConfiguration;
                }
            }
        );
        searchTerm.setHelpText("Creates a column with a link to an existing search which functions as a drill-down.  <br/>Note: Selected search MUST have a single term which expects a barcode value.");
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    private List<SearchTerm> buildLabVesselBatchTypes() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        // LCSET and XTR batches are filtered by workflow batches
        SearchTerm.ImmutableTermFilter workflowOnlyFilter = new SearchTerm.ImmutableTermFilter(
                "labBatchType", SearchInstance.Operator.EQUALS, LabBatch.LabBatchType.WORKFLOW);
        // LCSET batches are filtered by name prefix = LCSET-
        SearchTerm.ImmutableTermFilter lscetBatchFilter = new SearchTerm.ImmutableTermFilter(
                "batchName", SearchInstance.Operator.LIKE, "LCSET-%");
        // ARRAY batches are filtered by name prefix = ARRAY-
        SearchTerm.ImmutableTermFilter arrayBatchFilter = new SearchTerm.ImmutableTermFilter(
                "batchName", SearchInstance.Operator.LIKE, "ARRAY-%");
        // XTR batches are filtered by name prefix = XTR-
        SearchTerm.ImmutableTermFilter xtrBatchFilter = new SearchTerm.ImmutableTermFilter(
                "batchName", SearchInstance.Operator.LIKE, "XTR-%");
        // FCT batches are filtered by type
        SearchTerm.ImmutableTermFilter fctOnlyFilter = new SearchTerm.ImmutableTermFilter(
                "labBatchType", SearchInstance.Operator.IN, LabBatch.LabBatchType.FCT, LabBatch.LabBatchType.MISEQ);
        // SK (Sample Kit) batches are filtered by type
        SearchTerm.ImmutableTermFilter skOnlyFilter = new SearchTerm.ImmutableTermFilter(
                "labBatchType", SearchInstance.Operator.EQUALS, LabBatch.LabBatchType.SAMPLES_RECEIPT);


        // todo jmt look at search inputs, to show only LCSET that was searched on?
        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("LCSET");
        searchTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getBatchNameInputConverter());
        searchTerm.setDisplayExpression(DisplayExpression.LCSET);

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
        searchTerm.setName("ARRAY");
        searchTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getBatchNameInputConverter());
        searchTerm.setDisplayExpression(DisplayExpression.ARRAY);

        criteriaPaths = new ArrayList<>();
        // Non-reworks (arrays don't get reworked)
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("labBatches", "labBatch"));
        criteriaPath.setPropertyName("batchName");
        criteriaPath.addImmutableTermFilter(workflowOnlyFilter);
        criteriaPath.addImmutableTermFilter(arrayBatchFilter);
        criteriaPath.setJoinFetch(Boolean.TRUE);
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("XTR");
        searchTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getBatchNameInputConverter());
        searchTerm.setDisplayExpression(DisplayExpression.XTR);

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
        searchTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getBatchNameInputConverter());
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
        searchTerm.setName("SK");
        searchTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getBatchNameInputConverter());
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                Set<String> results = null;
                LabVessel labVessel = (LabVessel) entity;

                // Only look for BSP sample kit as an ancestor of a mercury lab vessel
                VesselBatchTraverserCriteria downstreamBatchFinder = new VesselBatchTraverserCriteria();
                if( labVessel.getContainerRole() != null ) {
                    labVessel.getContainerRole().applyCriteriaToAllPositions(
                            downstreamBatchFinder, TransferTraverserCriteria.TraversalDirection.Ancestors);
                } else {
                    labVessel.evaluateCriteria(
                            downstreamBatchFinder, TransferTraverserCriteria.TraversalDirection.Ancestors);
                }

                for ( LabBatch labBatch : downstreamBatchFinder.getLabBatches() ) {
                    if( labBatch.getLabBatchType() == LabBatch.LabBatchType.SAMPLES_RECEIPT ) {
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
        criteriaPath.addImmutableTermFilter(skOnlyFilter);
        criteriaPath.setJoinFetch(Boolean.TRUE);
        criteriaPaths.add(criteriaPath);
        // No SK reworks, another ticket is created
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    // todo jmt rename
    static List<SearchTerm> buildBsp() {
        List<SearchTerm> searchTerms = new ArrayList<>();
        // Non-searchable data from BSP
        {
            SearchTerm searchTerm = buildLabVesselBspTerm(BSPSampleSearchColumn.STOCK_SAMPLE, "Stock Sample ID");
            searchTerm.setDisplayExpression(DisplayExpression.STOCK_SAMPLE);
            searchTerms.add(searchTerm);
        }
        {
            SearchTerm searchTerm = buildLabVesselBspTerm(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID);
            searchTerm.setDisplayExpression(DisplayExpression.COLLABORATOR_SAMPLE_ID);
            searchTerms.add(searchTerm);
        }
        {
            SearchTerm searchTerm = buildLabVesselBspTerm(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID);
            searchTerm.setDisplayExpression(DisplayExpression.COLLABORATOR_PARTICIPANT_ID);
            searchTerms.add(searchTerm);
        }
        {
            SearchTerm searchTerm = buildLabVesselBspTerm(BSPSampleSearchColumn.SAMPLE_TYPE, "Tumor / Normal");
            searchTerm.setDisplayExpression(DisplayExpression.SAMPLE_TYPE);
            searchTerms.add(searchTerm);
        }
        {
            SearchTerm searchTerm = buildLabVesselBspTerm(BSPSampleSearchColumn.COLLECTION);
            searchTerm.setDisplayExpression(DisplayExpression.COLLECTION);
            searchTerms.add(searchTerm);
        }
        {
            SearchTerm searchTerm = buildLabVesselBspTerm(BSPSampleSearchColumn.ORIGINAL_MATERIAL_TYPE);
            searchTerm.setDisplayExpression(DisplayExpression.ORIGINAL_MATERIAL_TYPE);
            searchTerms.add(searchTerm);
        }
        return searchTerms;
    }

    /**
     * Builds BSP term with default display name
     */
    private static SearchTerm buildLabVesselBspTerm(BSPSampleSearchColumn bspSampleSearchColumn) {
        return buildLabVesselBspTerm(bspSampleSearchColumn, bspSampleSearchColumn.columnName());
    }

    /**
     * Builds BSP term with user specified display name
     */
    private static SearchTerm buildLabVesselBspTerm(final BSPSampleSearchColumn bspSampleSearchColumn, String name) {
        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName(name);
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
        searchTerm.setDisplayExpression(DisplayExpression.PDO);
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
        searchTerm.setDisplayExpression(DisplayExpression.RESEARCH_PROJECT);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Regulatory Designation");
        searchTerm.setDisplayExpression(DisplayExpression.REGULATORY_DESIGNATION);
        searchTerms.add(searchTerm);

        // Product
        searchTerm = new SearchTerm();
        searchTerm.setName("Product");
        searchTerm.setDisplayExpression(DisplayExpression.PRODUCT_NAME);
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

        /****  Result Criteria ****/
        searchTerm = new SearchTerm();
        searchTerm.setName("Event Vessel Barcodes");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                return VesselEventResultParamConfiguration.getDisplayValue(
                        entity, context, VesselEventResultParamConfiguration.ResultEventData.BARCODE);
            }
        });
        searchTerm.setResultParamConfigurationExpression(new SearchTerm.Evaluator<ResultParamConfiguration>() {
            @Override
            public ResultParamConfiguration evaluate(Object entity, SearchContext context) {
                return new VesselEventResultParamConfiguration();
            }
        });
        searchTerm.setHelpText("Traverses all transfers from a vessel and finds barcodes at all user selected event types.");
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Event Vessel Positions");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                return VesselEventResultParamConfiguration.getDisplayValue(
                        entity, context, VesselEventResultParamConfiguration.ResultEventData.POSITION);
            }
        });
        searchTerm.setResultParamConfigurationExpression(new SearchTerm.Evaluator<ResultParamConfiguration>() {
            @Override
            public ResultParamConfiguration evaluate(Object entity, SearchContext context) {
                return new VesselEventResultParamConfiguration();
            }
        });
        searchTerm.setHelpText("Traverses all transfers from a vessel and finds container positions at all user selected event types.");
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Event Vessel Date");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                return VesselEventResultParamConfiguration.getDisplayValue(
                        entity, context, VesselEventResultParamConfiguration.ResultEventData.DATE);
            }
        });
        searchTerm.setResultParamConfigurationExpression(new SearchTerm.Evaluator<ResultParamConfiguration>() {
            @Override
            public ResultParamConfiguration evaluate(Object entity, SearchContext context) {
                return new VesselEventResultParamConfiguration();
            }
        });
        searchTerm.setHelpText("Traverses all transfers from a vessel and finds dates of all user selected event types.");
        searchTerms.add(searchTerm);
        /****  Result Criteria ****/

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

                VesselsForEventTraverserCriteria eval = new VesselsForEventTraverserCriteria(POND_LAB_EVENT_TYPES);
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

                VesselsForEventTraverserCriteria eval = new VesselsForEventTraverserCriteria(POND_LAB_EVENT_TYPES);
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
        searchTerm.setName("Norm Pond Tube Barcode");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;

                VesselsForEventTraverserCriteria eval = new VesselsForEventTraverserCriteria(Collections.singletonList(
                        LabEventType.PCR_PLUS_POND_NORMALIZATION));
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

                Set<String> barcodes = null;
                for(Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                        : eval.getPositions().asMap().entrySet()) {
                    (barcodes == null ? barcodes = new HashSet<>() : barcodes)
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

                // Handle the case where the flowcell itself is returned (e.g. flowcell barcode as search criteria)
                if( OrmUtil.proxySafeIsInstance(labVessel, RunCartridge.class) ) {
                    barcodes = new HashSet<>();
                    barcodes.add(labVessel.getLabel());
                    return barcodes;
                }

                VesselsForEventTraverserCriteria eval = new VesselsForEventTraverserCriteria(FLOWCELL_LAB_EVENT_TYPES);

                if( labVessel.getContainerRole() != null ) {
                    labVessel.getContainerRole().applyCriteriaToAllPositions(eval,
                            TransferTraverserCriteria.TraversalDirection.Descendants);
                } else {
                    labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);
                }

                for(Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                        : eval.getPositions().asMap().entrySet()) {
                    (barcodes==null?barcodes=new HashSet<>():barcodes)
                            .add(labVesselAndPositions.getKey().getLabel());
                }
                return barcodes;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Number of Lanes");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Integer evaluate(Object entity, SearchContext context) {

                LabVessel labVessel = (LabVessel) entity;
                // Handle the case where the vessel itself is a flowcell
                if( OrmUtil.proxySafeIsInstance(labVessel, RunCartridge.class) ) {
                    return labVessel.getContainerRole().getPositions().size();
                }
                VesselsForEventTraverserCriteria eval = new VesselsForEventTraverserCriteria(FLOWCELL_LAB_EVENT_TYPES);

                if( labVessel.getContainerRole() != null ) {
                    labVessel.getContainerRole().applyCriteriaToAllPositions(eval,
                            TransferTraverserCriteria.TraversalDirection.Descendants);
                } else {
                    labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);
                }

                return eval.getPositions().values().size();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Sequencing Run Name");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Collections.singletonList("sequencingRun"));
        criteriaPath.setPropertyName("runName");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {

                LabVessel labVessel = (LabVessel) entity;
                Set<String> seqRunNames = null;

                // Handle the case where the flowcell itself is returned (e.g. flowcell barcode as search criteria)
                if( OrmUtil.proxySafeIsInstance(labVessel, RunCartridge.class) ) {
                    RunCartridge flowCell = OrmUtil.proxySafeCast(labVessel, RunCartridge.class);
                    for(SequencingRun run : flowCell.getSequencingRuns()) {
                        (seqRunNames==null?seqRunNames=new HashSet<>():seqRunNames)
                                .add(run.getRunName());
                    }
                    return seqRunNames;
                }

                VesselsForEventTraverserCriteria eval = new VesselsForEventTraverserCriteria(FLOWCELL_LAB_EVENT_TYPES);

                if( labVessel.getContainerRole() != null ) {
                    labVessel.getContainerRole().applyCriteriaToAllPositions(eval, TransferTraverserCriteria.TraversalDirection.Descendants);
                } else {
                    labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);
                }

                for(LabVessel flowCellVessel : eval.getPositions().keySet()) {
                    if( OrmUtil.proxySafeIsInstance(flowCellVessel, RunCartridge.class) ) {
                        RunCartridge flowCell = OrmUtil.proxySafeCast(flowCellVessel, RunCartridge.class);
                        for(SequencingRun run : flowCell.getSequencingRuns()) {
                            (seqRunNames==null?seqRunNames=new HashSet<>():seqRunNames)
                                    .add(run.getRunName());
                        }
                    }
                }
                return seqRunNames;
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
        searchTerm.setName("EmergeVolumeTransfer Rack Barcode");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                List<String> results = new ArrayList<>();

                Map<LabEvent, Set<LabVessel>> mapEventToVessels = labVessel.findVesselsForLabEventType(
                        LabEventType.EMERGE_VOLUME_TRANSFER, true,
                        EnumSet.of(TransferTraverserCriteria.TraversalDirection.Ancestors,
                                TransferTraverserCriteria.TraversalDirection.Descendants));
                for (Map.Entry<LabEvent, Set<LabVessel>> labEventSetEntry : mapEventToVessels.entrySet()) {
                    for (SectionTransfer sectionTransfer : labEventSetEntry.getKey().getSectionTransfers()) {
                        if (sectionTransfer.getAncillaryTargetVessel() != null) {
                            results.add(sectionTransfer.getAncillaryTargetVessel().getLabel());
                        }
                    }
                }
                return results;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("EmergeVolumeTransfer Rack Position");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                List<String> results = new ArrayList<>();

                Map<LabEvent, Set<LabVessel>> mapEventToVessels = labVessel.findVesselsForLabEventType(
                        LabEventType.EMERGE_VOLUME_TRANSFER, true,
                        EnumSet.of(TransferTraverserCriteria.TraversalDirection.Ancestors,
                                TransferTraverserCriteria.TraversalDirection.Descendants));
                for (Map.Entry<LabEvent, Set<LabVessel>> labEventSetEntry : mapEventToVessels.entrySet()) {
                    for (SectionTransfer sectionTransfer : labEventSetEntry.getKey().getSectionTransfers()) {
                        for (LabVessel vessel : labEventSetEntry.getValue()) {
                            results.add(sectionTransfer.getTargetVesselContainer().getPositionOfVessel(vessel).toString());
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
            public Set<String> evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                Set<String> results = new HashSet<>();

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
            public Set<String> evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                Set<String> results = new HashSet<>();

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

        searchTerm = new SearchTerm();
        searchTerm.setName("Library Type");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                for (LabEvent labEvent : labVessel.getTransfersTo()) {
                    LabEventType.LibraryType libraryType = labEvent.getLabEventType().getLibraryType();
                    if (libraryType != LabEventType.LibraryType.NONE_ASSIGNED) {
                        return libraryType.getMercuryDisplayName();
                    }
                }
                return null;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Transfers To");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                Set<String> events = new HashSet<>();
                LabVessel labVessel = (LabVessel) entity;
                for (LabEvent labEvent : labVessel.getTransfersTo()) {
                    events.add(labEvent.getLabEventType().getName());
                }
                return events;
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
        searchTerm.setDisplayExpression(DisplayExpression.NEAREST_SAMPLE_ID);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Root Sample ID");
        searchTerm.setDisplayExpression(DisplayExpression.ROOT_SAMPLE_ID);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Sample Count");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Integer evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                return labVessel.getSampleInstancesV2().size();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Molecular Index");
        searchTerm.setDisplayExpression(DisplayExpression.MOLECULAR_INDEX);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Unique Molecular Identifier");
        searchTerm.setDisplayExpression(DisplayExpression.UNIQUE_MOLECULAR_IDENTIFIER);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Bait/CAT Name");
        searchTerm.setDisplayExpression(DisplayExpression.BAIT_OR_CAT_NAME);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Mercury Sample Tube Barcode");
        // todo jmt replace?
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
        for (Metadata.Key meta : Metadata.Key.values()) {
            if (meta.getCategory() == Metadata.Category.SAMPLE) {
                searchTerm = new SearchTerm();
                searchTerm.setName(meta.getDisplayName());
                searchTerm.setDisplayExpression(DisplayExpression.METADATA);
                searchTerms.add(searchTerm);
            }
        }

        searchTerm = new SearchTerm();
        searchTerm.setName("Abandon Reason");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                LabVessel currentVessel = (LabVessel) entity;
                Set<String> reasons = null;
                TransferTraverserCriteria.AbandonedLabVesselCriteria abandonCriteria =
                            new TransferTraverserCriteria.AbandonedLabVesselCriteria(false);

                if (currentVessel.getContainerRole() == null) {
                    currentVessel.evaluateCriteria(abandonCriteria,
                            TransferTraverserCriteria.TraversalDirection.Ancestors);
                } else {
                    currentVessel.getContainerRole().applyCriteriaToAllPositions(abandonCriteria,
                            TransferTraverserCriteria.TraversalDirection.Ancestors);
                }

                // Bail out if nothing to do
                if (! abandonCriteria.isAncestorAbandoned()) {
                    return reasons;
                }

                reasons = new HashSet<>();
                String reason;
                MultiValuedMap<LabVessel, AbandonVessel> abandonMap = abandonCriteria.getAncestorAbandonVessels();

                // Display each reason with a group of positions (if container) in parentheses
                if( abandonMap.size() == 1 && abandonMap.mapIterator().next().getAbandonVessels().size() == 1 ) {
                    // Only one abandon - could be a tube so show position only if available
                    AbandonVessel abandonVessel = abandonMap.mapIterator().next().getAbandonVessels().iterator().next();
                    reason = abandonVessel.getReason().getDisplayName();
                    if( abandonVessel.getVesselPosition() != null ) {
                        reason += "(" + abandonVessel.getVesselPosition().name() + ")";
                    }
                    reasons.add( reason );
                } else {
                    // Gather reasons and positions
                    // TODO JMS Display can get ugly when ancestor abandons/positions are mixed in with current vessel
                    String position;
                    Map<String,Set<String>> reasonPositionMap = new HashMap<>();

                    for( AbandonVessel abandonVessel : abandonMap.values() ) {
                        reason = abandonVessel.getReason().getDisplayName();
                        if( !reasonPositionMap.containsKey( reason ) ) {
                            reasonPositionMap.put(reason, new TreeSet<String>());
                        }
                        position = abandonVessel.getVesselPosition() == null?"":abandonVessel.getVesselPosition().name();
                        reasonPositionMap.get(reason).add( position );
                    }
                    for( String key : reasonPositionMap.keySet() ) {
                        StringBuilder posDisplay = new StringBuilder(64);
                        for( String pos : reasonPositionMap.get(key)) {
                            if( pos.length() > 0 ) {
                                posDisplay.append(pos).append(",");
                            }
                        }
                        if( posDisplay.length() > 1 ) {
                            posDisplay.deleteCharAt(posDisplay.length() - 1);
                            posDisplay.append(")");
                            posDisplay.insert(0,"(");
                            posDisplay.insert(0,key );
                            reasons.add( posDisplay.toString() );
                        }
                    }
                }
                return reasons;
            }
        });
        searchTerm.setHelpText("These abandon terms only looks backwards in transfers and gather any past vessel abandons, otherwise results would be ambiguous for reworks looking forward.  Infinium related logic requires using 'Infinium Array Metrics' term.");
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Abandon Date");
        searchTerm.setValueType(ColumnValueType.DATE);

        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<Date> evaluate(Object entity, SearchContext context) {
                LabVessel currentVessel = (LabVessel) entity;
                Set<Date> dateVal = null;
                TransferTraverserCriteria.AbandonedLabVesselCriteria abandonCriteria =
                        new TransferTraverserCriteria.AbandonedLabVesselCriteria(false);

                if( currentVessel.getContainerRole() == null ) {
                    currentVessel.evaluateCriteria(abandonCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors);
                } else {
                    currentVessel.getContainerRole().applyCriteriaToAllPositions(abandonCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors);
                }
                if( abandonCriteria.isAncestorAbandoned() ) {
                    dateVal = new HashSet<>();
                    MultiValuedMap<LabVessel,AbandonVessel> abandonMap = abandonCriteria.getAncestorAbandonVessels();
                    for( LabVessel labVessel : abandonMap.keySet() ) {
                        for( AbandonVessel abandonVessel : abandonMap.get(labVessel)) {
                            dateVal.add(abandonVessel.getAbandonedOn());
                        }
                    }
                }
                return dateVal;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Storage Location");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>(){
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = (LabVessel) entity;
                if (labVessel.getStorageLocation() != null) {
                    // If Barcoded Tube, attempt to find its container by grabbing most recent Storage Check-in event.
                    if (OrmUtil.proxySafeIsInstance(labVessel, BarcodedTube.class)) {
                        SortedMap<Date, TubeFormation> sortedMap = new TreeMap<>();
                        for (LabVessel container : labVessel.getContainers()) {
                            if (OrmUtil.proxySafeIsInstance(container, TubeFormation.class)) {
                                TubeFormation tubeFormation = OrmUtil.proxySafeCast(
                                        container, TubeFormation.class);
                                for (LabEvent labEvent : tubeFormation.getInPlaceLabEvents()) {
                                    if (labEvent.getLabEventType() == LabEventType.STORAGE_CHECK_IN) {
                                        sortedMap.put(labEvent.getEventDate(), tubeFormation);
                                    }
                                }
                            }
                        }
                        if (!sortedMap.isEmpty()) {
                            TubeFormation tubeFormation = sortedMap.get(sortedMap.lastKey());
                            for (RackOfTubes rackOfTubes : tubeFormation.getRacksOfTubes()) {
                                if (rackOfTubes.getStorageLocation() != null) {
                                    if (rackOfTubes.getStorageLocation().equals(labVessel.getStorageLocation())) {
                                        VesselContainer<BarcodedTube> containerRole = tubeFormation.getContainerRole();
                                        for (Map.Entry<VesselPosition, BarcodedTube> entry:
                                                containerRole.getMapPositionToVessel().entrySet()) {
                                            BarcodedTube value = entry.getValue();
                                            if (value != null && value.getLabel().equals(labVessel.getLabel())) {
                                                String locationTrail = rackOfTubes.getStorageLocation().buildLocationTrail();
                                                locationTrail = locationTrail + ": [" +
                                                                rackOfTubes.getLabel() + "] : " +
                                                                entry.getKey().name();
                                                return locationTrail;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // On Failure at least return storage location w/o container
                        return labVessel.getStorageLocation().buildLocationTrail();
                    } else {
                        String location = labVessel.getStorageLocation().buildLocationTrail();
                        location += " [" + labVessel.getLabel() + "]";
                        return location;
                    }
                }
                return null;
            }
        });
        searchTerm.setUiDisplayOutputExpression(new SearchTerm.Evaluator<String>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                if (entity != null && entity instanceof String) {
                    String str = (String) entity;
                    if (str.contains("[")) {
                        String containerBarcode = str.substring(str.indexOf("[")+1,str.indexOf("]"));
                        String href = String.format(
                                "/Mercury/container/container.action?containerBarcode=%s&viewContainerSearch=",
                                containerBarcode
                        );
                        return String
                                .format("<a class=\"external\" target=\"new\" href=\"%s\">%s</a>", href, str);
                    } else {
                        return str;
                    }
                }
                return null;
            }
        });
        searchTerms.add(searchTerm);

        return searchTerms;

        // todo jmt break some of these "metadata" terms into their own group
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

        // Criteria paths all use label
        List<SearchTerm.CriteriaPath> labelCriteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath labelCriteriaPath = new SearchTerm.CriteriaPath();
        labelCriteriaPath.setPropertyName("label");
        labelCriteriaPaths.add(labelCriteriaPath);

        final List<LabEventType> ampPlateEventTypes
                = Collections.singletonList(LabEventType.INFINIUM_AMPLIFICATION);

        searchTerm = new SearchTerm();
        searchTerm.setName("DNA Plate Well");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabVessel vessel = (LabVessel)entity;

                // Ignore for all but Infinium DNA Plate wells as source vessel
                if(!InfiniumVesselTraversalEvaluator.isInfiniumSearch(context) || vessel.getType() != LabVessel.ContainerType.PLATE_WELL) {
                    return null;
                }

                return vessel.getContainers().iterator().next().getContainerRole().getPositionOfVessel(vessel).toString();
            }
        });

        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.INFINIUM_DNA_PLATE.getTermRefName());
        searchTerm.setCriteriaPaths(labelCriteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                return InfiniumVesselTraversalEvaluator.getInfiniumDnaPlateBarcode( (LabVessel)entity, context );
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Amp Plate Well");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                String result = null;
                LabVessel vessel = (LabVessel)entity;

                // Ignore for all but Infinium DNA Plate wells as source vessel
                if(!InfiniumVesselTraversalEvaluator.isInfiniumSearch(context) || vessel.getType() != LabVessel.ContainerType.PLATE_WELL) {
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
        searchTerm.setCriteriaPaths(labelCriteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                return InfiniumVesselTraversalEvaluator.getInfiniumAmpPlateBarcode((LabVessel)entity, context);
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Chip Well Barcode");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                String result = null;
                LabVessel vessel = (LabVessel)entity;

                // Ignore for all but Infinium DNA Plate wells as source vessel
                if(!InfiniumVesselTraversalEvaluator.isInfiniumSearch(context) || vessel.getType() != LabVessel.ContainerType.PLATE_WELL) {
                    return null;
                }

                // DNA plate well event/vessel looks to (latest if rehyb-ed) descendant for chip well (1:1)
                for (Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                        : InfiniumVesselTraversalEvaluator.getChipDetailsForDnaWell(vessel, CHIP_EVENT_TYPES, context).asMap().entrySet()) {
                    result = labVesselAndPositions.getKey().getLabel() + "_" + labVesselAndPositions.getValue().iterator().next().toString();
                    break;
                }

                return result;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Chip Well");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                String result = null;
                LabVessel vessel = (LabVessel)entity;

                // Ignore for all but Infinium DNA Plate wells as source vessel
                if(!InfiniumVesselTraversalEvaluator.isInfiniumSearch(context) || vessel.getType() != LabVessel.ContainerType.PLATE_WELL) {
                    return null;
                }

                // DNA plate well event/vessel looks to (latest if rehyb-ed) descendant for chip well (1:1)
                for (Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                        : InfiniumVesselTraversalEvaluator.getChipDetailsForDnaWell(vessel, CHIP_EVENT_TYPES, context).asMap().entrySet()) {
                    result = labVesselAndPositions.getValue().iterator().next().toString();
                    break;
                }

                return result;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName(MultiRefTerm.INFINIUM_CHIP.getTermRefName());
        searchTerm.setCriteriaPaths(labelCriteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                Set<String> result = null;
                LabVessel vessel = (LabVessel)entity;

                if( SearchDefinitionFactory.findVesselType(vessel).startsWith("Infinium") ) {
                    (result == null?result = new HashSet<>():result).add(vessel.getLabel());
                } else if( vessel.getType() == LabVessel.ContainerType.PLATE_WELL ) {
                    // Plate well will only show latest chip in the case of a re-hyb
                    for (Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                            : InfiniumVesselTraversalEvaluator.getChipDetailsForDnaWell(vessel, CHIP_EVENT_TYPES, context ).asMap().entrySet()) {
                        (result == null?result = new HashSet<>():result).add(labVesselAndPositions.getKey().getLabel());
                        break;
                    }
                } else {
                    // Plate shows list of all chips, initial and re-hyb
                    for (Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                            : InfiniumVesselTraversalEvaluator.getChipDetailsForDnaPlate(vessel, CHIP_EVENT_TYPES, context ).asMap().entrySet()) {
                        (result == null?result = new HashSet<>():result).add(labVesselAndPositions.getKey().getLabel());
                    }
                }
                return result;
            }
        });
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
                return InfiniumVesselTraversalEvaluator.getInfiniumDnaPlateBarcode( (LabVessel)entity, context );

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
                return InfiniumVesselTraversalEvaluator.getInfiniumAmpPlateBarcode((LabVessel)entity, context);
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
            public List<String> evaluate(Object entity, SearchContext context) {
                List<String> result = null;
                LabVessel vessel = (LabVessel)entity;

                Map<LabVessel, Collection<VesselPosition>> vesselCollectionMap;

                if( SearchDefinitionFactory.findVesselType(vessel).startsWith("Infinium") ) {
                    vesselCollectionMap = new HashMap<>();
                    vesselCollectionMap.put(vessel,Collections.EMPTY_LIST);
                } else if( vessel.getType() == LabVessel.ContainerType.PLATE_WELL ) {
                    vesselCollectionMap = InfiniumVesselTraversalEvaluator.getChipDetailsForDnaWell(vessel, CHIP_EVENT_TYPES, context ).asMap();
                } else {
                    vesselCollectionMap = InfiniumVesselTraversalEvaluator.getChipDetailsForDnaPlate(vessel, CHIP_EVENT_TYPES, context ).asMap();
                }

                for (Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                        : vesselCollectionMap.entrySet()) {
                    if( result == null ) {
                        result = new ArrayList<>();
                    }
                    result.add(labVesselAndPositions.getKey().getLabel());
                }

                return result;
            }
        });
        searchTerm.setUiDisplayOutputExpression(new SearchTerm.Evaluator<String>() {
            final String drillDownSearchName = "GLOBAL|GLOBAL_LAB_VESSEL_SEARCH_INSTANCES|Infinium Chip Drill Down";
            final String drillDownSearchTerm = MultiRefTerm.INFINIUM_CHIP.getTermRefName();

            @Override
            public String evaluate(Object value, SearchContext context) {
                StringBuilder results = null;
                List<String> barcodes = (List<String>)value;

                if( barcodes == null || barcodes.isEmpty() ) {
                    return "";
                }

                for( String barcode : barcodes ) {
                    Map<String, String[]> terms = new HashMap<>();
                    terms.put(drillDownSearchTerm, new String[]{barcode});
                    if( results == null ) {
                        results = new StringBuilder();
                    }
                    results.append( SearchDefinitionFactory.buildDrillDownLink(barcode, ColumnEntity.LAB_VESSEL, drillDownSearchName, terms, context));
                    results.append(" ");
                }
                return results.toString();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Hyb Chamber");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                Set<String> results = null;
                LabVessel vessel = (LabVessel)entity;

                TransferTraverserCriteria.VesselForEventTypeCriteria traverserCriteria =
                        new TransferTraverserCriteria.VesselForEventTypeCriteria(Collections.singletonList(
                                LabEventType.INFINIUM_HYB_CHAMBER_LOADED), true);

                if( vessel.getType() == LabVessel.ContainerType.PLATE_WELL ) {
                    vessel.evaluateCriteria(traverserCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
                    LabEvent latestEvent = null;
                    for (LabEvent labEvent : traverserCriteria.getVesselsForLabEventType().keySet()) {
                        // Re-hybs based upon a plate well as the row source need to show chamber of latest event
                        if( latestEvent == null ) {
                            latestEvent = labEvent;
                        } else if ( labEvent.getEventDate().after(latestEvent.getEventDate())) {
                            latestEvent = labEvent;
                        }
                    }

                    if( latestEvent != null ) {
                        (results==null?results=new HashSet<>():results).add( latestEvent.getEventLocation() );
                    }
                } else if ( vessel.getType() == LabVessel.ContainerType.STATIC_PLATE ) {
                    vessel.getContainerRole().applyCriteriaToAllPositions(traverserCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
                    for (LabEvent labEvent : traverserCriteria.getVesselsForLabEventType().keySet()) {
                        // Show all chambers downstream from a plate as row source
                        (results==null?results=new HashSet<>():results).add( labEvent.getEventLocation() );
                    }
                }

                return results;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Tecan Position");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                Set<String> results = null;

                if(!InfiniumVesselTraversalEvaluator.isInfiniumSearch(context)) {
                    return results;
                }

                LabVessel vessel = (LabVessel)entity;
                TransferTraverserCriteria.VesselForEventTypeCriteria traverserCriteria =
                        new TransferTraverserCriteria.VesselForEventTypeCriteria(Collections.singletonList(
                                LabEventType.INFINIUM_XSTAIN), true);

                if( vessel.getType() == LabVessel.ContainerType.PLATE_WELL ) {
                    vessel.evaluateCriteria(traverserCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
                    LabEvent latestEvent = null;

                    // Re-hybs based upon a plate well as the row source need to show data from latest event
                    for (LabEvent labEvent : traverserCriteria.getVesselsForLabEventType().keySet()) {
                        if( latestEvent == null ) {
                            latestEvent = labEvent;
                        } else if ( labEvent.getEventDate().after(latestEvent.getEventDate())) {
                            latestEvent = labEvent;
                        }
                    }

                    if( latestEvent != null ) {
                        for (LabEventMetadata labEventMetadata : latestEvent.getLabEventMetadatas()) {
                            if (labEventMetadata.getLabEventMetadataType() ==
                                    LabEventMetadata.LabEventMetadataType.MessageNum) {
                                (results == null ? results = new HashSet<>() : results).add(labEventMetadata.getValue());
                                break;
                            }
                        }
                    }
                } else if ( vessel.getType() == LabVessel.ContainerType.STATIC_PLATE ) {
                    vessel.getContainerRole().applyCriteriaToAllPositions(traverserCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
                    for (LabEvent labEvent : traverserCriteria.getVesselsForLabEventType().keySet()) {
                        // Show all chambers downstream from a plate as row source
                        for (LabEventMetadata labEventMetadata : labEvent.getLabEventMetadatas()) {
                            if (labEventMetadata.getLabEventMetadataType() ==
                                    LabEventMetadata.LabEventMetadataType.MessageNum) {
                                (results == null ? results = new HashSet<>() : results).add(labEventMetadata.getValue());
                                break;
                            }
                        }
                    }
                }

                return results;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Tecan Robot");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                Set<String> results = null;

                if(!InfiniumVesselTraversalEvaluator.isInfiniumSearch(context)) {
                    return results;
                }

                LabVessel vessel = (LabVessel)entity;
                TransferTraverserCriteria.VesselForEventTypeCriteria traverserCriteria =
                        new TransferTraverserCriteria.VesselForEventTypeCriteria(Collections.singletonList(
                                LabEventType.INFINIUM_XSTAIN), true);

                if( vessel.getType() == LabVessel.ContainerType.PLATE_WELL ) {
                    vessel.evaluateCriteria(traverserCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
                    LabEvent latestEvent = null;
                    // Re-hybs based upon a plate well as the row source need to show data from latest event
                    for (LabEvent labEvent : traverserCriteria.getVesselsForLabEventType().keySet()) {
                        if( latestEvent == null ) {
                            latestEvent = labEvent;
                        } else if ( labEvent.getEventDate().after(latestEvent.getEventDate())) {
                            latestEvent = labEvent;
                        }
                    }

                    if( latestEvent != null ) {
                        (results == null ? results = new HashSet<>() : results).add(latestEvent.getEventLocation());
                    }
                } else if ( vessel.getType() == LabVessel.ContainerType.STATIC_PLATE ) {
                    vessel.getContainerRole().applyCriteriaToAllPositions(traverserCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
                    for (LabEvent labEvent : traverserCriteria.getVesselsForLabEventType().keySet()) {
                        // Show all chambers downstream from a plate as row source
                        (results == null ? results = new HashSet<>() : results).add(labEvent.getEventLocation());
                    }
                }

                return results;
            }
        });
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    /**
     * Build search terms to display details about a rack scan term
     * @return List of search terms/column definitions for lab vessel rack scan
     */
    private List<SearchTerm> buildMetricsTerms(){
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Initial Pico Value");
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
        searchTerm.setDisplayExpression(DisplayExpression.PROCEED_IF_OOS);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Flowcell Pool Test Lane(s)");
        searchTerm.setHelpText("Valid only when the row vessel is a flowcell");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                List<String> poolTestLanes;

                LabVessel labVessel = (LabVessel)entity;

                // Quick exit
                if( !OrmUtil.proxySafeIsInstance(labVessel, IlluminaFlowcell.class)) {
                    return null;
                }

                IlluminaFlowcell flowcell = OrmUtil.proxySafeCast(labVessel, IlluminaFlowcell.class);

                VesselBatchTraverserCriteria downstreamBatchFinder = new VesselBatchTraverserCriteria(true);
                flowcell.getContainerRole().applyCriteriaToAllPositions(
                            downstreamBatchFinder, TransferTraverserCriteria.TraversalDirection.Ancestors);

                // Never more than one Flowcell ticket per flowcell
                LabBatch fct = null;

                for ( LabBatch labBatch : downstreamBatchFinder.getLabBatches() ) {
                    if( labBatch.getLabBatchType() == LabBatch.LabBatchType.FCT ) {
                        fct = labBatch;
                        break;
                    }
                }

                if( fct == null ) {
                    return null;
                }

                poolTestLanes = new ArrayList<>();

                for( LabBatchStartingVessel startingVessel : fct.getLabBatchStartingVessels() ) {
                    FlowcellDesignation designation = startingVessel.getFlowcellDesignation();
                    if( designation != null && designation.isPoolTest() ) {
                        poolTestLanes.add( startingVessel.getVesselPosition().toString() );
                    }
                }

                return poolTestLanes;
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
        // todo jmt rename to Latest Descendant Event
        searchTerm.setName("Vessel Latest Event");
        searchTerm.setPluginClass(LabVesselLatestEventPlugin.class);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Most Recent Rack and Event");
        searchTerm.setPluginClass(LabVesselLatestPositionPlugin.class);
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    /**
     * Build multi column search terms for lab vessel metrics details.
     * @return List of search terms/column definitions, one for each LabMetric.MetricType
     */
    private List<SearchTerm> buildVesselMetricDetailCols() {
        List<SearchTerm> searchTerms = new ArrayList<>();
        SearchTerm searchTerm;

        for( LabMetric.MetricType metricType : LabMetric.MetricType.values() ) {
            // Only interested in concentration metrics
            if( metricType.getCategory() != LabMetric.MetricType.Category.CONCENTRATION) {
                continue;
            }
            // Names must be consistent -  column name logic is used in plugin to filter metric types!
            searchTerm = new SearchTerm();
            searchTerm.setName(metricType.getDisplayName());
            searchTerm.setPluginClass(VesselMetricDetailsPlugin.class);
            searchTerms.add(searchTerm);
        }

        return searchTerms;
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

        // Want to be able to find vessel for latest event if more than one event (e.g infinium rehyb chip)
        private boolean captureLatestEventVesselsFlag = false;
        // Want to be able to find all events related to a traversal
        private boolean captureAllEventVesselsFlag = false;
        private Map<LabEvent,LabVessel> eventMap;

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
         * Flags traversal process to return only the vessels associated with the latest event found. <br />
         * <strong>NOTE: This logic will fail (miserably) if traversal is started on a container (e.g. VesselContainer#applyCriteriaToAllPositions)</strong>
         */
        public void captureLatestEventVesselsOnly() {
            this.captureLatestEventVesselsFlag = true;
            eventMap = new TreeMap<>(LabEvent.BY_EVENT_DATE);
        }

        /**
         * Flags traversal process to capture all events. <br />
         */
        public void captureAllEvents() {
            captureAllEventVesselsFlag = true;
            eventMap = new TreeMap<>(LabEvent.BY_EVENT_DATE);
        }

        /**
         * Obtains the outcome of the traversal
         * @return A set of barcode-position pairs.
         * Note:  If the vessel in the event of interest is not in a container, the position value will be null.
         */
        public MultiValuedMap<LabVessel, VesselPosition> getPositions(){
            if( !positions.isEmpty() && captureLatestEventVesselsFlag ) {
                LabVessel lastEventVessel = null;
                for( Map.Entry<LabEvent,LabVessel> entry : eventMap.entrySet() ) {
                    lastEventVessel = entry.getValue();
                }
                for(MapIterator<LabVessel,VesselPosition> iter = positions.mapIterator(); iter.hasNext();) {
                    if( !iter.next().getLabel().equals(lastEventVessel.getLabel()) ) {
                        iter.remove();
                    }
                }
            }
            return positions;
        }

        public Set<LabEvent> getAllEvents() {
            if( captureLatestEventVesselsFlag || captureAllEventVesselsFlag ) {
                return eventMap.keySet();
            } else {
                throw new IllegalStateException("Instance is not flagged to capture events");
            }
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(
                Context context ) {

            // State variable to handle configuration option to stop on first hit
            TraversalControl outcome = TraversalControl.ContinueTraversing;
            boolean catchThisVessel = false;

            // There is no event at traversal starting vessel (hopcount = 0)
            if ( context.getHopCount() > 0 ) {

                // The stop has to happen after we've gathered all the vessels from a pooling type event
                if (context.getHopCount() > previousHopCount && stopTraversingBeforeNextHop) {
                    return TraversalControl.StopTraversing;
                } else {
                    previousHopCount = context.getHopCount();
                }

                LabVessel.VesselEvent eventNode = context.getVesselEvent();

                if (labEventTypes.contains(eventNode.getLabEvent().getLabEventType())) {
                    Map.Entry<LabVessel, VesselPosition> vesselPositionEntry = getTraversalVessel(context);
                    positions.put(vesselPositionEntry.getKey(), vesselPositionEntry.getValue());
                    catchThisVessel = true;

                    if (captureLatestEventVesselsFlag || captureAllEventVesselsFlag) {
                        eventMap.put(eventNode.getLabEvent(), vesselPositionEntry.getKey());
                    }
                }
            }

            // Try in-place events
            if( ! catchThisVessel ) {
                Map.Entry<LabVessel,VesselPosition> vesselPositionEntry = getTraversalVessel(context);

                for (LabEvent inPlaceEvent : vesselPositionEntry.getKey().getInPlaceLabEvents()) {
                    if (labEventTypes.contains(inPlaceEvent.getLabEventType())) {
                        positions.put(vesselPositionEntry.getKey(), vesselPositionEntry.getValue());
                        catchThisVessel = true;

                        if(captureLatestEventVesselsFlag || captureAllEventVesselsFlag) {
                            eventMap.put(inPlaceEvent,vesselPositionEntry.getKey());
                        }
                        break;
                    }
                }

                // First hop, try looking in containers for event type
                if( ! catchThisVessel && context.getHopCount() == 0 ) {
                    // Still here?  Try the container's in-place events
                    for( VesselContainer container : vesselPositionEntry.getKey().getVesselContainers() ) {
                        for( LabEvent inPlaceEvent : container.getEmbedder().getInPlaceLabEvents() ) {
                            if (labEventTypes.contains(inPlaceEvent.getLabEventType() ) ) {
                                positions.put(vesselPositionEntry.getKey(), container.getPositionOfVessel(vesselPositionEntry.getKey()));
                                catchThisVessel = true;

                                if(captureLatestEventVesselsFlag || captureAllEventVesselsFlag) {
                                    eventMap.put(inPlaceEvent, vesselPositionEntry.getKey());
                                }
                                break;

                            }
                        }
                    }
                }
            }

            if (catchThisVessel) {
                stopTraversingBeforeNextHop = stopTraverseAtFirstFind;
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
            VesselPosition position = null;
            if( context.getHopCount() == 0 ) {
                eventVessel = context.getContextVessel();
                if( eventVessel == null ) {
                    eventVessel = context.getContextVesselContainer().getEmbedder();
                }
            } else if( useEventTarget ) {
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

        /**
         * For cases when looking in ancestry from a flowcell for FCT tickets where a huge amount of overhead
         *   is saved by not traversing farther gathering all other batches
         */
        public VesselBatchTraverserCriteria( boolean isForFctBatchOnly ) {
            this.isForFctBatchOnly = isForFctBatchOnly;
        }

        private Set<LabBatch> labBatches = new HashSet<>();
        private LabVessel startingVessel = null;
        private boolean stopCollectingFctBatches = false;
        private boolean isForFctBatchOnly = false;

        public Set<LabBatch> getLabBatches(){
            return labBatches;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(
                Context context ) {

            // Save a ton of overhead by stopping if we're looking for an FCT batch and we found one
            if( isForFctBatchOnly && stopCollectingFctBatches ) {
                return TraversalControl.StopTraversing;
            }


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
                    if( bucketEntry.getLabBatch() != null ) {
                        labBatches.add(bucketEntry.getLabBatch());
                    }
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
