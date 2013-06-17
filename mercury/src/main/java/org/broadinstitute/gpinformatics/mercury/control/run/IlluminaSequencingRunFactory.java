package org.broadinstitute.gpinformatics.mercury.control.run;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
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

    /**
     * storeReadStructureDBFree applies necessary read structure changes to a Sequencing Run based on given information
     *
     * @param readStructureRequest contains all information necessary to searching for and update a Sequencing run
     * @param run                  Sequencing Run to apply read structure to.
     *
     * @return a new instance of a readStructureRequest populated with the values as they are found on the run itself
     */
    @DaoFree
    public ReadStructureRequest storeReadsStructureDBFree(ReadStructureRequest readStructureRequest,
                                                           SequencingRun run) {

        if (StringUtils.isBlank(readStructureRequest.getActualReadStructure()) &&
            StringUtils.isBlank(readStructureRequest.getSetupReadStructure())) {
            throw new ResourceException("Neither the actual nor the setup read structures are set",
                    Response.Status.BAD_REQUEST);
        }

        if (StringUtils.isNotBlank(readStructureRequest.getActualReadStructure())) {
            run.setActualReadStructure(readStructureRequest.getActualReadStructure());
        }

        if (StringUtils.isNotBlank(readStructureRequest.getSetupReadStructure())) {
            run.setSetupReadStructure(readStructureRequest.getSetupReadStructure());
        }

        ReadStructureRequest returnValue = new ReadStructureRequest();
        returnValue.setRunBarcode(run.getRunBarcode());

        returnValue.setActualReadStructure(run.getActualReadStructure());
        returnValue.setSetupReadStructure(run.getSetupReadStructure());
        return returnValue;
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
                false, solexaRunBean.getRunDate(), solexaRunBean.getRunDirectory());
    }
}
