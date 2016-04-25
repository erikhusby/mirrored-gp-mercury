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

package org.broadinstitute.gpinformatics.athena.control.dao.projects;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassFileType;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionDto;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

@Test(groups = TestGroups.STUBBY)
public class SubmissionTrackerDaoTest extends ContainerTest {
    @Inject
    SubmissionTrackerDao submissionTrackerDao;
    @Inject
    private ResearchProjectDao researchProjectDao;
    private ResearchProject researchProject;

    private static final String TEST_FILE = "/some/test/file";

    private static final String RP_ID = "RP-SubmissionTrackerDaoTest";
    public static final int DEFAULT_VERSION = 1;

    private String sampleName;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        if (submissionTrackerDao == null) {
            return;
        }
        sampleName = "NewSample_" + System.currentTimeMillis();
        researchProject = researchProjectDao.findByBusinessKey(RP_ID);
        if (researchProject == null) {
            researchProject = createResearchProject(RP_ID);
        }
    }

    @AfterMethod
    public void cleanUpTrackers() throws Exception {
        if (submissionTrackerDao == null) {
            return;
        }
    }

    public void testFindSubmissionTrackersNoneExist() throws Exception {
        SubmissionDto submissionDto = getSubmissionDto(RP_ID, sampleName, BassFileType.BAM, 1, TEST_FILE);
        List<SubmissionTracker> submissionTrackers =
                submissionTrackerDao.findSubmissionTrackers(RP_ID, Collections.singleton(submissionDto));
        assertThat(submissionTrackers, emptyCollectionOf(SubmissionTracker.class));
    }

    public void testFindSubmissionTrackersWithResult() throws Exception {
        SubmissionDto submissionDto = getSubmissionDto(RP_ID, sampleName, BassFileType.BAM, DEFAULT_VERSION, null);
        SubmissionTracker submissionTracker = addTracker(submissionDto);

        persistTrackers(Collections.singleton(submissionTracker));

        List<SubmissionTracker> submissionTrackers =
                submissionTrackerDao.findSubmissionTrackers(RP_ID, Collections.singleton(submissionDto));
        assertThat(submissionTrackers, hasSize(1));

        assertThat(submissionTrackers.iterator().next().getTuple(),
                equalTo(submissionDto.getBassDTO().getTuple()));
    }

    public void testFindSubmissionTrackersWithNewVersion() throws Exception {
        SubmissionDto submissionDto1 = getSubmissionDto(RP_ID, sampleName, BassFileType.BAM, DEFAULT_VERSION, null);
        SubmissionTracker submissionTracker1 = addTracker(submissionDto1);

        int newVersion = DEFAULT_VERSION + 1;
        SubmissionDto submissionDto2 = getSubmissionDto(RP_ID, sampleName, BassFileType.BAM, newVersion, null);
        SubmissionTracker submissionTracker2 = addTracker(submissionDto2);

        persistTrackers(Arrays.asList(submissionTracker1, submissionTracker2));

        List<SubmissionTracker> submissionTrackers =
                submissionTrackerDao.findSubmissionTrackers(RP_ID, Collections.singleton(submissionDto1));
        assertThat(submissionTrackers, hasSize(2));

        submissionTracker1 = submissionTrackers.get(0);
        submissionTracker2 = submissionTrackers.get(1);

        // versions should be different.
        assertThat(submissionTracker1.getVersion(), not(equalTo(submissionTracker2.getVersion())));
    }

    public void testFindSubmissionTrackersWithDifferentFileType() throws Exception {
        BassFileType bam = BassFileType.BAM;
        BassFileType picard = BassFileType.PICARD;

        // SubmissionTracker 1
        SubmissionDto submissionDto1 = getSubmissionDto(RP_ID, sampleName, bam, DEFAULT_VERSION, null);
        SubmissionTracker submissionTracker1 = addTracker(submissionDto1);

        // SubmissionTracker 2
        SubmissionDto submissionDto2 = getSubmissionDto(RP_ID, sampleName, picard, DEFAULT_VERSION, null);
        SubmissionTracker submissionTracker2 = addTracker(submissionDto2);

        persistTrackers(Arrays.asList(submissionTracker1, submissionTracker2));

        List<SubmissionTracker> submissionTrackers =
                submissionTrackerDao.findSubmissionTrackers(RP_ID, Collections.singleton(submissionDto1));
        assertThat(submissionTrackers, hasSize(1));
        submissionTracker1 = submissionTrackers.get(0);

        submissionTrackers =
                submissionTrackerDao.findSubmissionTrackers(RP_ID, Collections.singleton(submissionDto2));
        assertThat(submissionTrackers, hasSize(1));
        submissionTracker2 = submissionTrackers.get(0);
        // Tuples should be different.
        assertThat(submissionTracker1.getTuple(), not(equalTo(submissionTracker2.getTuple())));
    }

    public void testFindSubmissionTrackersWithDifferentFileNames() throws Exception {
        // SubmissionTracker 1
        SubmissionDto submissionDto1 =
                getSubmissionDto(RP_ID, sampleName, BassFileType.BAM, DEFAULT_VERSION, TEST_FILE);
        SubmissionTracker submissionTracker = addTracker(submissionDto1);

        // SubmissionTracker 2
        SubmissionDto submissionDto2 =
                getSubmissionDto(RP_ID, sampleName, BassFileType.BAM, DEFAULT_VERSION, TEST_FILE + "/is/not");
        SubmissionTracker submissionTracker2 =
                addTracker(submissionDto2);

        persistTrackers(Arrays.asList(submissionTracker, submissionTracker2));

        List<SubmissionTracker> submissionTrackers =
                submissionTrackerDao.findSubmissionTrackers(RP_ID, Collections.singleton(submissionDto1));
        assertThat(submissionTrackers, hasSize(2));

        SubmissionTracker tracker1 = submissionTrackers.get(0);
        SubmissionTracker tracker2 = submissionTrackers.get(1);

        // Tuples should be equal
        assertThat(tracker1.getTuple(), equalTo(tracker2.getTuple()));
    }


    private void persistTrackers(Collection<SubmissionTracker> submissionTrackers) {
        for (SubmissionTracker tracker : submissionTrackers) {
            submissionTrackerDao.persist(tracker);
        }
        submissionTrackerDao.clear();
    }

    private SubmissionDto getSubmissionDto(ProductOrder productOrder, Map<BassDTO.BassResultColumn, String> bassInfo) {
        return new SubmissionDto(new BassDTO(bassInfo), null, Collections.singleton(productOrder), null);
    }

    private SubmissionDto getSubmissionDto(String productOrderId, final String sampleName,
                                           final BassFileType fileType, final int version, final String path) {
        ProductOrder productOrder = ProductOrderTestFactory.createDummyProductOrder(1, productOrderId);
        return getSubmissionDto(productOrder, new HashMap<BassDTO.BassResultColumn, String>() {{
                    put(BassDTO.BassResultColumn.sample, sampleName);
                    put(BassDTO.BassResultColumn.file_type, fileType == null ? null : fileType.getBassValue());
                    put(BassDTO.BassResultColumn.version, String.valueOf(version));
                    put(BassDTO.BassResultColumn.path, path);
                }}
        );
    }

    private SubmissionTracker addTracker(SubmissionDto submissionDto) {
        SubmissionTracker submissionTracker =
                new SubmissionTracker(submissionDto.getSampleName(), submissionDto.getFileType(),
                        String.valueOf(submissionDto.getVersion()));
        submissionTracker.setResearchProject(researchProject);
        return submissionTracker;
    }

    private ResearchProject createResearchProject(String jiraKey) {
        ResearchProject testResearchProject = ResearchProjectTestFactory.createTestResearchProject();
        testResearchProject.setJiraTicketKey(jiraKey);
        researchProjectDao.persist(testResearchProject);
        researchProjectDao.flush();

        return testResearchProject;
    }
}
