/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.metrics.LevelOfDetection;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.hamcrest.Matchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class SubmissionDtoFetcherIntegrationTest extends Arquillian {
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Inject
    private ResearchProjectDao researchProjectDao;
    @Inject
    private SubmissionDtoFetcher submissionDtoFetcher;

    public static final String COLLABORATOR_SAMPLE_ID = "NA12878";
    public static final String RESEARCH_PROJECT_ID = "RP-697";

    public void testFetch() throws Exception {
        Date dateCompleted = DateUtils.parseDate( "yyyy-MM-dd HH:mm:ss.S", "2014-06-04 22:12:49.0");
        double contamination = 2.2d;
        LevelOfDetection fingerprintLod = new LevelOfDetection(53.437256, 55.771678);

        ResearchProject researchProject = researchProjectDao.findByBusinessKey(RESEARCH_PROJECT_ID);

        List<SubmissionDto> submissionDtoList = submissionDtoFetcher.fetch(researchProject, 1);

        assertThat(submissionDtoList, is(not(empty())));
        for (SubmissionDto submissionDto : submissionDtoList) {
            assertThat(submissionDto.getSampleName(), equalTo(COLLABORATOR_SAMPLE_ID));
            assertThat(submissionDto.getAggregationProject(), equalTo(RESEARCH_PROJECT_ID));
            assertThat(submissionDto.getResearchProject(), equalTo(RESEARCH_PROJECT_ID));
            assertThat(submissionDto.getFingerprintLOD(), equalTo(fingerprintLod));
            assertThat(submissionDto.getLanesInAggregation(), Matchers.equalTo(2));
            assertThat(dateCompleted, Matchers.equalTo(submissionDto.getDateCompleted()));
        }


    }
}
