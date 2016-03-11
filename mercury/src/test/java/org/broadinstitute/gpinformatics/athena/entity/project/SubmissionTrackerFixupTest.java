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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.SubmissionTrackerDao;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionLibraryDescriptor;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionRepository;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.metamodel.SingularAttribute;
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

    public static final String
            ERROR_FORMAT = "Could not update SubmissionTracker for sample '%s' because the '%s' was expected to be null.";


    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false)
    public void fixupGplim4121_BackFillDefaultRepository() throws Exception {
        userBean.loginOSUser();
        SingularAttribute<SubmissionTracker, String> columnName = SubmissionTracker_.submissionRepositoryName;

        List<SubmissionTracker> submissionTrackers = submissionTrackerDao
                .findList(SubmissionTracker.class, columnName, null);
        for (SubmissionTracker submissionTracker : submissionTrackers) {
            if (StringUtils.isNotBlank(submissionTracker.getSubmissionRepositoryName())) {
                String errorMessage =
                        String.format(ERROR_FORMAT, submissionTracker.getSubmittedSampleName(), columnName.getName());
                log.error(errorMessage);
                throw new RuntimeException(errorMessage);
            } else {
                submissionTracker.setSubmissionRepositoryName(SubmissionRepository.DEFAULT_REPOSITORY_NAME);
            }
        }
        submissionTrackerDao
                .persist(new FixupCommentary("see https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-4021"));
        log.info(String.format("Updated %d rows", submissionTrackers.size()));
    }

    @Test(enabled = false)
    public void fixupGplim4121_BackfillDefaultLibraryDescriptor() throws Exception {
        userBean.loginOSUser();
        SingularAttribute<SubmissionTracker, String> columnName = SubmissionTracker_.submissionLibraryDescriptorName;
        List<SubmissionTracker> submissionTrackers = submissionTrackerDao
                .findList(SubmissionTracker.class, columnName, null);

        for (SubmissionTracker submissionTracker : submissionTrackers) {
            if (StringUtils.isNotBlank(submissionTracker.getSubmissionLibraryDescriptorName())) {
                String errorMessage =
                        String.format(ERROR_FORMAT, submissionTracker.getSubmittedSampleName(), columnName.getName());

                log.error(errorMessage);
                throw new RuntimeException(errorMessage);
            } else {
                submissionTracker.setSubmissionRepositoryName(SubmissionLibraryDescriptor.WHOLE_GENOME_NAME);
            }
        }
        submissionTrackerDao
                .persist(new FixupCommentary("see https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-4021"));
        log.info(String.format("Updated %d rows", submissionTrackers.size()));
    }
}
