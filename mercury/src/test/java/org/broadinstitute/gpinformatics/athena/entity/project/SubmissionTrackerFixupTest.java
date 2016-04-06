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

import org.broadinstitute.gpinformatics.athena.control.dao.projects.SubmissionTrackerDao;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassFileType;
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
    private UserBean userBean;
    @Inject
    private SubmissionTrackerDao submissionTrackerDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false)
    public void gplim4091BackfillFileType() throws Exception {
        userBean.loginOSUser();

        List<SubmissionTracker> submissionTrackerList =
                submissionTrackerDao.findList(SubmissionTracker.class, SubmissionTracker_.fileType, null);

        BassFileType defaultFileType = BassFileType.BAM;

        for (SubmissionTracker submissionTracker : submissionTrackerList) {
            if (submissionTracker.getFileType() != null) {
                throw new RuntimeException(
                        String.format("Expected SubmissionTracker %s to have null value but it was %s",
                                submissionTracker.createSubmissionIdentifier(), submissionTracker.getFileType()));
            } else {
                submissionTracker.setFileType(defaultFileType);
            }
        }

        submissionTrackerDao.persist(new FixupCommentary(
                "Backfill fileTypes for existing SubmissionTrackers. See https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-4060"));
    }

    @Test(enabled = false)
    public void gplim4086RemoveDuplicateSubmissions() {
        userBean.loginOSUser();

        long[] ids = new long[] { 1, 951, 952, 2952, 2983 };

        for (long id : ids) {
            SubmissionTracker tracker = submissionTrackerDao.findById(SubmissionTracker.class, id);
            submissionTrackerDao.remove(tracker);
        }

        submissionTrackerDao.persist(new FixupCommentary("GPLIM-4086 removed duplicate submissions"));
    }
}
