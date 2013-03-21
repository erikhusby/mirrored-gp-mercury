package org.broadinstitute.gpinformatics.mercury.control.zims;

import edu.mit.broad.prodinfo.thrift.lims.IndexPosition;
import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.map.LazyMap;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
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

    // todo jmt do these violate the Athena / Mercury wall?
    private ProductOrderDao productOrderDao;
    private BSPSampleDataFetcher bspSampleDataFetcher;

    @Inject
    public ZimsIlluminaRunFactory(ProductOrderDao productOrderDao, BSPSampleDataFetcher bspSampleDataFetcher) {
        this.productOrderDao = productOrderDao;
        this.bspSampleDataFetcher = bspSampleDataFetcher;
    }

    public ZimsIlluminaRun makeZimsIlluminaRun(SequencingRun sequencingRun) {
        if (!OrmUtil.proxySafeIsInstance(sequencingRun, IlluminaSequencingRun.class)) {
            throw new RuntimeException("Run, " + sequencingRun.getRunName() + ", is not an Illumina run.");
        }
        IlluminaSequencingRun illuminaRun = OrmUtil.proxySafeCast(sequencingRun, IlluminaSequencingRun.class);
        RunCartridge flowcell = illuminaRun.getSampleCartridge();
        Set<SampleInstance> sampleInstances = flowcell.getSampleInstances(true);
        Set<String> sampleIds = new HashSet<String>();
        for (SampleInstance sampleInstance : sampleInstances) {
            sampleIds.add(sampleInstance.getStartingSample().getSampleKey());
        }
        Map<String, BSPSampleDTO> mapSampleIdToDto = bspSampleDataFetcher.fetchSamplesFromBSP(sampleIds);

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
                libraryBeans.addAll(makeLibraryBeans(labVessel, mapSampleIdToDto));
            }
            ZimsIlluminaChamber lane = new ZimsIlluminaChamber(laneNum, libraryBeans, null, null);
            run.addLane(lane);
            laneNum++;
        }

        return run;
    }

    public List<LibraryBean> makeLibraryBeans(LabVessel labVessel, Map<String, BSPSampleDTO> mapSampleIdToDto) {
        List<LibraryBean> libraryBeans = new ArrayList<LibraryBean>();
        // todo jmt reuse the sampleInstances fetched in makeZimsIlluminaRun? Would save a few milliseconds.
        Set<SampleInstance> sampleInstances = labVessel.getSampleInstances(true);
        for (SampleInstance sampleInstance : sampleInstances) {
            ProductOrder productOrder = productOrderDao.findByBusinessKey(sampleInstance.getStartingSample().getProductOrderKey());
            BSPSampleDTO bspSampleDTO = mapSampleIdToDto.get(sampleInstance.getStartingSample().getSampleKey());
            LabBatch labBatch = labVessel.getNearestWorkflowLabBatches().iterator().next(); // TODO: change to use singular version
            String lcSet;
            if (labBatch.getJiraTicket() != null) {
                lcSet = labBatch.getJiraTicket().getTicketId();
            } else {
                throw new RuntimeException("Could not find LCSET for vessel: " + labVessel.getLabel());
            }
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
            if(indexingSchemeEntity != null) {
                Map<IndexPosition, String> positionSequenceMap = new HashMap<IndexPosition, String>();
                for (Map.Entry<MolecularIndexingScheme.IndexPosition, MolecularIndex> indexEntry : indexingSchemeEntity.getIndexes().entrySet()) {
                    String indexName = indexEntry.getKey().toString();
                    positionSequenceMap.put(
                            IndexPosition.valueOf(indexName.substring(indexName.lastIndexOf('_') + 1)),
                            indexEntry.getValue().getSequence());
                }
                indexingSchemeDto = new edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme(
                        indexingSchemeEntity.getName(), positionSequenceMap);
            }
            libraryBeans.add(new LibraryBean(
                    labVessel.getLabel() + (indexingSchemeEntity == null ? "" : "_" + indexingSchemeEntity.getName()),
                    productOrder.getResearchProject().getBusinessKey(), null, null, indexingSchemeDto,
                    null/*todo jmt hasIndexingRead, designation?*/, null, null, null, null, null, null, null, null,
                    null, null, null, null, baitName, null, 0.0, null, null, null, null, null, null,
                    catNames, productOrder, lcSet, bspSampleDTO));
        }
        return libraryBeans;
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
