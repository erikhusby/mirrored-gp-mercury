package org.broadinstitute.gpinformatics.mercury.control.zims;

import edu.mit.broad.prodinfo.thrift.lims.IndexPosition;
import edu.mit.broad.prodinfo.thrift.lims.TZDevExperimentData;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SequencingTemplateFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FlowcellDesignationEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.zims.CrspPipelineUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.Aligner;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.DesignedReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.run.ArchetypeAttribute;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.run.WorkflowMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.SubmissionMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateLaneType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateType;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleData;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class constructs a pipeline API bean from a Mercury chain of custody.
 *
 * @author breilly
 */
@SuppressWarnings("FeatureEnvy")
@Dependent
public class ZimsIlluminaRunFactory {

    private SampleDataFetcher sampleDataFetcher;
    private ControlDao controlDao;
    private SequencingTemplateFactory sequencingTemplateFactory;
    private ProductOrderDao productOrderDao;
    private final CrspPipelineUtils crspPipelineUtils;
    private FlowcellDesignationEjb flowcellDesignationEjb;
    private AttributeArchetypeDao attributeArchetypeDao;
    private static final Log log = LogFactory.getLog(ZimsIlluminaRunFactory.class);

    @Inject
    private JiraService jiraService;

    @Inject
    public ZimsIlluminaRunFactory(SampleDataFetcher sampleDataFetcher,
                                  ControlDao controlDao, SequencingTemplateFactory sequencingTemplateFactory,
                                  ProductOrderDao productOrderDao,
                                  CrspPipelineUtils crspPipelineUtils,
                                  FlowcellDesignationEjb flowcellDesignationEjb,
                                  AttributeArchetypeDao attributeArchetypeDao) {
        this.sampleDataFetcher = sampleDataFetcher;
        this.controlDao = controlDao;
        this.sequencingTemplateFactory = sequencingTemplateFactory;
        this.productOrderDao = productOrderDao;
        this.crspPipelineUtils = crspPipelineUtils;
        this.flowcellDesignationEjb = flowcellDesignationEjb;
        this.attributeArchetypeDao = attributeArchetypeDao;
    }

