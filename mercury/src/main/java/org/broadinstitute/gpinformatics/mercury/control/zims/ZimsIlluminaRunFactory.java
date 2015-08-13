package org.broadinstitute.gpinformatics.mercury.control.zims;

import edu.mit.broad.prodinfo.thrift.lims.IndexPosition;
import edu.mit.broad.prodinfo.thrift.lims.TZDevExperimentData;
import org.apache.commons.collections4.Factory;
import org.apache.commons.collections4.map.LazyMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SequencingTemplateFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.boundary.zims.CrspPipelineUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.DesignedReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWellTransient;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateLaneType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateType;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This class constructs a pipeline API bean from a Mercury chain of custody.
 *
 * @author breilly
 */
@SuppressWarnings("FeatureEnvy")
public class ZimsIlluminaRunFactory {

    private SampleDataFetcher sampleDataFetcher;
    private ControlDao controlDao;
    private SequencingTemplateFactory sequencingTemplateFactory;
    private ProductOrderDao productOrderDao;
    private ResearchProjectDao researchProjectDao;
    private final CrspPipelineUtils crspPipelineUtils;

    // Positive controls must have a product part number in order to go through variant calling.  Controls aren't
    // entered into product orders, so the part number must be determined by looking at the other samples in the lane.
    // If all samples are in one of the following sets, use the first entry in the set (germline) for the positive
    // control.
    static final String AGILENT_GERMLINE_PART_NUMBER = "P-CLA-0001";
    static final String AGILENT_SOMATIC_PART_NUMBER = "P-CLA-0002";
    private static final Set<String> AGILENT_PART_NUMBERS = new LinkedHashSet<String>() {{
        add(AGILENT_GERMLINE_PART_NUMBER);
        add(AGILENT_SOMATIC_PART_NUMBER);
    }};

    static final String ICE_GERMLINE_PART_NUMBER = "P-CLA-0003";
    static final String ICE_SOMATIC_PART_NUMBER = "P-CLA-0004";
    private static final Set<String> ICE_PART_NUMBERS = new LinkedHashSet<String>() {{
        add(ICE_GERMLINE_PART_NUMBER);
        add(ICE_SOMATIC_PART_NUMBER);
    }};

    static final String BUICK_PART_NUMBER = "P-EX-0011";
    private static final Set<String> BUICK_PART_NUMBERS = new LinkedHashSet<String>() {{
        add(BUICK_PART_NUMBER);
    }};

    private static final List<Set<String>> PART_NUMBER_SETS = new ArrayList<Set<String>>() {{
        add(AGILENT_PART_NUMBERS);
        add(ICE_PART_NUMBERS);
        add(BUICK_PART_NUMBERS);
    }};

    private static final Log log = LogFactory.getLog(ZimsIlluminaRunFactory.class);

    @Inject
    public ZimsIlluminaRunFactory(SampleDataFetcher sampleDataFetcher,
                                  ControlDao controlDao, SequencingTemplateFactory sequencingTemplateFactory,
                                  ProductOrderDao productOrderDao,
                                  ResearchProjectDao researchProjectDao,
                                  CrspPipelineUtils crspPipelineUtils) {
        this.sampleDataFetcher = sampleDataFetcher;
        this.controlDao = controlDao;
        this.sequencingTemplateFactory = sequencingTemplateFactory;
        this.productOrderDao = productOrderDao;
        this.researchProjectDao = researchProjectDao;
        this.crspPipelineUtils = crspPipelineUtils;
    }

