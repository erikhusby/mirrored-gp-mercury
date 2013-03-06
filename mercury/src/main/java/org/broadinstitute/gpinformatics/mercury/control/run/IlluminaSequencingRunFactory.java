package org.broadinstitute.gpinformatics.mercury.control.run;

import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.File;
import java.io.Serializable;

/**
 * Creates a sequencing run from a JAX-RS DTO.  Implements Serializable because it's used by a Stateful session bean.
 */
public class IlluminaSequencingRunFactory implements Serializable {
    @Inject
    private IlluminaFlowcellDao illuminaFlowcellDao;

    public IlluminaSequencingRun build(SolexaRunBean solexaRunBean) {
        // todo jmt how to get operator? -- SGM -- can't, will always be run from a machine.
        IlluminaFlowcell illuminaFlowcell = illuminaFlowcellDao.findByBarcode(solexaRunBean.getFlowcellBarcode());
        return buildDbFree(solexaRunBean, illuminaFlowcell);
    }

    @DaoFree
    public IlluminaSequencingRun buildDbFree(@Nonnull SolexaRunBean solexaRunBean, @Nonnull IlluminaFlowcell illuminaFlowcell) {

        if (solexaRunBean.getRunDate() == null) {
            throw new RuntimeException("runDate must be specified");
        }

        File runDirectory = new File(solexaRunBean.getRunDirectory());
        if (!runDirectory.exists()) {
            throw new RuntimeException("runDirectory '" + solexaRunBean.getRunDirectory() + "' does not exist");
        }

        // todo what about directory path?
        return new IlluminaSequencingRun(illuminaFlowcell, runDirectory.getName(), solexaRunBean.getRunBarcode(),
                solexaRunBean.getMachineName(), null /*TODO Add call to ServiceAccessUtility.getBean ( BSPUserList.class );

                */,
                false, solexaRunBean.getRunDate());
    }
}