    private LabVessel getContextVessel(SampleInstanceV2 sampleInstance) {
        if(sampleInstance.getFirstPcrVessel() == null) {
            LabVessel contextVessel = sampleInstance.getInitialLabVessel();
            if (contextVessel != null) {
                if (contextVessel.getTransfersTo().isEmpty()) {
                    if (contextVessel.getLabBatches().size() == 0) {
                        return contextVessel;
                    }
                    for (LabBatch labBatch : contextVessel.getLabBatches()) {
                        if (labBatch.getStartingBatchLabVessels().contains(contextVessel)) {
                            return contextVessel;
                        }
                    }
                }
            }
        }
        return sampleInstance.getFirstPcrVessel();
    }
    public ZimsIlluminaRun makeZimsIlluminaRun(IlluminaSequencingRun illuminaRun) {
        RunCartridge flowcell = illuminaRun.getSampleCartridge();
        FlowcellDesignation flowcellDesignation = null;

        List<List<SampleInstanceDto>> perLaneSampleInstanceDtos = new ArrayList<>();
        Set<String> sampleIds = new HashSet<>();
        Set<String> productOrderKeys = new HashSet<>();
        // Makes DTOs for aliquot samples and product orders, per lane.
        Iterator<String> positionNames = flowcell.getVesselGeometry().getPositionNames();
        short laneNum = 0;
        while (positionNames.hasNext()) {
            ++laneNum;
            List<SampleInstanceDto> sampleInstanceDtos = new ArrayList<>();
            perLaneSampleInstanceDtos.add(sampleInstanceDtos);
            String positionName = positionNames.next();
            VesselPosition vesselPosition = VesselPosition.getByName(positionName);

            boolean mixedLaneOk = false;
            for (SampleInstanceV2 sampleInstance : flowcell.getContainerRole().getSampleInstancesAtPositionV2(
                    vesselPosition)) {
                BucketEntry singleBucketEntry = sampleInstance.getSingleBucketEntry();
                if (singleBucketEntry != null) {
                    if (Objects.equals(singleBucketEntry.getProductOrder().getProduct().getAggregationDataType(),
                        Aggregation.DATA_TYPE_WGS)) {
                        mixedLaneOk = true;
                        break;
                    }
                }
            }

            for (SampleInstanceV2 laneSampleInstance :
                flowcell.getContainerRole().getSampleInstancesAtPositionV2(vesselPosition)) {
                BucketEntry singleBucketEntry = laneSampleInstance.getSingleBucketEntry();
                String productOrderKey = null;
                if (singleBucketEntry != null) {
                    productOrderKey = singleBucketEntry.getPoBusinessKey();
                    productOrderKeys.add(productOrderKey);
                }
                // todo jmt root may be null
                String pdoSampleName;
                ProductOrderSample productOrderSample = laneSampleInstance.getSingleProductOrderSample();
                if (productOrderSample != null) {
                    pdoSampleName = productOrderSample.getSampleKey();
                } else {
                    // Controls won't have a ProductOrderSample, so use root sample ID.
                    pdoSampleName = laneSampleInstance.getMercuryRootSampleName();
                }
                String sampleId = laneSampleInstance.getMercuryRootSampleName();
                LabBatchStartingVessel importLbsv = laneSampleInstance.getSingleBatchVessel(LabBatch.LabBatchType.SAMPLES_IMPORT);
                if (importLbsv != null) {
                    Collection<MercurySample> mercurySamples = importLbsv.getLabVessel().getMercurySamples();
                    if (!mercurySamples.isEmpty()) {
                        sampleId = mercurySamples.iterator().next().getSampleKey();
                    }
                }
                sampleIds.add(sampleId);
                LabVessel libraryVessel = flowcell.getNearestTubeAncestorsForLanes().get(vesselPosition);
                if (flowcellDesignation == null) {
                    // Gets the flowcell designation from batch starting vessel.
                    flowcellDesignation = laneSampleInstance.getAllBatchVessels(LabBatch.LabBatchType.FCT).stream().
                            filter(lbsVessel -> lbsVessel.getFlowcellDesignation() != null).
                            map(LabBatchStartingVessel::getFlowcellDesignation).
                            findFirst().orElse(null);
                }
                boolean isCrspLane;
                if (mixedLaneOk && singleBucketEntry != null) {
                    isCrspLane = singleBucketEntry.getProductOrder().getResearchProject().getRegulatoryDesignation().
                            isClinical();
                } else {
                    isCrspLane = crspPipelineUtils.areAllSamplesForCrsp(
                            libraryVessel.getSampleInstancesV2(), mixedLaneOk);
                }

                String libraryName = libraryVessel.getLabel();
                String metadataSource = laneSampleInstance.getMetadataSourceForPipelineAPI();
                    LabVessel contextVessel = getContextVessel(laneSampleInstance);
                sampleInstanceDtos.add(new SampleInstanceDto(laneNum, contextVessel,
                        laneSampleInstance, sampleId, productOrderKey, libraryName, libraryVessel.getCreatedOn(),
                        pdoSampleName, isCrspLane, metadataSource));
            }
        }
        int numberOfLanes = laneNum;

        Map<String, SampleData> mapSampleIdToDto = sampleDataFetcher.fetchSampleData(sampleIds);
        Map<String, ProductOrder> mapKeyToProductOrder = new HashMap<>();
        for (String productOrderKey : productOrderKeys) {
            mapKeyToProductOrder.put(productOrderKey, productOrderDao.findByBusinessKey(productOrderKey));
        }

        List<Control> activeControls = controlDao.findAllActive();
        Map<String, Control> mapNameToControl = new HashMap<>();
        for (Control activeControl : activeControls) {
            mapNameToControl.put(activeControl.getCollaboratorParticipantId(), activeControl);
        }

        Format dateFormat = FastDateFormat.getInstance(ZimsIlluminaRun.DATE_FORMAT);
        boolean isPaired = flowcellDesignation != null && flowcellDesignation.isPairedEndRead();

        double imagedArea = 0;
        if (illuminaRun.getImagedAreaPerMM2() != null) {
            // avoid unboxing NPE
            imagedArea = illuminaRun.getImagedAreaPerMM2();
        }

        ZimsIlluminaRun run = new ZimsIlluminaRun(illuminaRun.getRunName(), illuminaRun.getRunBarcode(),
                                                  flowcell.getLabel(), illuminaRun.getMachineName(),
                                                  flowcell.getSequencerModel(), dateFormat.format(
                illuminaRun.getRunDate()),
                                                  isPaired, illuminaRun.getActualReadStructure(), imagedArea,
                                                  illuminaRun.getSetupReadStructure(), illuminaRun.getLanesSequenced(),
                                                  illuminaRun.getRunDirectory(), SystemRouter.System.MERCURY);

        IlluminaFlowcell illuminaFlowcell = (IlluminaFlowcell) flowcell;
        Set<VesselAndPosition> loadedVesselsAndPositions = illuminaFlowcell.getLoadingVessels();
        List<SequencingTemplateLaneType> sequencingTemplateLanes = null;
        try {
            SequencingTemplateType sequencingTemplate = sequencingTemplateFactory.getSequencingTemplate(
                    illuminaFlowcell, loadedVesselsAndPositions, false);
            sequencingTemplateLanes = sequencingTemplate.getLanes();
            if (sequencingTemplateLanes != null) {
                Collections.sort(sequencingTemplateLanes, new Comparator<SequencingTemplateLaneType>() {
                    @Override
                    public int compare(SequencingTemplateLaneType lane1, SequencingTemplateLaneType lane2) {
                        return lane1.getLaneName().compareTo(lane2.getLaneName());
                    }
                });
            }
        } catch (Exception e) {
            log.error("Failed to get sequencingTemplate.", e);
            throw e;
        }
        Map<String, WorkflowMetadata> mapWorkflowToMetadata = new HashMap<>();
        for (List<SampleInstanceDto> sampleInstanceDtos : perLaneSampleInstanceDtos) {
            if (sampleInstanceDtos != null && !sampleInstanceDtos.isEmpty()) {
                ArrayList<LibraryBean> libraryBeans = new ArrayList<>();
                SampleInstanceDto sampleInstanceDto = sampleInstanceDtos.get(0);
                short laneNumber = sampleInstanceDto.getLaneNumber();
                String workflowName = sampleInstanceDto.getSampleInstance().getWorkflowName();
                if (workflowName != null) {
                    if (!mapWorkflowToMetadata.containsKey(workflowName)) {
                        mapWorkflowToMetadata.put(workflowName, attributeArchetypeDao.findWorkflowMetadata(workflowName));
                    }
                }
                libraryBeans.addAll(
                        makeLibraryBeans(sampleInstanceDtos, mapSampleIdToDto, mapKeyToProductOrder, mapNameToControl,
                                mapWorkflowToMetadata));
                String sequencedLibraryName = sampleInstanceDto.getSequencedLibraryName();
                Date sequencedLibraryDate = sampleInstanceDto.getSequencedLibraryDate();

                String setupReadStructure = null;
                BigDecimal loadingConcentration = null;
                if (sequencingTemplateLanes != null && sequencingTemplateLanes.size() == numberOfLanes) {
                    loadingConcentration = sequencingTemplateLanes.get(laneNumber - 1).getLoadingConcentration();
                    setupReadStructure = sequencingTemplateLanes.get(laneNumber - 1).getReadStructure();
                }
                IlluminaSequencingRunChamber sequencingRunChamber = illuminaRun.getSequencingRunChamber(laneNumber);
                String actualReadStructure = null;
                if (sequencingRunChamber != null) {
                    actualReadStructure = sequencingRunChamber.getActualReadStructure();
                }
                ZimsIlluminaChamber lane = new ZimsIlluminaChamber(laneNumber, libraryBeans, null, sequencedLibraryName,
                                                                   sequencedLibraryDate,
                                                                   loadingConcentration == null ? null :
                                                                           loadingConcentration.doubleValue(),
                                                                   actualReadStructure, setupReadStructure);
                run.addLane(lane);
            }
        }

        return run;
    }

