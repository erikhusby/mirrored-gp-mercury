package org.broadinstitute.gpinformatics.mercury.control.zims;

import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

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

        TransferTraverserCriteria criteria = new TransferTraverserCriteria() {
            @Override
            public TraversalControl evaluateVesselPreOrder(Context context) {
                if (context.getHopCount() > 0) {
                    run.addLane(new ZimsIlluminaChamber((short) 1, new ArrayList<LibraryBean>(), null, null));
                    return TraversalControl.StopTraversing;
                }
                return TraversalControl.ContinueTraversing;
            }

            @Override
            public void evaluateVesselInOrder(Context context) {}

            @Override
            public void evaluateVesselPostOrder(Context context) {}
        };

        for (VesselPosition vesselPosition : flowcell.getContainerRole().getPositions()) {

//            flowcell.getContainerRole().evaluateCriteria(vesselPosition, criteria, Ancestors);
        }
        flowcell.evaluateCriteria(criteria, Ancestors);

        return run;
    }
}
