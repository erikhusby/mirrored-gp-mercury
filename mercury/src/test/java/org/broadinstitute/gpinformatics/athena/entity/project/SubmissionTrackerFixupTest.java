/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2016 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.SubmissionTrackerDao;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.FIXUP)
public class SubmissionTrackerFixupTest extends Arquillian {
    @Inject
    SubmissionTrackerDao submissionTrackerDao;

    @Inject
    private UserBean userBean;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private Log log;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false)
    public void fixupGplim4121_BackFillFileType() throws Exception {
        userBean.loginOSUser();

        List<SubmissionTracker> submissionTrackers =
                submissionTrackerDao.findList(SubmissionTracker.class, SubmissionTracker_.fileType, null);
        for (SubmissionTracker submissionTracker : submissionTrackers) {
            if (submissionTracker.getFileType() != null) {
                String errorMessage =
                        String.format(
                                "Could not update SubmissionTracker for sample '%s' because the fileType was expected to be null but is %s",
                                submissionTracker.getSubmittedSampleName(), submissionTracker.getFileType());
                log.error(errorMessage);
                throw new RuntimeException(errorMessage);
            } else {
                submissionTracker.setFileType(BassDTO.FileType.BAM);
            }
        }
        submissionTrackerDao
                .persist(new FixupCommentary("Backfill SubmissionTracker fileType. See https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-4021"));
        log.info(String.format("Updated %d rows", submissionTrackers.size()));
    }

}