    @DaoFree
    public List<LibraryBean> makeLibraryBeans(List<SampleInstanceDto> sampleInstanceDtos,
                                              Map<String, SampleData> mapSampleIdToDto,
                                              Map<String, ProductOrder> mapKeyToProductOrder,
                                              Map<String, Control> mapNameToControl,
                                              Map<String, WorkflowMetadata> mapWorkflowToMetadata) {
        List<LibraryBean> libraryBeans = new ArrayList<>();

        // Get distinct analysis types and reference sequences.  If there's only one distinct, it's used for the
        // positive control.
        Set<String> analysisTypes = new HashSet<>();
        Set<String> referenceSequenceKeys = new HashSet<>();
        Set<String> aggregationDataTypes = new HashSet<>();
        Set<Integer> insertSizes = new HashSet<>();
        Set<ResearchProject> positiveControlResearchProjects = new HashSet<>();
        for (SampleInstanceDto sampleInstanceDto : sampleInstanceDtos) {
            ProductOrder productOrder = (sampleInstanceDto.getProductOrderKey() != null) ?
                    mapKeyToProductOrder.get(sampleInstanceDto.getProductOrderKey()) : null;
            if (productOrder != null) {
                Product product = productOrder.getProduct();
                analysisTypes.add(product.getAnalysisTypeKey());
                aggregationDataTypes.add(product.getAggregationDataType());
                ResearchProject project = productOrder.getResearchProject();
                if (!StringUtils.isBlank(project.getReferenceSequenceKey())) {
                    referenceSequenceKeys.add(project.getReferenceSequenceKey());
                }
                ResearchProject positiveControlResearchProject = product.getPositiveControlResearchProject();
                if (positiveControlResearchProject != null) {
                    positiveControlResearchProjects.add(positiveControlResearchProject);
                }
                Integer insertSize = product.getInsertSize();
                if (insertSize != null) {
                    insertSizes.add(insertSize);
                }
            }
        }

        for (SampleInstanceDto sampleInstanceDto : sampleInstanceDtos) {
            SampleInstanceV2 sampleInstance = sampleInstanceDto.getSampleInstance();
            ProductOrder productOrder = (sampleInstanceDto.getProductOrderKey() != null) ?
                    mapKeyToProductOrder.get(sampleInstanceDto.getProductOrderKey()) : null;

            List<LabBatch> lcSetBatches = new ArrayList<>();
            for (LabBatchStartingVessel labBatchStartingVessel :
                    sampleInstance.getAllBatchVessels(LabBatch.LabBatchType.WORKFLOW)) {
                lcSetBatches.add(labBatchStartingVessel.getLabBatch());
            }

            // if there's at least one LCSET batch, and getLabBatch (the singular version) returns null,
            // then throw an exception
            if (lcSetBatches.size() > 1 && sampleInstance.getSingleBatch() == null) {
                throw new RuntimeException(
                        String.format("Expected one LabBatch but found %s.", lcSetBatches.size()));
            }
            String lcSet = null;
            if (sampleInstance.getSingleBatch() != null) {
                lcSet = sampleInstance.getSingleBatch().getBatchName();
            }

            // This loop goes through all the reagents and takes the last bait name (under the assumption that
            // the lab would only ever have one for this sample instance. All cat names are collected and the
            // last indexing scheme reagent.
            MolecularIndexingScheme indexingSchemeEntity = null;
            String baitName = null;
            List<String> catNames = new ArrayList<>();

            // If this is an uploaded library, uses its reagent design.
            if(!sampleInstance.getReagentsDesigns().isEmpty())            {
                for (ReagentDesign reagentDesign : sampleInstance.getReagentsDesigns()) {
                    indexingSchemeEntity = sampleInstance.getMolecularIndexingScheme();
                    ReagentDesign.ReagentType reagentType = reagentDesign.getReagentType();
                        if (reagentType == ReagentDesign.ReagentType.BAIT) {
                            baitName = reagentDesign.getDesignName();
                        } else if (reagentType == ReagentDesign.ReagentType.CAT) {
                            catNames.add(reagentDesign.getDesignName());
                        }
                    }
            }
            else {
                for (Reagent reagent : sampleInstance.getReagents()) {
                    if (OrmUtil.proxySafeIsInstance(reagent, MolecularIndexReagent.class)) {
                        indexingSchemeEntity =
                                OrmUtil.proxySafeCast(reagent, MolecularIndexReagent.class).getMolecularIndexingScheme();
                    } else if (OrmUtil.proxySafeIsInstance(reagent, DesignedReagent.class)) {
                        DesignedReagent designedReagent = OrmUtil.proxySafeCast(reagent, DesignedReagent.class);
                        ReagentDesign.ReagentType reagentType = designedReagent.getReagentDesign().getReagentType();
                        if (reagentType == ReagentDesign.ReagentType.BAIT) {
                            baitName = designedReagent.getReagentDesign().getDesignName();
                        } else if (reagentType == ReagentDesign.ReagentType.CAT) {
                            catNames.add(designedReagent.getReagentDesign().getDesignName());
                        }
                    }
                }
            }
            edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme indexingSchemeDto = null;
            if (indexingSchemeEntity != null) {
                Map<IndexPosition, String> positionSequenceMap = new HashMap<>();
                Set<Map.Entry<MolecularIndexingScheme.IndexPosition, MolecularIndex>> entries =
                        indexingSchemeEntity.getIndexes().entrySet();

                for (Map.Entry<MolecularIndexingScheme.IndexPosition, MolecularIndex> indexEntry : entries) {
                    String indexName = indexEntry.getKey().toString();
                    positionSequenceMap.put(
                            IndexPosition.valueOf(indexName.substring(indexName.lastIndexOf('_') + 1)),
                            indexEntry.getValue().getSequence());
                }

                indexingSchemeDto = new edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme(
                        indexingSchemeEntity.getName(), positionSequenceMap);
            }

            SampleData sampleData = mapSampleIdToDto.get(sampleInstanceDto.getSampleId());
            Boolean isPooledTube = sampleInstance.getIsPooledTube();
            if (isPooledTube && sampleData instanceof MercurySampleData) {
                MercurySampleData mercurySampleData = (MercurySampleData) sampleData;
                mercurySampleData.setRootSampleId(sampleInstance.getMercuryRootSampleName());
                mercurySampleData.setSampleId(sampleInstance.getNearestMercurySampleName());
            }
            TZDevExperimentData devExperimentData = sampleInstance.getTzDevExperimentData();
            WorkflowMetadata workflowMetadata = mapWorkflowToMetadata.get(sampleInstance.getWorkflowName());
            libraryBeans.add(createLibraryBean(sampleInstanceDto, productOrder, sampleData, lcSet,
                    baitName, indexingSchemeEntity, catNames, sampleInstanceDto.getSampleInstance().getWorkflowName(),
                    indexingSchemeDto, mapNameToControl, sampleInstanceDto.getPdoSampleName(),
                    sampleInstanceDto.isCrspLane(), sampleInstanceDto.getMetadataSourceForPipelineAPI(), analysisTypes,
                    referenceSequenceKeys, aggregationDataTypes, positiveControlResearchProjects, insertSizes,
                    devExperimentData, isPooledTube, workflowMetadata));
        }

        // Make order predictable.  Include library name because for ICE there are 8 ancestor catch tubes, all with
        // the same samples.  We must tell the pipeline the same library name when they ask multiple times.
        Collections.sort(libraryBeans, LibraryBean.BY_SAMPLE_ID_LIBRARY);

        // Consolidates beans that have the same consolidation key.
        SortedSet<String> previouslySeenSampleAndMis = new TreeSet<>();
        for (Iterator<LibraryBean> iter = libraryBeans.iterator(); iter.hasNext(); ) {
            LibraryBean libraryBean = iter.next();

            //In certain cases no moleclular indexing scheme will be preset.
            String molIndexSecheme = "";
            if(libraryBean.getMolecularIndexingScheme() != null) {
                molIndexSecheme = libraryBean.getMolecularIndexingScheme().getName();
            }

            String consolidationKey =
                    makeConsolidationKey(libraryBean.getSampleId(), molIndexSecheme);
            if (!previouslySeenSampleAndMis.add(consolidationKey)) {
                iter.remove();
            }
        }
        return libraryBeans;
    }

