package org.broadinstitute.gpinformatics.mercury.control.run;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.LimsQueryObjectFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.LaneReadStructure;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;

import javax.annotation.Nonnull;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.Serializable;
import java.text.MessageFormat;

/**
 * Creates a sequencing run from a JAX-RS DTO.  Implements Serializable because it's used by a Stateful session bean.
 */
@Dependent
public class IlluminaSequencingRunFactory implements Serializable {

    private static final Log log = LogFactory.getLog(IlluminaSequencingRunFactory.class);

    private JiraCommentUtil jiraCommentUtil;

    @Inject
    public IlluminaSequencingRunFactory(JiraCommentUtil jiraCommentUtil) {
        this.jiraCommentUtil = jiraCommentUtil;
    }

    /**
     * storeReadStructureDBFree applies necessary read structure changes to a Sequencing Run based on given information
     *
     * @param readStructureRequest contains all information necessary to search for and update a Sequencing run
     * @param run                  Sequencing Run to apply read structure to.
     *
     * @return a new instance of a readStructureRequest populated with the values as they are found on the run itself
     */
    @DaoFree
    public ReadStructureRequest storeReadsStructureDBFree(ReadStructureRequest readStructureRequest,
                                                           IlluminaSequencingRun run) {

        if (StringUtils.isNotBlank(readStructureRequest.getActualReadStructure())) {
            run.setActualReadStructure(readStructureRequest.getActualReadStructure());
        }

        if (StringUtils.isNotBlank(readStructureRequest.getSetupReadStructure())) {
            run.setSetupReadStructure(readStructureRequest.getSetupReadStructure());
        }

        if (readStructureRequest.getImagedArea() != null) {
            run.setImagedAreaPerMM2(readStructureRequest.getImagedArea());
        }

        if (StringUtils.isNotBlank(readStructureRequest.getLanesSequenced())) {
            run.setLanesSequenced(readStructureRequest.getLanesSequenced());
        }

        for (LaneReadStructure laneReadStructure : readStructureRequest.getLaneStructures()) {
            IlluminaSequencingRunChamber sequencingRunChamber = run.getSequencingRunChamber(
                    laneReadStructure.getLaneNumber());
            if (sequencingRunChamber == null) {
                sequencingRunChamber = new IlluminaSequencingRunChamber(run,
                        laneReadStructure.getLaneNumber());
                run.addSequencingRunChamber(sequencingRunChamber);
            }
            sequencingRunChamber.setActualReadStructure(laneReadStructure.getActualReadStructure());
        }

        return LimsQueryObjectFactory.createReadStructureRequest(run);
    }

    public IlluminaSequencingRun build(SolexaRunBean solexaRunBean, IlluminaFlowcell illuminaFlowcell) {
        IlluminaSequencingRun builtRun = buildDbFree(solexaRunBean, illuminaFlowcell);
        String runName = null;
        String runDirectory = null;
        if (builtRun != null) {
            runName = builtRun.getRunName();
            runDirectory = builtRun.getRunDirectory();
        }
        try {
            jiraCommentUtil.postUpdate(MessageFormat.format("Registered new Solexa run {0} located at {1}",
                    runName,
                    runDirectory),
                    illuminaFlowcell);
        }
        catch(Throwable t) {
            log.error("Failed to log jira run comment for " + runName);
        }
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
                                                /* TODO -- Operator information is always missing.  may revisit later*/,
                false, solexaRunBean.getRunDate(), solexaRunBean.getRunDirectory());
    }
}
