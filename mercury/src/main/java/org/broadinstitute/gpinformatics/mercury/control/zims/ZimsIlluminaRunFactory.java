package org.broadinstitute.gpinformatics.mercury.control.zims;

import edu.mit.broad.prodinfo.thrift.lims.IndexPosition;
import edu.mit.broad.prodinfo.thrift.lims.TZDevExperimentData;
import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.map.LazyMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.DesignedReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;

import javax.inject.Inject;
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
import java.util.TreeMap;

import static org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria.TraversalDirection.Ancestors;

/**
 * This class constructs a pipeline API bean from a Mercury chain of custody.
 * @author breilly
 */
@SuppressWarnings("FeatureEnvy")
public class ZimsIlluminaRunFactory {

    private AthenaClientService athenaClientService;
    private BSPSampleDataFetcher bspSampleDataFetcher;
    private ControlDao controlDao;

    @Inject
    public ZimsIlluminaRunFactory(BSPSampleDataFetcher bspSampleDataFetcher, AthenaClientService athenaClientService,
                                  ControlDao controlDao) {
        this.bspSampleDataFetcher = bspSampleDataFetcher;
        this.athenaClientService = athenaClientService;
        this.controlDao = controlDao;
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
            for (LabVessel labVessel : criteria.getNearestLabVessels()) {
                Set<SampleInstance> sampleInstances =
                        labVessel.getSampleInstances(LabVessel.SampleType.PREFER_PDO, LabBatch.LabBatchType.WORKFLOW);
                for (SampleInstance sampleInstance : sampleInstances) {

                    String productOrderKey = sampleInstance.getProductOrderKey();
                    if (productOrderKey != null) {
                        productOrderKeys.add(productOrderKey);
                    }
                    String pdoSampleName = sampleInstance.getStartingSample().getSampleKey();
                    String sampleId = (sampleInstance.getBspExportSample() != null) ?
                            sampleInstance.getBspExportSample().getSampleKey() : pdoSampleName;
                    sampleIds.add(sampleId);
                    LabVessel libraryVessel = flowcell.getNearestTubeAncestorsForLanes().get(vesselPosition);
                    String libraryName = libraryVessel.getLabel();
                    sampleInstanceDtos
                            .add(new SampleInstanceDto(laneNum, labVessel, sampleInstance, sampleId, productOrderKey,
                                    libraryName, libraryVessel.getCreatedOn(), pdoSampleName));
                }
            }
        }

        Map<String, BSPSampleDTO> mapSampleIdToDto = bspSampleDataFetcher.fetchSamplesFromBSP(sampleIds);
        Map<String, ProductOrder> mapKeyToProductOrder = new HashMap<>();
        for (String productOrderKey : productOrderKeys) {
            mapKeyToProductOrder.put(productOrderKey, athenaClientService.retrieveProductOrderDetails(productOrderKey));
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
                flowcell.getLabel(), illuminaRun.getMachineName(), flowcell.getSequencerModel(), dateFormat.format(illuminaRun.getRunDate()),
                false, illuminaRun.getActualReadStructure(), imagedArea, illuminaRun.getSetupReadStructure(),illuminaRun.getLanesSequenced(),
                illuminaRun.getRunDirectory(), SystemRouter.System.MERCURY);

        for (List<SampleInstanceDto> sampleInstanceDtos : perLaneSampleInstanceDtos) {
            if (sampleInstanceDtos != null && !sampleInstanceDtos.isEmpty()) {
                ArrayList<LibraryBean> libraryBeans = new ArrayList<>();
                short laneNumber = sampleInstanceDtos.get(0).getLaneNumber();
                libraryBeans.addAll(
                        makeLibraryBeans(sampleInstanceDtos, mapSampleIdToDto, mapKeyToProductOrder, mapNameToControl));
               String sequencedLibraryName=sampleInstanceDtos.get(0).getSequencedLibraryName();
                Date sequencedLibraryDate = sampleInstanceDtos.get(0).getSequencedLibraryDate();

                ZimsIlluminaChamber lane = new ZimsIlluminaChamber(laneNumber, libraryBeans, null, sequencedLibraryName, sequencedLibraryDate);
                run.addLane(lane);
            }
        }

