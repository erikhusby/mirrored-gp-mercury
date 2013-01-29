package org.broadinstitute.gpinformatics.mercury.control.zims;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.map.LazyMap;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria.TraversalDirection.Ancestors;

/**
 * @author breilly
 */
public class ZimsIlluminaRunFactory {

    public ZimsIlluminaRun makeZimsIlluminaRun(SequencingRun sequencingRun) {
        if (!OrmUtil.proxySafeIsInstance(sequencingRun, IlluminaSequencingRun.class)) {
            throw new RuntimeException("Run, " + sequencingRun.getRunName() + ", is not an Illumina run.");
        }
        IlluminaSequencingRun illuminaRun = OrmUtil.proxySafeCast(sequencingRun, IlluminaSequencingRun.class);
        IlluminaFlowcell flowcell = illuminaRun.getFlowcell();

        DateFormat dateFormat = new SimpleDateFormat(ZimsIlluminaRun.DATE_FORMAT);
        // TODO: fill in sequencerModel and isPaired
        final ZimsIlluminaRun run = new ZimsIlluminaRun(sequencingRun.getRunName(), sequencingRun.getRunBarcode(), flowcell.getLabel(), sequencingRun.getMachineName(), null, dateFormat.format(illuminaRun.getRunDate()), null);


        Iterator<String> positionNames = flowcell.getVesselGeometry().getPositionNames();
        while (positionNames.hasNext()) {
            String positionName = positionNames.next();
            VesselPosition vesselPosition = VesselPosition.getByName(positionName);
            TransferTraverserCriteria criteria = new LibrariesForIlluminaRunCriteria(run, vesselPosition);
            flowcell.getContainerRole().evaluateCriteria(vesselPosition, criteria, Ancestors, null, 0);
        }

        return run;
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
