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
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SequencingTemplateFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria.TraversalDirection.Ancestors;

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

    private static final Log log = LogFactory.getLog(ZimsIlluminaRunFactory.class);

    @Inject
    public ZimsIlluminaRunFactory(SampleDataFetcher sampleDataFetcher,
                                  ControlDao controlDao, SequencingTemplateFactory sequencingTemplateFactory,
                                  ProductOrderDao productOrderDao) {
        this.sampleDataFetcher = sampleDataFetcher;
        this.controlDao = controlDao;
        this.sequencingTemplateFactory = sequencingTemplateFactory;
        this.productOrderDao = productOrderDao;
    }

    public ZimsIlluminaRun makeZimsIlluminaRun(IlluminaSequencingRun illuminaRun) {
        RunCartridge flowcell = illuminaRun.getSampleCartridge();

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
            PipelineTransformationCriteria criteria = new PipelineTransformationCriteria();
            flowcell.getContainerRole().evaluateCriteria(vesselPosition, criteria, Ancestors, null, 0);
            Map<SampleInstanceV2, SampleInstanceV2> laneSampleInstances = new HashMap<>();
            for (SampleInstanceV2 sampleInstanceV2 :
                    flowcell.getContainerRole().getSampleInstancesAtPositionV2(vesselPosition)) {
                laneSampleInstances.put(sampleInstanceV2, sampleInstanceV2);
            }

            for (LabVessel labVessel : criteria.getNearestLabVessels()) {
                Set<SampleInstanceV2> sampleInstances = labVessel.getSampleInstancesV2();
                for (SampleInstanceV2 sampleInstance : sampleInstances) {
                    // Must use equivalent sample instance in the lane, to reflect any rework LCSET since the catch.
                    SampleInstanceV2 laneSampleInstance = laneSampleInstances.get(sampleInstance);
                    if (laneSampleInstance == null) {
                        throw new RuntimeException("Failed to find " + sampleInstance.getMercuryRootSampleName() +
                                " in lane " + laneNum);
                    }
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
                        Set<MercurySample> mercurySamples = importLbsv.getLabVessel().getMercurySamples();
                        if (!mercurySamples.isEmpty()) {
                            sampleId = mercurySamples.iterator().next().getSampleKey();
                        }
                    }
                    sampleIds.add(sampleId);
                    LabVessel libraryVessel = flowcell.getNearestTubeAncestorsForLanes().get(vesselPosition);
                    String libraryName = libraryVessel.getLabel();
                    sampleInstanceDtos
                            .add(new SampleInstanceDto(laneNum, labVessel, laneSampleInstance, sampleId, productOrderKey,
                                                       libraryName, libraryVessel.getCreatedOn(), pdoSampleName));
                }
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
            mapNameToControl.put(activeControl.getCollaboratorSampleId(), activeControl);
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
                        makeLibraryBeans(sampleInstanceDtos, mapSampleIdToDto, mapKeyToProductOrder, mapNameToControl));
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
                                              Map<String, Control> mapNameToControl) {
        List<LibraryBean> libraryBeans = new ArrayList<>();
        Map<String, LibraryBean> mapSampleAndIndexToBean = new HashMap<>();
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
            if (sampleInstance.getSingleInferredBucketedBatch() == null) {
                if (sampleInstance.getSingleBatch() != null) {
                    lcSet = sampleInstance.getSingleBatch().getBatchName();
                }
                // else it is probably a control.
            } else {
                lcSet = sampleInstance.getSingleInferredBucketedBatch().getBatchName();
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

            libraryBeans.add(
                    createLibraryBean(sampleInstanceDto.getLabVessel(), productOrder, sampleData, lcSet, baitName,
                                      indexingSchemeEntity, catNames,
                                      sampleInstanceDto.getSampleInstance().getWorkflowName(),
                                      indexingSchemeDto, mapNameToControl, sampleInstanceDto.getPdoSampleName()));
        }

        // Make order predictable
        Collections.sort(libraryBeans, LibraryBean.BY_SAMPLE_ID);

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
            Map<String, Control> mapNameToControl, String pdoSampleName) {

        Format dateFormat = FastDateFormat.getInstance(ZimsIlluminaRun.DATE_FORMAT);

        String library =
                labVessel.getLabel() + (indexingSchemeEntity == null ? "" : "_" + indexingSchemeEntity.getName());
        String initiative = null;
        Long workRequest = null;
        Boolean hasIndexingRead = null;     // todo jmt hasIndexingRead, designation?
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

        if (sampleData != null && productOrder == null) {
            Control control = mapNameToControl.get(sampleData.getCollaboratorsSampleName());
            if (control != null) {
                switch (control.getType()) {
                case POSITIVE:
                    positiveControl = true;
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
        String analysisType = null;
        String referenceSequence = null;
        String referenceSequenceVersion = null;
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
            String[] referenceSequenceValues;
            if (!StringUtils.isBlank(project.getReferenceSequenceKey())) {
                referenceSequenceValues = project.getReferenceSequenceKey().split("\\|");
                referenceSequence = referenceSequenceValues[0];
                referenceSequenceVersion = referenceSequenceValues[1];
            }

            // horrible 7/25 hack.  todo fixme with workflow
            if (analysisType != null) {
                analysisType = "HybridSelection." + analysisType;
            }
        }
        String libraryCreationDate = dateFormat.format(labVessel.getCreatedOn());

        return new LibraryBean(
                library, initiative, workRequest, indexingSchemeDto, hasIndexingRead, expectedInsertSize,
                analysisType, referenceSequence, referenceSequenceVersion, organism, species,
                strain, aligner, rrbsSizeRange, restrictionEnzyme, bait, labMeasuredInsertSize,
                positiveControl, negativeControl, devExperimentData, gssrBarcodes, gssrSampleType, doAggregation,
                catNames, productOrder, lcSet, sampleData, labWorkflow, libraryCreationDate, pdoSampleName);
    }

    private static class PipelineTransformationCriteria implements TransferTraverserCriteria {

        private Map<Integer, Set<LabVessel>> mapHopToLabVessels = LazyMap.lazyMap(
                new TreeMap<Integer, Set<LabVessel>>(),
                new Factory<Set<LabVessel>>() {
                    @Override
                    public Set<LabVessel> create() {
                        return new HashSet<>();
                    }
                });

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            LabEvent event = context.getEvent();
            if (event == null
                || event.getLabEventType().getPipelineTransformation() == LabEventType.PipelineTransformation.NONE) {
                return TraversalControl.ContinueTraversing;
            }
            // todo jmt in place events?
            for (LabVessel labVessel : event.getTargetLabVessels()) {
                VesselContainer<?> containerRole = labVessel.getContainerRole();
                if (containerRole == null) {
                    mapHopToLabVessels.get(context.getHopCount()).add(labVessel);
                } else {
                    LabVessel vesselAtPosition = containerRole.getVesselAtPosition(context.getVesselPosition());
                    if (vesselAtPosition == null) {
                        if (OrmUtil.proxySafeIsInstance(labVessel, StaticPlate.class)) {
                            vesselAtPosition = new PlateWellTransient((StaticPlate) labVessel, context.getVesselPosition());
                        } else {
                            throw new RuntimeException("No vessel at position " + context.getVesselPosition() + " in " +
                                    labVessel.getLabel());
                        }
                    }
                    mapHopToLabVessels.get(context.getHopCount()).add(vesselAtPosition);
                }
            }
            return TraversalControl.StopTraversing;
        }

        Set<LabVessel> getNearestLabVessels() {
            Set<Map.Entry<Integer, Set<LabVessel>>> entries = mapHopToLabVessels.entrySet();
            Iterator<Map.Entry<Integer, Set<LabVessel>>> iterator = entries.iterator();
            if (iterator.hasNext()) {
                return iterator.next().getValue();
            }
            return Collections.emptySet();
        }

        @Override
        public void evaluateVesselInOrder(Context context) {
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }
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

        public SampleInstanceDto(short laneNumber, LabVessel labVessel, SampleInstanceV2 sampleInstance, String sampleId,
                                 String productOrderKey, String sequencedLibraryName, Date sequencedLibraryDate,
                                 String pdoSampleName) {
            this.laneNumber = laneNumber;
            this.labVessel = labVessel;
            this.sampleInstance = sampleInstance;
            this.sampleId = sampleId;
            this.productOrderKey = productOrderKey;
            this.sequencedLibraryName = sequencedLibraryName;
            this.sequencedLibraryDate = sequencedLibraryDate;
            this.pdoSampleName = pdoSampleName;
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