    private String makeConsolidationKey(String... components) {
        return StringUtils.join(components, "__delimiter__");
    }

    private LibraryBean createLibraryBean(
            SampleInstanceDto sampleInstanceDto, ProductOrder productOrder, SampleData sampleData, String lcSet, String baitName,
            MolecularIndexingScheme indexingSchemeEntity, List<String> catNames, String labWorkflow,
            edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme indexingSchemeDto,
            Map<String, Control> mapNameToControl, String pdoSampleName,
            boolean isCrspLane, String metadataSourceForPipelineAPI, Set<String> analysisTypes,
            Set<String> referenceSequenceKeys, Set<String> aggregationDataTypes,
            Set<ResearchProject> positiveControlProjects, Set<Integer> insertSizes, TZDevExperimentData devExperimentData,
            boolean isPooledTube, WorkflowMetadata workflowMetadata) {

        Format dateFormat = FastDateFormat.getInstance(ZimsIlluminaRun.DATE_FORMAT);

        String label = "";
        String libraryCreationDate = "";

        //If this is an uploaded pooled tube, create the label based on the provided library name.
        if(isPooledTube) {
            label = sampleInstanceDto.getSampleInstance().getSampleLibraryName();
            libraryCreationDate = dateFormat.format(sampleInstanceDto.getSampleInstance().getLibraryCreationDate());
        }
        else {
            label = sampleInstanceDto.labVessel.getLabel();
            libraryCreationDate = dateFormat.format(sampleInstanceDto.labVessel.getCreatedOn());
        }

        String library = label + (indexingSchemeEntity == null ? "" : "_" + indexingSchemeEntity.getName());
        String initiative = null;
        Long workRequest = null;
        Boolean hasIndexingRead = null;
        String expectedInsertSize = null;
        String organism = null;
        String strain = null;
        String rrbsSizeRange = null;
        String restrictionEnzyme = null;
        double labMeasuredInsertSize = 0.0;
        Boolean positiveControl = null;
        Boolean negativeControl = null;
        Collection<String> gssrBarcodes = null;
        String gssrSampleType = null;
        Boolean doAggregation = Boolean.TRUE;
        ResearchProject positiveControlProject = null;

        String analysisType = null;
        String referenceSequence = null;
        String referenceSequenceVersion = null;
        String aggregationDataType = null;
        String species = null;
        if (sampleData != null && productOrder == null) {
            Control control = mapNameToControl.get(sampleData.getCollaboratorParticipantId());
            species = sampleData.getOrganism();
            if (control != null) {
                switch (control.getType()) {
                case POSITIVE:
                    positiveControl = true;
                    if (analysisTypes.size() == 1 && referenceSequenceKeys.size() == 1 &&
                            aggregationDataTypes.size() == 1 && positiveControlProjects.size() <= 1 &&
                            insertSizes.size() <= 1) {
                        analysisType = analysisTypes.iterator().next();
                        String[] referenceSequenceValues = referenceSequenceKeys.iterator().next().split("\\|");
                        referenceSequence = referenceSequenceValues[0];
                        referenceSequenceVersion = referenceSequenceValues[1];
                        aggregationDataType = aggregationDataTypes.iterator().next();
                        if (positiveControlProjects.size() == 1) {
                            positiveControlProject = positiveControlProjects.iterator().next();
                        }
                        if (insertSizes.size() == 1) {
                            expectedInsertSize = insertSizes.iterator().next().toString();
                        }
                    }
                    break;
                case NEGATIVE:
                    negativeControl = true;
                    break;
                }
            }
        }

        // These items are pulled off the project, product, or SampleInstanceEntity.
        String aligner = null;
        Boolean analyzeUmi = sampleInstanceDto.sampleInstance.getUmisPresent();
        if (sampleInstanceDto.sampleInstance.getAnalysisType() != null) {
            analysisType = sampleInstanceDto.sampleInstance.getAnalysisType().getBusinessKey();
        }

        // insert size is a  range consisting of two integers with a hyphen in between, e.g. "225-350".
        if (expectedInsertSize == null) {
            expectedInsertSize = sampleInstanceDto.sampleInstance.getExpectedInsertSize();
        }
        String aggregationParticle = sampleInstanceDto.sampleInstance.getAggregationParticle();
         if (sampleInstanceDto.sampleInstance.getReferenceSequence() != null) {
            referenceSequence = sampleInstanceDto.sampleInstance.getReferenceSequence().getName();
            referenceSequenceVersion = sampleInstanceDto.sampleInstance.getReferenceSequence().getVersion();
        }

        // Uses the bait found in the workflow reagents or from the uploaded library.
        // If none, takes the one defined on the product order.
        String bait = StringUtils.isNotBlank(sampleInstanceDto.sampleInstance.getBaitNameOverride()) ?
                sampleInstanceDto.sampleInstance.getBaitNameOverride() : baitName;

        if (productOrder != null) {
            Product product = productOrder.getProduct();
            if (StringUtils.isBlank(expectedInsertSize) && product.getInsertSize() != null) {
                expectedInsertSize = product.getInsertSize().toString();
            }
            if (analyzeUmi == null) {
                analyzeUmi = productOrder.getAnalyzeUmiOverride();
            }
            if (analysisType == null) {
                analysisType = product.getAnalysisTypeKey();
            }
            if (bait == null) {
                bait = productOrder.getReagentDesignKey();
            }

            // Project stuff.
            ResearchProject project = productOrder.getResearchProject();
            aligner = project.getSequenceAlignerKey();
            if (Aligner.UNALIGNED.equals(aligner)) {
                aligner = null;
            }
            // If there is a reference sequence value on the project, then populate the name and version.
            if (StringUtils.isBlank(referenceSequence) && !StringUtils.isBlank(project.getReferenceSequenceKey())) {
                String[] referenceSequenceValues = project.getReferenceSequenceKey().split("\\|");
                referenceSequence = referenceSequenceValues[0];
                referenceSequenceVersion = referenceSequenceValues[1];
            }
        }
        if (ReferenceSequence.NO_REFERENCE_SEQUENCE.equals(referenceSequence)) {
            referenceSequence = null;
            referenceSequenceVersion = null;
        }
        if (AnalysisType.NO_ANALYSIS.equals(analysisType)) {
            analysisType = null;
        }

        List<SubmissionMetadata> submissionMetadataList = new ArrayList<>();
            if (workflowMetadata != null) {
            Set<ArchetypeAttribute> attributes = workflowMetadata.getAttributes();
            for (ArchetypeAttribute archetypeAttribute: attributes) {
                SubmissionMetadata metadata = new SubmissionMetadata(archetypeAttribute.getAttributeName(),
                        archetypeAttribute.getAttributeValue());
                submissionMetadataList.add(metadata);
            }
        }

        LibraryBean libraryBean = new LibraryBean(
                library, initiative, workRequest, indexingSchemeDto, hasIndexingRead, expectedInsertSize,
                analysisType, referenceSequence, referenceSequenceVersion, organism, species,
                strain, aligner, rrbsSizeRange, restrictionEnzyme, bait, labMeasuredInsertSize,
                positiveControl, negativeControl, devExperimentData, gssrBarcodes, gssrSampleType, doAggregation,
                catNames, productOrder, lcSet, sampleData, labWorkflow, libraryCreationDate, pdoSampleName,
                metadataSourceForPipelineAPI, aggregationDataType, jiraService, submissionMetadataList,
                Boolean.TRUE.equals(analyzeUmi), aggregationParticle);
        if (isCrspLane) {
            crspPipelineUtils.setFieldsForCrsp(libraryBean, sampleData, bait);
        }
        if (Boolean.TRUE.equals(libraryBean.isPositiveControl())) {
            String participantWithLcSetName = libraryBean.getCollaboratorParticipantId() + "_" + lcSet;
            libraryBean.setCollaboratorSampleId(participantWithLcSetName);
            libraryBean.setCollaboratorParticipantId(participantWithLcSetName);

            if (positiveControlProject != null) {
                libraryBean.setResearchProjectId(positiveControlProject.getBusinessKey());
                libraryBean.setResearchProjectName(positiveControlProject.getTitle());
                libraryBean.setRegulatoryDesignation(positiveControlProject.getRegulatoryDesignationCodeForPipeline());
            }
        }

        return libraryBean;
    }

