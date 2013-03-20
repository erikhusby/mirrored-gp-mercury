package org.broadinstitute.gpinformatics.mercury.control.run;

import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.OutputDataLocation;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.File;
import java.io.Serializable;
import java.text.MessageFormat;

/**
 * Creates a sequencing run from a JAX-RS DTO.  Implements Serializable because it's used by a Stateful session bean.
 */
public class IlluminaSequencingRunFactory implements Serializable {

    private JiraCommentUtil jiraCommentUtil;

    @Inject
    public IlluminaSequencingRunFactory(JiraCommentUtil jiraCommentUtil) {
        this.jiraCommentUtil = jiraCommentUtil;
    }

    public IlluminaSequencingRun build(SolexaRunBean solexaRunBean, IlluminaFlowcell illuminaFlowcell) {
        IlluminaSequencingRun builtRun = buildDbFree(solexaRunBean, illuminaFlowcell);
        jiraCommentUtil.postUpdate(MessageFormat.format("Registered new Solexa run {0} located at {1}",
                                                               builtRun.getRunName(),
                                                               builtRun.getRunDirectory()),
                                          illuminaFlowcell);

        return builtRun;
    }

    @DaoFree
    public IlluminaSequencingRun buildDbFree(@Nonnull SolexaRunBean solexaRunBean,
                                             @Nonnull IlluminaFlowcell illuminaFlowcell) {

        if (solexaRunBean.getRunDate() == null) {
            throw new RuntimeException("runDate must be specified");
        }

        String runName = new File(solexaRunBean.getRunDirectory()).getName();

        return new IlluminaSequencingRun(illuminaFlowcell, runName, solexaRunBean.getRunBarcode(),
                                                solexaRunBean.getMachineName(),
                                                null
                                                /* TODO SGM -- Operator information is always missing.  may revisit later*/,
                                                false, solexaRunBean.getRunDate(), null, solexaRunBean.getRunDirectory());
    }
}
