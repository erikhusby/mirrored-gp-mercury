package org.broadinstitute.gpinformatics.mercury.control.zims;

import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

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
        ZimsIlluminaRun run = new ZimsIlluminaRun(sequencingRun.getRunName(), sequencingRun.getRunBarcode(), flowcell.getLabel(), sequencingRun.getMachineName(), null, dateFormat.format(illuminaRun.getRunDate()), null);

        return run;
    }
}
