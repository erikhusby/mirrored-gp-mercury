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

package org.broadinstitute.gpinformatics.infrastructure.submission;

import com.google.common.collect.Collections2;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

@Test(groups = TestGroups.DATABASE_FREE)
public class ActiveRepositoryPredicateTest {

    public void testActiveRepositories(){
        SubmissionsService submissionsService = new SubmissionsServiceStub();
        List<SubmissionRepository> submissionRepositories = submissionsService.getSubmissionRepositories();

        assertThat(submissionRepositories, hasItem(SubmissionsServiceStub.INACTIVE_REPO));
        assertThat(submissionRepositories, hasItem(SubmissionsServiceStub.ACTIVE_REPO));

        Collection<SubmissionRepository> activeRepositories = Collections2
                .filter(submissionRepositories, SubmissionRepository.activeRepositoryPredicate);

        for (SubmissionRepository activeRepository : activeRepositories) {
            assertThat(activeRepository.isActive(), is(true));
        }
    }

}
