package org.broadinstitute.gpinformatics.mercury.control.zims;

import edu.mit.broad.prodinfo.thrift.lims.IndexPosition;
import edu.mit.broad.prodinfo.thrift.lims.TZDevExperimentData;
import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.map.LazyMap;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.DesignedReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;

import javax.inject.Inject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

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

    public ZimsIlluminaRun makeZimsIlluminaRun(SequencingRun sequencingRun) {
        if (!OrmUtil.proxySafeIsInstance(sequencingRun, IlluminaSequencingRun.class)) {
            throw new RuntimeException("Run, " + sequencingRun.getRunName() + ", is not an Illumina run.");
        }
        IlluminaSequencingRun illuminaRun = OrmUtil.proxySafeCast(sequencingRun, IlluminaSequencingRun.class);
        RunCartridge flowcell = illuminaRun.getSampleCartridge();

        // Fetch samples and product orders
        Set<SampleInstance> sampleInstances = flowcell.getSampleInstances(LabVessel.SampleType.PREFER_PDO,
                LabBatch.LabBatchType.WORKFLOW);
        Set<String> sampleIds = new HashSet<String>();
        Set<String> productOrderKeys = new HashSet<String>();
        for (SampleInstance sampleInstance : sampleInstances) {
            sampleIds.add(sampleInstance.getStartingSample().getSampleKey());
            if (sampleInstance.getProductOrderKey() != null) {
                productOrderKeys.add(sampleInstance.getProductOrderKey());
            }
        }
        Map<String, BSPSampleDTO> mapSampleIdToDto = bspSampleDataFetcher.fetchSamplesFromBSP(sampleIds);
        Map<String, ProductOrder> mapKeyToProductOrder = new HashMap<String, ProductOrder>();
        for (String productOrderKey : productOrderKeys) {
            mapKeyToProductOrder.put(productOrderKey, athenaClientService.retrieveProductOrderDetails(productOrderKey));
        }

        List<Control> activeControls = controlDao.findAllActive();
        Map<String, Control> mapNameToControl = new HashMap<String, Control>();
        for (Control activeControl : activeControls) {
            mapNameToControl.put(activeControl.getCollaboratorSampleId(), activeControl);
        }

        DateFormat dateFormat = new SimpleDateFormat(ZimsIlluminaRun.DATE_FORMAT);
        // TODO: fill in sequencerModel and isPaired
        ZimsIlluminaRun run = new ZimsIlluminaRun(sequencingRun.getRunName(), sequencingRun.getRunBarcode(),
                flowcell.getLabel(), sequencingRun.getMachineName(), null, dateFormat.format(illuminaRun.getRunDate()),
                false, null, 0.0);

        Iterator<String> positionNames = flowcell.getVesselGeometry().getPositionNames();
        short laneNum = 1;
        while (positionNames.hasNext()) {
            String positionName = positionNames.next();
            VesselPosition vesselPosition = VesselPosition.getByName(positionName);
            PipelineTransformationCriteria criteria = new PipelineTransformationCriteria();
            flowcell.getContainerRole().evaluateCriteria(vesselPosition, criteria, Ancestors, null, 0);
            ArrayList<LibraryBean> libraryBeans = new ArrayList<LibraryBean>();
            for (LabVessel labVessel : criteria.getNearestLabVessels()) {
                libraryBeans.addAll(makeLibraryBeans(labVessel, mapSampleIdToDto, mapKeyToProductOrder, mapNameToControl));
            }
            ZimsIlluminaChamber lane = new ZimsIlluminaChamber(laneNum, libraryBeans, null, null);
            run.addLane(lane);
            laneNum++;
        }

        return run;
    }

    @DaoFree
    public List<LibraryBean> makeLibraryBeans(LabVessel labVessel, Map<String, BSPSampleDTO> mapSampleIdToDto,
            Map<String, ProductOrder> mapKeyToProductOrder, Map<String, Control> mapNameToControl) {
        List<LibraryBean> libraryBeans = new ArrayList<LibraryBean>();
        // todo jmt reuse the sampleInstances fetched in makeZimsIlluminaRun? Would save a few milliseconds.
        Set<SampleInstance> sampleInstances = labVessel.getSampleInstances(LabVessel.SampleType.PREFER_PDO,
                LabBatch.LabBatchType.WORKFLOW);
        for (SampleInstance sampleInstance : sampleInstances) {
            ProductOrder productOrder = null;
            if (sampleInstance.getProductOrderKey() != null) {
                productOrder = mapKeyToProductOrder.get(sampleInstance.getProductOrderKey());
            }

            LabBatch labBatch = sampleInstance.getLabBatch();
            String lcSet = null;
            if (labBatch != null && labBatch.getJiraTicket() != null) {
                lcSet = labBatch.getJiraTicket().getTicketId();
            }

            // This loop goes through all the reagents and takes the last bait name (under the assumption that
            // the lab would only ever have one for this sample instance. All cat names are collected and the
            // last indexing scheme reagent.
            MolecularIndexingScheme indexingSchemeEntity = null;
            String baitName = null;
            List<String> catNames = new ArrayList<String>();
            for (Reagent reagent : sampleInstance.getReagents()) {
                if (OrmUtil.proxySafeIsInstance(reagent, MolecularIndexReagent.class)) {
                    indexingSchemeEntity = OrmUtil.proxySafeCast(reagent, MolecularIndexReagent.class).getMolecularIndexingScheme();
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
                Map<IndexPosition, String> positionSequenceMap = new HashMap<IndexPosition, String>();
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

            BSPSampleDTO bspSampleDTO = mapSampleIdToDto.get(sampleInstance.getStartingSample().getSampleKey());

            libraryBeans.add(
                createLibraryBean(labVessel, productOrder, bspSampleDTO, lcSet, baitName,
                                  indexingSchemeEntity, catNames, indexingSchemeDto, mapNameToControl));
        }

        return libraryBeans;
    }

    private LibraryBean createLibraryBean(
        LabVessel labVessel, ProductOrder productOrder, BSPSampleDTO bspSampleDTO, String lcSet, String baitName,
        MolecularIndexingScheme indexingSchemeEntity, List<String> catNames,
        edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme indexingSchemeDto, Map<String, Control> mapNameToControl) {

        String library = labVessel.getLabel() + (indexingSchemeEntity == null ? "" : "_" + indexingSchemeEntity.getName());

        String projectName = null;
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
        Boolean doAggregation = null;

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
            projectName = project.getBusinessKey();
            aligner = project.getSequenceAlignerKey();

            // If there is a reference sequence value on the project, then populate the name and version.
            String[] referenceSequenceValues = null;
            if (!StringUtils.isBlank(project.getReferenceSequenceKey())) {
                referenceSequenceValues = project.getReferenceSequenceKey().split("\\|");
                referenceSequence = referenceSequenceValues[0];
                referenceSequenceVersion = referenceSequenceValues[1];
            }
        }

        return new LibraryBean(
                library, projectName, initiative, workRequest, indexingSchemeDto, hasIndexingRead, expectedInsertSize,
                analysisType, referenceSequence, referenceSequenceVersion, organism, species,
                strain, aligner, rrbsSizeRange, restrictionEnzyme, bait, labMeasuredInsertSize,
                positiveControl, negativeControl, devExperimentData, gssrBarcodes, gssrSampleType, doAggregation,
                catNames, productOrder, lcSet, bspSampleDTO);
    }

    private static class PipelineTransformationCriteria implements TransferTraverserCriteria {

        private Map<Integer, Set<LabVessel>> mapHopToLabVessels = LazyMap.decorate(
                new TreeMap<Integer, Set<LabVessel>>(),
                new Factory<Set<LabVessel>>() {
            @Override
            public Set<LabVessel> create() {
                return new HashSet<LabVessel>();
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
}
