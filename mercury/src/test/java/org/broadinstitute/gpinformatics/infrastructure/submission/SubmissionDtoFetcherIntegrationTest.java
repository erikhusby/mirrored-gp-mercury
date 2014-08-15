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
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.LevelOfDetection;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

@Test(groups = TestGroups.STANDARD)
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


    @BeforeMethod
    public void setUp() throws Exception {
        if(researchProjectDao == null) {
            return;
        }
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if(researchProjectDao == null) {
            return;
        }
    }
    public void testFetch() throws Exception {
        double contamination = 0.0002d;
        LevelOfDetection fingerprintLod = new LevelOfDetection(RESEARCH_PROJECT_ID, COLLABORATOR_SAMPLE_ID, 1, 53.437256, 55.771678);

        ResearchProject researchProject = researchProjectDao.findByBusinessKey(RESEARCH_PROJECT_ID);

        List<SubmissionDto> submissionDtoList = submissionDtoFetcher.fetch(researchProject);

        assertThat(submissionDtoList, is(not(empty())));
        for (SubmissionDto submissionDto : submissionDtoList) {
            assertThat(submissionDto.getSampleName(), equalTo(COLLABORATOR_SAMPLE_ID));
            assertThat(submissionDto.getAggregationProject(), equalTo(RESEARCH_PROJECT_ID));
            assertThat(submissionDto.getDataType(), equalTo(BassDTO.DATA_TYPE_EXOME));
            assertThat(submissionDto.getContamination(), equalTo(contamination));
            assertThat(submissionDto.getResearchProject(), equalTo(RESEARCH_PROJECT_ID));
            assertThat(submissionDto.getFingerprintLOD(), equalTo(fingerprintLod));
            assertThat(submissionDto.getLanesInAggregation(), equalTo(2));
        }
    }
}
