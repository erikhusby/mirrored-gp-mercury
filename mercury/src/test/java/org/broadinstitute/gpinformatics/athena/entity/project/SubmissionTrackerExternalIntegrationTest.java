/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2018 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.project;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.submission.FileType;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionBioSampleBean;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionConfig;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsService;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class SubmissionTrackerExternalIntegrationTest {
    SubmissionsService submissionsService;

    @BeforeMethod
    public void setUp() {
        submissionsService = new SubmissionsServiceImpl(SubmissionConfig.produce(Deployment.DEV));
    }

    public void testFindOrphanFindsAnOrphan() {
        SubmissionTracker submissionTracker =
            new SubmissionTracker(12345l, "PRJNA420786FOO", "aSample", "1", FileType.BAM,
                SubmissionBioSampleBean.ON_PREM, Aggregation.DATA_TYPE_EXOME);

        List<SubmissionTracker> orphans = submissionsService.findOrphans(Collections.singleton(submissionTracker));

        assertThat(orphans, contains(submissionTracker));
    }
}
