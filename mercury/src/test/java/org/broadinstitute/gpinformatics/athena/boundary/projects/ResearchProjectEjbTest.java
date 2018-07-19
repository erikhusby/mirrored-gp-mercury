package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.SubmissionTrackerDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker;
import org.broadinstitute.gpinformatics.infrastructure.cognos.entity.PicardAggregationSample;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.AggregationAlignment;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.AggregationReadGroup;
import org.broadinstitute.gpinformatics.infrastructure.submission.FileType;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionDto;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionStatusDetailBean;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

@Test(groups = TestGroups.STANDARD)
public class ResearchProjectEjbTest extends Arquillian {
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Inject
    ResearchProjectEjb researchProjectEjb;
    @Inject
    private SubmissionTrackerDao submissionTrackerDao;
    @Inject
    private ResearchProjectDao researchProjectDao;

    @BeforeMethod
    public void setUp() throws Exception {
        if (submissionTrackerDao == null) {
            return;
        }
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (submissionTrackerDao == null) {
            return;
        }
    }

    public void testSubmitRollbackOnError() throws Exception {
        String randomName = Double.toString(Math.random());
        String sampleName = "PD_030T";
        int version = 1;
        String location = "OnPrem";
        String dataType = "Whole Exome";
        String project = "G102647";
        String rpName = "DUMMY-" + randomName;

        ResearchProject dummy = ResearchProjectTestFactory
            .createDummyResearchProject(ResearchProjectTestFactory.TEST_CREATOR, "MyResearchProject" + randomName,
                "To Study Stuff", ResearchProject.IRB_ENGAGED);

        dummy.setJiraTicketKey(rpName);
        researchProjectDao.persist(dummy);
        SubmissionTracker submissionTracker =
            new SubmissionTracker(dummy.getJiraTicketKey(), sampleName, Integer.toString(version), FileType.BAM,
                location, dataType);
        dummy.addSubmissionTracker(submissionTracker);
        submissionTrackerDao.persist(submissionTracker);

        Map<SubmissionTracker, SubmissionDto> submissionDtoMap = new HashMap<>();
        PicardAggregationSample picardAggregationSample =
            new PicardAggregationSample(dummy.getBusinessKey(), project, "pdo-1234", sampleName, dataType);
        submissionDtoMap.put(submissionTracker, new SubmissionDto(
            new Aggregation(project, sampleName, dataType, version, 1, dataType,
                Collections.<AggregationAlignment>emptySet(), null, null, Collections.<AggregationReadGroup>emptySet(),
                null, null, picardAggregationSample, location), null
        ));
        Collection<SubmissionStatusDetailBean> submissionResults = new HashSet<>();
        List<String> errors = new ArrayList<>();

        researchProjectEjb.updateAndPersistSubmissionDtoStatusFromResults(dummy, submissionDtoMap, submissionResults,
                SubmissionTracker.uuidMap(Collections.singletonList(submissionTracker)), errors);
        assertThat(dummy.getSubmissionTrackers(), hasSize(0));

        List<SubmissionTracker> submissionTrackers =
            submissionTrackerDao.findSubmissionTrackers(Collections.singleton(submissionTracker));
        assertThat(submissionTrackers, hasSize(0));

        researchProjectDao.remove(dummy);
    }

}
