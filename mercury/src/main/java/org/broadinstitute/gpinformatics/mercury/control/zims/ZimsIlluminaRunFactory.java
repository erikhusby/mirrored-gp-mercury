package org.broadinstitute.gpinformatics.mercury.control.zims;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.map.LazyMap;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;

import javax.inject.Inject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria.TraversalDirection.Ancestors;

/**
 * @author breilly
 */
public class ZimsIlluminaRunFactory {

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

        DateFormat dateFormat = new SimpleDateFormat(ZimsIlluminaRun.DATE_FORMAT);
        // TODO: fill in sequencerModel and isPaired
        final ZimsIlluminaRun run = new ZimsIlluminaRun(sequencingRun.getRunName(), sequencingRun.getRunBarcode(), flowcell.getLabel(), sequencingRun.getMachineName(), null, dateFormat.format(illuminaRun.getRunDate()), false, null, 0);


        Iterator<String> positionNames = flowcell.getVesselGeometry().getPositionNames();
        while (positionNames.hasNext()) {
            String positionName = positionNames.next();
            VesselPosition vesselPosition = VesselPosition.getByName(positionName);
            TransferTraverserCriteria criteria = new LibrariesForIlluminaRunCriteria(run, vesselPosition);
            flowcell.getContainerRole().evaluateCriteria(vesselPosition, criteria, Ancestors, null, 0);
        }

        return run;
    }

    public LibraryBean makeLibraryBean(LabVessel labVessel) {
        String productOrderKey = labVessel.getNearestProductOrders().iterator().next(); // TODO: use singular version
        ProductOrder productOrder = productOrderDao.findByBusinessKey(productOrderKey);
        Set<SampleInstance> sampleInstances = labVessel.getSampleInstances();
        if (sampleInstances.size() > 1) {
            throw new RuntimeException("Cannot currently handle vessels with more than one sample");
        }
        SampleInstance sampleInstance = sampleInstances.iterator().next();
        BSPSampleDTO bspSampleDTO = bspSampleDataFetcher.fetchSingleSampleFromBSP(sampleInstance.getStartingSample().getSampleKey());
        LabBatch labBatch = labVessel.getNearestLabBatches().iterator().next(); // TODO: change to use singular version
        String lcSet;
        if (labBatch.getJiraTicket() != null) {
            lcSet = labBatch.getJiraTicket().getTicketId();
        } else {
            throw new RuntimeException("Could not find LCSET for vessel: " + labVessel.getLabel());
        }
        LibraryBean libraryBean = new LibraryBean(labVessel.getLabel(), productOrder.getResearchProject().getBusinessKey(), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, null, null, null, null, null, null, null, productOrder, lcSet, bspSampleDTO);
        return libraryBean;
    }

    private static class LibrariesForIlluminaRunCriteria implements TransferTraverserCriteria {
        private final ZimsIlluminaRun run;
        private VesselPosition lane;
        private Map<LabEventType, Set<LabVessel>> eventTypeToTube = LazyMap.decorate(new HashMap<LabEventType, Set<LabVessel>>(), new Factory<Set<LabVessel>>() {
            @Override
            public Set<LabVessel> create() {
                return new HashSet<LabVessel>();
            }
        });
        private LibraryBean libraryBean;

        public LibrariesForIlluminaRunCriteria(ZimsIlluminaRun run, VesselPosition lane) {
            this.run = run;
            this.lane = lane;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            LabEvent event = context.getEvent();
            if (event != null) {
                for (LabVessel labVessel : event.getTargetLabVessels()) {
                    if (OrmUtil.proxySafeIsInstance(labVessel, TwoDBarcodedTube.class)) {
                        eventTypeToTube.get(event.getLabEventType()).add(labVessel);
                    } else if (OrmUtil.proxySafeIsInstance(labVessel, TubeFormation.class)) {
                        TubeFormation tubeFormation = OrmUtil.proxySafeCast(labVessel, TubeFormation.class);
                        eventTypeToTube.get(event.getLabEventType()).addAll(tubeFormation.getContainerRole().getContainedVessels());
                    }
                }
            }
            if (context.getHopCount() > 0) {
//                ZimsIlluminaChamber lane = new ZimsIlluminaChamber((short) context.getVesselPosition(), new ArrayList<LibraryBean>(), null, null);
//                run.addLane(lane);
                return TraversalControl.StopTraversing;
            }
            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselInOrder(Context context) {}

        @Override
        public void evaluateVesselPostOrder(Context context) {}
    }
}
