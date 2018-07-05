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
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.LevelOfDetection;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.hamcrest.Matchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

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
    public static final String RP_518 = "RP-518";

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
        double contamination = 0.0003d;
        double lodMin = 17.926603;
        double lodMax = 55.771678;
        int version = 2;
        LevelOfDetection fingerprintLod =
                new LevelOfDetection(lodMin, lodMax);
        ResearchProject researchProject = researchProjectDao.findByBusinessKey(RESEARCH_PROJECT_ID);

        List<SubmissionDto> submissionDtoList = submissionDtoFetcher.fetch(researchProject, MessageReporter.UNUSED);

        assertThat(submissionDtoList, is(not(empty())));
        for (SubmissionDto submissionDto : submissionDtoList) {
            assertThat(submissionDto.getVersion(), equalTo(version));
            assertThat(submissionDto.getSampleName(), equalTo(COLLABORATOR_SAMPLE_ID));
            assertThat(submissionDto.getAggregationProject(), equalTo(RESEARCH_PROJECT_ID));
            assertThat(submissionDto.getDataType(),
                equalTo(SubmissionLibraryDescriptor.getNormalizedLibraryName(Aggregation.DATA_TYPE_EXOME)));
            assertThat(submissionDto.getContamination(), equalTo(contamination));
            assertThat(submissionDto.getResearchProject(), equalTo(RESEARCH_PROJECT_ID));
            assertThat(String.format("expected LOD min to be %f but was %f", lodMin,
                    submissionDto.getFingerprintLOD().getMin()),
                    submissionDto.getFingerprintLOD().getMin(), equalTo(lodMin));
            assertThat(String.format("expected LOD max to be %f but was %f", lodMax,
                            submissionDto.getFingerprintLOD().getMax()),
                    submissionDto.getFingerprintLOD().getMax(), equalTo(lodMax));
            assertThat(submissionDto.getFingerprintLOD(), equalTo(fingerprintLod));
            assertThat(submissionDto.getLanesInAggregation(), equalTo(22));
        }
    }

    public void testFetchSquidProject() {
        ResearchProject researchProject = researchProjectDao.findByBusinessKey(RP_518);
        List<SubmissionDto> submissionDtoList = submissionDtoFetcher.fetch(researchProject, MessageReporter.UNUSED);

        assertThat(submissionDtoList, is(not(empty())));
        boolean matchedSample=false;
        for (SubmissionDto submissionDto : submissionDtoList) {
            if (submissionDto.getSampleName().equals(COLLABORATOR_SAMPLE_ID)) {
                assertThat(submissionDto.getAggregationProject(),
                    Matchers.either(startsWith("C")).or(startsWith("G")).or(startsWith("D")));
                matchedSample = true;
            }
            assertThat(submissionDto.getResearchProject(), equalTo(RP_518));
        }
        assertThat(matchedSample, is(true));
    }
}
