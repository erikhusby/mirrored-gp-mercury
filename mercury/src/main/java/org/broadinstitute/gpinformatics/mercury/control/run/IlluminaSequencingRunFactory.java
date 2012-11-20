package org.broadinstitute.gpinformatics.mercury.control.run;

import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;

import javax.inject.Inject;
import java.io.File;

/**
 * Creates a sequencing run from a JAX-RS DTO
 */
public class IlluminaSequencingRunFactory {
    @Inject
    private IlluminaFlowcellDao illuminaFlowcellDao;

    public IlluminaSequencingRun build(SolexaRunBean solexaRunBean) {
        // todo jmt how to get operator?
        IlluminaFlowcell illuminaFlowcell = illuminaFlowcellDao.findByBarcode(solexaRunBean.getFlowcellBarcode());
        return buildDbFree(solexaRunBean, illuminaFlowcell);
    }
    
    public IlluminaSequencingRun buildDbFree(SolexaRunBean solexaRunBean, IlluminaFlowcell illuminaFlowcell) {
        if (illuminaFlowcell == null) {
            throw new RuntimeException("Flowcell with barcode '" + solexaRunBean.getFlowcellBarcode() + "' does not exist");
        }

        if (solexaRunBean.getRunDate() == null) {
            throw new RuntimeException("runDate must be specified");
        }

        File runDirectory = new File(solexaRunBean.getRunDirectory());
        if (!runDirectory.exists()) {
            throw new RuntimeException("runDirectory '" + solexaRunBean.getRunDirectory() + "' does not exist");
        }

        // todo what about directory path?
        return new IlluminaSequencingRun(illuminaFlowcell, runDirectory.getName(), solexaRunBean.getRunBarcode(),
                solexaRunBean.getMachineName(), null /*TODO SGM  Add call to BspuserService*/,
                false, solexaRunBean.getRunDate());
    }
}