        return run;
    }

    @DaoFree
    public List<LibraryBean> makeLibraryBeans(List<SampleInstanceDto> sampleInstanceDtos,
                                              Map<String, BSPSampleDTO> mapSampleIdToDto,
                                              Map<String, ProductOrder> mapKeyToProductOrder,
                                              Map<String, Control> mapNameToControl) {
        List<LibraryBean> libraryBeans = new ArrayList<>();
        for (SampleInstanceDto sampleInstanceDto : sampleInstanceDtos) {
            SampleInstance sampleInstance = sampleInstanceDto.getSampleInstance();
            ProductOrder productOrder = (sampleInstanceDto.getProductOrderKey() != null) ?
                    mapKeyToProductOrder.get(sampleInstanceDto.getProductOrderKey()) : null;

            Set<LabBatch> allLabBatches = new HashSet<>(sampleInstance.getAllLabBatches());
            List<LabBatch> lcSetBatches = new ArrayList<>();
            for (LabBatch labBatch : allLabBatches) {
                if (labBatch.getLabBatchType() == LabBatch.LabBatchType.WORKFLOW) {
                    lcSetBatches.add(labBatch);
                }
            }

            // if there's at least one LCSET batch, and getLabBatch (the singular version) returns null,
            // then throw an exception
            if (lcSetBatches.size() > 1 && sampleInstance.getLabBatch() == null) {
                throw new RuntimeException(
                        String.format("Expected one LabBatch but found %s.", lcSetBatches.size()));
            }
            String lcSet = null;
            if (lcSetBatches.size() == 1) {
                JiraTicket jiraTicket = lcSetBatches.get(0).getJiraTicket();
                if (jiraTicket != null) {
                    lcSet = jiraTicket.getTicketId();
                }
            // else it is probably a control.
            } else {
                if (sampleInstance.getLabBatch() != null) {
                    lcSet = sampleInstance.getLabBatch().getBatchName();
                }
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

            BSPSampleDTO bspSampleDTO = mapSampleIdToDto.get(sampleInstanceDto.getSampleId());

            libraryBeans.add(
                createLibraryBean(sampleInstanceDto.getLabVessel(), productOrder, bspSampleDTO, lcSet, baitName,
                        indexingSchemeEntity, catNames, sampleInstanceDto.getSampleInstance().getWorkflowName(),
                        indexingSchemeDto, mapNameToControl, sampleInstanceDto.getPdoSampleName()));
        }

        // Make order predictable
        Collections.sort(libraryBeans, LibraryBean.BY_SAMPLE_ID);
        return libraryBeans;
    }

    private LibraryBean createLibraryBean(
        LabVessel labVessel, ProductOrder productOrder, BSPSampleDTO bspSampleDTO, String lcSet, String baitName,
        MolecularIndexingScheme indexingSchemeEntity, List<String> catNames, String labWorkflow,
        edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme indexingSchemeDto,
        Map<String, Control> mapNameToControl, String pdoSampleName) {

        String library = labVessel.getLabel() + (indexingSchemeEntity == null ? "" : "_" + indexingSchemeEntity.getName());
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

        if (bspSampleDTO != null) {
            Control control = mapNameToControl.get(bspSampleDTO.getCollaboratorsSampleName());
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

        return new LibraryBean(
                library, initiative, workRequest, indexingSchemeDto, hasIndexingRead, expectedInsertSize,
                analysisType, referenceSequence, referenceSequenceVersion, organism, species,
                strain, aligner, rrbsSizeRange, restrictionEnzyme, bait, labMeasuredInsertSize,
                positiveControl, negativeControl, devExperimentData, gssrBarcodes, gssrSampleType, doAggregation,
                catNames, productOrder, lcSet, bspSampleDTO, labWorkflow, pdoSampleName);
    }

    private static class PipelineTransformationCriteria implements TransferTraverserCriteria {

        private Map<Integer, Set<LabVessel>> mapHopToLabVessels = LazyMap.decorate(
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
            if (event == null || event.getLabEventType().getPipelineTransformation() == LabEventType.PipelineTransformation.NONE) {
                return TraversalControl.ContinueTraversing;
            }
            // todo jmt in place events?
            for (LabVessel labVessel : event.getTargetLabVessels()) {
                VesselContainer<?> containerRole = labVessel.getContainerRole();
                if(containerRole == null) {
                    mapHopToLabVessels.get(context.getHopCount()).add(labVessel);
                } else {
                    mapHopToLabVessels.get(context.getHopCount()).addAll(containerRole.getContainedVessels());
                }
            }
            return TraversalControl.StopTraversing;
        }

        Set<LabVessel> getNearestLabVessels() {
            Set<Map.Entry<Integer, Set<LabVessel>>> entries = mapHopToLabVessels.entrySet();
            Iterator<Map.Entry<Integer, Set<LabVessel>>> iterator = entries.iterator();
            if(iterator.hasNext()) {
                return iterator.next().getValue();
            }
            return Collections.emptySet();
        }

        @Override
        public void evaluateVesselInOrder(Context context) {}

        @Override
        public void evaluateVesselPostOrder(Context context) {}
    }

    /**
     * Need DTO to store choice of aliquot or root sample ID.
     */
    static class SampleInstanceDto {
        private short laneNumber;
        private LabVessel labVessel;
        private SampleInstance sampleInstance;
        private String sampleId;
        private String productOrderKey;
        private String sequencedLibraryName;
        private Date sequencedLibraryDate;
        private String pdoSampleName;

        public SampleInstanceDto(short laneNumber, LabVessel labVessel, SampleInstance sampleInstance, String sampleId,
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

        public SampleInstance getSampleInstance() {
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