    public void setSequencingTemplateFactory(
            SequencingTemplateFactory sequencingTemplateFactory) {
        this.sequencingTemplateFactory = sequencingTemplateFactory;
    }

    /**
     * Need DTO to store choice of aliquot or root sample ID.
     */
    static class SampleInstanceDto {
        private short laneNumber;
        private LabVessel labVessel;
        private SampleInstanceV2 sampleInstance;
        private String sampleId;
        private String productOrderKey;
        private String sequencedLibraryName;
        private Date sequencedLibraryDate;
        private String pdoSampleName;
        private final boolean isCrspLane;
        private final String metadataSource;

        public SampleInstanceDto(short laneNumber, LabVessel labVessel, SampleInstanceV2 sampleInstance,
                                 String sampleId,
                                 String productOrderKey, String sequencedLibraryName, Date sequencedLibraryDate,
                                 String pdoSampleName, boolean isCrspLane, String metadataSource) {
            this.laneNumber = laneNumber;
            this.labVessel = labVessel;
            this.sampleInstance = sampleInstance;
            this.sampleId = sampleId;
            this.productOrderKey = productOrderKey;
            this.sequencedLibraryName = sequencedLibraryName;
            this.sequencedLibraryDate = sequencedLibraryDate;
            this.pdoSampleName = pdoSampleName;
            this.isCrspLane = isCrspLane;
            this.metadataSource = metadataSource;
        }