    public ZimsIlluminaRun makeZimsIlluminaRun(IlluminaSequencingRun illuminaRun) {
        RunCartridge flowcell = illuminaRun.getSampleCartridge();
        ResearchProject crspPositiveControlsProject = researchProjectDao.findByBusinessKey(
                crspPipelineUtils.getResearchProjectForCrspPositiveControls()
        );

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
            // todo jmt delete this code, and determine if remainder can be simplified
//            PipelineTransformationCriteria criteria = new PipelineTransformationCriteria();
//            flowcell.getContainerRole().evaluateCriteria(vesselPosition, criteria, Ancestors, null, 0);
//            Map<SampleInstanceV2, SampleInstanceV2> laneSampleInstances = new HashMap<>();
            for (SampleInstanceV2 laneSampleInstance :
                    flowcell.getContainerRole().getSampleInstancesAtPositionV2(vesselPosition)) {
//                laneSampleInstances.put(sampleInstanceV2, sampleInstanceV2);
//            }
//
//            for (LabVessel labVessel : criteria.getNearestLabVessels()) {
//                Set<SampleInstanceV2> sampleInstances = labVessel.getSampleInstancesV2();
//                for (SampleInstanceV2 sampleInstance : sampleInstances) {
//                    // Must use equivalent sample instance in the lane, to reflect any rework LCSET since the catch.
//                    SampleInstanceV2 laneSampleInstance = laneSampleInstances.get(sampleInstance);
//                    if (laneSampleInstance == null) {
//                        throw new RuntimeException("Failed to find " + sampleInstance.getMercuryRootSampleName() +
//                                " in lane " + laneNum);
//                    }
                    BucketEntry singleBucketEntry = laneSampleInstance.getSingleBucketEntry();
                    String productOrderKey = null;
                    if (singleBucketEntry != null) {
                        productOrderKey = singleBucketEntry.getPoBusinessKey();
                        productOrderKeys.add(productOrderKey);
                    }
                    // todo jmt root may be null
                    String pdoSampleName = laneSampleInstance.getMercuryRootSampleName();
                    String sampleId = pdoSampleName;
                    LabBatchStartingVessel importLbsv = laneSampleInstance.getSingleBatchVessel(LabBatch.LabBatchType.SAMPLES_IMPORT);
                    if (importLbsv != null) {
                        Collection<MercurySample> mercurySamples = importLbsv.getLabVessel().getMercurySamples();
                        if (!mercurySamples.isEmpty()) {
                            sampleId = mercurySamples.iterator().next().getSampleKey();
                        }
                    }
                    sampleIds.add(sampleId);
                    LabVessel libraryVessel = flowcell.getNearestTubeAncestorsForLanes().get(vesselPosition);

                    boolean isCrspLane = crspPipelineUtils.areAllSamplesForCrsp(
                            libraryVessel.getSampleInstancesV2());

                    String libraryName = libraryVessel.getLabel();
                    String metadataSource = laneSampleInstance.getMetadataSourceForPipelineAPI();

                    sampleInstanceDtos.add(new SampleInstanceDto(laneNum, laneSampleInstance.getFirstPcrVessel(),
                            laneSampleInstance, sampleId, productOrderKey, libraryName, libraryVessel.getCreatedOn(),
                            pdoSampleName,isCrspLane,metadataSource));
//                }
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
        // TODO: fill in isPaired

        double imagedArea = 0;
        if (illuminaRun.getImagedAreaPerMM2() != null) {
            // avoid unboxing NPE
            imagedArea = illuminaRun.getImagedAreaPerMM2();
        }

        ZimsIlluminaRun run = new ZimsIlluminaRun(illuminaRun.getRunName(), illuminaRun.getRunBarcode(),
                                                  flowcell.getLabel(), illuminaRun.getMachineName(),
                                                  flowcell.getSequencerModel(), dateFormat.format(
                illuminaRun.getRunDate()),
                                                  false, illuminaRun.getActualReadStructure(), imagedArea,
                                                  illuminaRun.getSetupReadStructure(), illuminaRun.getLanesSequenced(),
                                                  illuminaRun.getRunDirectory(), SystemRouter.System.MERCURY);

        IlluminaFlowcell illuminaFlowcell = (IlluminaFlowcell) flowcell;
        Set<VesselAndPosition> loadedVesselsAndPositions = illuminaFlowcell.getLoadingVessels();
        List<SequencingTemplateLaneType> sequencingTemplateLanes = null;
        try {
            SequencingTemplateType sequencingTemplate = sequencingTemplateFactory.getSequencingTemplate(
                    illuminaFlowcell, loadedVesselsAndPositions, true);
            sequencingTemplateLanes = sequencingTemplate.getLanes();
        } catch (Exception e) {
            log.error("Failed to get sequencingTemplate.", e);
            // don't rethrow, failing to get loading concentration is not fatal.
        }
        for (List<SampleInstanceDto> sampleInstanceDtos : perLaneSampleInstanceDtos) {
            if (sampleInstanceDtos != null && !sampleInstanceDtos.isEmpty()) {
                ArrayList<LibraryBean> libraryBeans = new ArrayList<>();
                SampleInstanceDto sampleInstanceDto = sampleInstanceDtos.get(0);
                short laneNumber = sampleInstanceDto.getLaneNumber();
                libraryBeans.addAll(
                        makeLibraryBeans(sampleInstanceDtos, mapSampleIdToDto, mapKeyToProductOrder, mapNameToControl,crspPositiveControlsProject));
                String sequencedLibraryName = sampleInstanceDto.getSequencedLibraryName();
                Date sequencedLibraryDate = sampleInstanceDto.getSequencedLibraryDate();

                BigDecimal loadingConcentration = null;
                if (sequencingTemplateLanes != null && sequencingTemplateLanes.size() == numberOfLanes) {
                    loadingConcentration = sequencingTemplateLanes.get(laneNumber - 1).getLoadingConcentration();
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
                                                                   actualReadStructure);
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
                                              ResearchProject crspPositiveControlProject) {
        List<LibraryBean> libraryBeans = new ArrayList<>();

        // Get distinct analysis types and reference sequences.  If there's only one distinct, it's used for the
        // positive control.
        Set<String> analysisTypes = new HashSet<>();
        Set<String> referenceSequenceKeys = new HashSet<>();
        Set<String> aggregationDataTypes = new HashSet<>();
        Set<String> productPartNumbers = new HashSet<>();
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
                productPartNumbers.add(product.getPartNumber());
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

            libraryBeans.add(createLibraryBean(sampleInstanceDto.getLabVessel(), productOrder, sampleData, lcSet,
                    baitName, indexingSchemeEntity, catNames, sampleInstanceDto.getSampleInstance().getWorkflowName(),
                    indexingSchemeDto, mapNameToControl, sampleInstanceDto.getPdoSampleName(),
                    sampleInstanceDto.isCrspLane(), crspPositiveControlProject,
                    sampleInstanceDto.getMetadataSourceForPipelineAPI(), analysisTypes, referenceSequenceKeys,
                    aggregationDataTypes, productPartNumbers));
        }

        // Make order predictable.  Include library name because for ICE there are 8 ancestor catch tubes, all with
        // the same samples.  We must tell the pipeline the same library name when they ask multiple times.
        Collections.sort(libraryBeans, LibraryBean.BY_SAMPLE_ID_LIBRARY);

        // Consolidates beans that have the same consolidation key.
        SortedSet<String> previouslySeenSampleAndMis = new TreeSet<>();
        for (Iterator<LibraryBean> iter = libraryBeans.iterator(); iter.hasNext(); ) {
            LibraryBean libraryBean = iter.next();
            String consolidationKey =
                    makeConsolidationKey(libraryBean.getSampleId(), libraryBean.getMolecularIndexingScheme().getName());
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
            LabVessel labVessel, ProductOrder productOrder, SampleData sampleData, String lcSet, String baitName,
            MolecularIndexingScheme indexingSchemeEntity, List<String> catNames, String labWorkflow,
            edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme indexingSchemeDto,
            Map<String, Control> mapNameToControl, String pdoSampleName,
            boolean isCrspLane, ResearchProject crspPositiveControlsProject,
            String metadataSourceForPipelineAPI, Set<String> analysisTypes, Set<String> referenceSequenceKeys,
            Set<String> aggregationDataTypes, Set<String> productPartNumbers) {

        Format dateFormat = FastDateFormat.getInstance(ZimsIlluminaRun.DATE_FORMAT);

        String library =
                labVessel.getLabel() + (indexingSchemeEntity == null ? "" : "_" + indexingSchemeEntity.getName());
        String initiative = null;
        Long workRequest = null;
        Boolean hasIndexingRead = null;
        String expectedInsertSize = null;
        String organism = null;
        String species = null;
        String strain = null;
        String rrbsSizeRange = null;
        String restrictionEnzyme = null;
        double labMeasuredInsertSize = 0.0;
        Boolean positiveControl = null;
        Boolean negativeControl = null;
        TZDevExperimentData devExperimentData = null;
        Collection<String> gssrBarcodes = null;
        String gssrSampleType = null;
        Boolean doAggregation = Boolean.TRUE;
        String controlProductPartNumber = null;

        String analysisType = null;
        String referenceSequence = null;
        String referenceSequenceVersion = null;
        String aggregationDataType = null;
        if (sampleData != null && productOrder == null) {
            Control control = mapNameToControl.get(sampleData.getCollaboratorParticipantId());
            if (control != null) {
                switch (control.getType()) {
                case POSITIVE:
                    positiveControl = true;
                    if (analysisTypes.size() == 1 && referenceSequenceKeys.size() == 1 &&
                            aggregationDataTypes.size() == 1) {
                        // horrible 7/25 hack.  todo fixme with workflow
                        analysisType = "HybridSelection." + analysisTypes.iterator().next();

                        String[] referenceSequenceValues = referenceSequenceKeys.iterator().next().split("\\|");
                        referenceSequence = referenceSequenceValues[0];
                        referenceSequenceVersion = referenceSequenceValues[1];
                        aggregationDataType = aggregationDataTypes.iterator().next();
                        controlProductPartNumber = getControlProductPartNumber(productPartNumbers);
                    }
                    break;
                case NEGATIVE:
                    negativeControl = true;
                    break;
                }
            }
        }

        // default to the passed in bait name, but override if there is product specified version.
        String bait = baitName;

        // These items are pulled off the project or product.
        String aligner = null;
        if (productOrder != null) {
            // Product stuff.
            Product product = productOrder.getProduct();
            analysisType = product.getAnalysisTypeKey();

            // If there was no bait on the actual samples, use the one defined on the product.
            if (bait == null) {
                bait = product.getReagentDesignKey();
            }

            // Project stuff.
            ResearchProject project = productOrder.getResearchProject();
            aligner = project.getSequenceAlignerKey();

            // If there is a reference sequence value on the project, then populate the name and version.
            if (!StringUtils.isBlank(project.getReferenceSequenceKey())) {
                String[] referenceSequenceValues = project.getReferenceSequenceKey().split("\\|");
                referenceSequence = referenceSequenceValues[0];
                referenceSequenceVersion = referenceSequenceValues[1];
            }

            // horrible 7/25 hack.  todo fixme with workflow
            if (analysisType != null) {
                analysisType = "HybridSelection." + analysisType;
            }
        }
        String libraryCreationDate = dateFormat.format(labVessel.getCreatedOn());

        LibraryBean libraryBean = new LibraryBean(
                library, initiative, workRequest, indexingSchemeDto, hasIndexingRead, expectedInsertSize,
                analysisType, referenceSequence, referenceSequenceVersion, organism, species,
                strain, aligner, rrbsSizeRange, restrictionEnzyme, bait, labMeasuredInsertSize,
                positiveControl, negativeControl, devExperimentData, gssrBarcodes, gssrSampleType, doAggregation,
                catNames, productOrder, lcSet, sampleData, labWorkflow, libraryCreationDate, pdoSampleName,
                metadataSourceForPipelineAPI, aggregationDataType);
        if (isCrspLane) {
            crspPipelineUtils.setFieldsForCrsp(libraryBean, sampleData, crspPositiveControlsProject, lcSet,
                    controlProductPartNumber);
        }
        return libraryBean;        
    }

    /**
     * Determines the product part number for a control, depending on the part numbers of other samples in the same
     * lane.
     */
    String getControlProductPartNumber(Set<String> productPartNumbers) {
        String controlProductPartNumber = null;
        for (Set<String> partNumberSet : PART_NUMBER_SETS) {
            Set<String> intersection = new HashSet<>(productPartNumbers);
            intersection.retainAll(partNumberSet);
            if (intersection.size() == productPartNumbers.size()) {
                controlProductPartNumber = partNumberSet.iterator().next();
                break;
            }
        }
        return controlProductPartNumber;
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