        /**
         * True if the data is being used for CRSP
         */
        public boolean isCrspLane() {
            return isCrspLane;
        }

        public short getLaneNumber() {
            return laneNumber;
        }

        public LabVessel getLabVessel() {
            return labVessel;
        }

        public SampleInstanceV2 getSampleInstance() {
            return sampleInstance;
        }

        public String getSampleId() {
            return sampleId;
        }

        public String getProductOrderKey() {
            return productOrderKey;
        }

        String getSequencedLibraryName() {
            return sequencedLibraryName;
        }

        Date getSequencedLibraryDate() {
            return sequencedLibraryDate;
        }

        String getPdoSampleName() {
            return pdoSampleName;
        }

        public String getMetadataSourceForPipelineAPI() {
            return metadataSource;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof SampleInstanceDto)) {
                return false;
            }

            SampleInstanceDto castOther = (SampleInstanceDto) other;
            return new EqualsBuilder()
                    .append(laneNumber, castOther.getLaneNumber())
                    .append(labVessel, castOther.getLabVessel())
                    .append(productOrderKey, castOther.getProductOrderKey())
                    .append(sequencedLibraryName, castOther.getSequencedLibraryName())
                    .append(sequencedLibraryDate, castOther.getSequencedLibraryDate())
                    .append(sampleId, castOther.getSampleId())
                    .append(sampleInstance, castOther.getSampleInstance())
                    .append(pdoSampleName, castOther.getPdoSampleName()).isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(laneNumber)
                    .append(labVessel)
                    .append(productOrderKey)
                    .append(sequencedLibraryName)
                    .append(sequencedLibraryDate)
                    .append(sampleId)
                    .append(sampleInstance)
                    .append(pdoSampleName)
                    .toHashCode();
        }
    }

}
