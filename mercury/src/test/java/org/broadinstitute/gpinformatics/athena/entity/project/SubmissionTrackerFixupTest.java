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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.SubmissionTrackerDao;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionBioSampleBean;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionDto;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionDtoFetcher;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionLibraryDescriptor;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionStatusDetailBean;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsService;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.FIXUP)
public class SubmissionTrackerFixupTest extends Arquillian {
    @Inject
    private SubmissionTrackerDao submissionTrackerDao;

    @Inject
    private SubmissionDtoFetcher submissionDtoFetcher;

    @Inject
    private SubmissionsService submissionService;

    @Inject
    private UserBean userBean;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private Log log;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

// This test was commented to prevent compilation issues due to data submission api changes.
//    @Test(enabled = false)
//    public void gplim4091BackfillFileType() throws Exception {
//        userBean.loginOSUser();
//
//        List<SubmissionTracker> submissionTrackerList =
//                submissionTrackerDao.findList(SubmissionTracker.class, SubmissionTracker_.fileType, null);
//
//        BassFileType defaultFileType = BassFileType.BAM;
//
//        for (SubmissionTracker submissionTracker : submissionTrackerList) {
//            if (submissionTracker.getFileType() != null) {
//                throw new RuntimeException(
//                        String.format("Expected SubmissionTracker %s to have null value but it was %s",
//                                submissionTracker.createSubmissionIdentifier(), submissionTracker.getFileType()));
//            } else {
//                submissionTracker.setFileType(defaultFileType);
//            }
//        }
//
//        submissionTrackerDao.persist(new FixupCommentary(
//                "Backfill fileTypes for existing SubmissionTrackers. See https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-4060"));
//    }

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

    @Test(enabled = false)
    public void gplim5678BackfillLocationAndType() {
        userBean.loginOSUser();

        List<SubmissionTracker> allTrackers = submissionTrackerDao.findTrackersMissingDatatypeOrLocation();

        Map<String, SubmissionTracker> submissionTrackersByUuid = new HashMap<>();
        for (SubmissionTracker submissionTracker : allTrackers) {
            submissionTrackersByUuid.put(submissionTracker.createSubmissionIdentifier(), submissionTracker);
        }
        int index = 1;
        Set<String> keys = submissionTrackersByUuid.keySet();
        int total = keys.size();
        if (!keys.isEmpty()) {
            Collection<SubmissionStatusDetailBean> submissionStatus =
                submissionService.getSubmissionStatus(keys.toArray(new String[0]));

            for (SubmissionStatusDetailBean submissionStatusDetailBean : submissionStatus) {
                SubmissionTracker submissionTracker =
                    submissionTrackersByUuid.get(submissionStatusDetailBean.getUuid());
                if (submissionTracker != null) {
                    if (StringUtils.isBlank(submissionTracker.getDataType())) {
                        String dataType = SubmissionLibraryDescriptor
                            .getNormalizedLibraryName(submissionStatusDetailBean.getSubmissionDatatype());
                        submissionTracker.setDataType(dataType);
                    }
                    if (StringUtils.isBlank(submissionTracker.getProcessingLocation())) {
                        submissionTracker.setProcessingLocation(SubmissionBioSampleBean.ON_PREM);
                    }
                } else {
                    log.info(
                        String.format("SubmissionTracker not found for %s", submissionStatusDetailBean.getUuid()));
                }
                if (index++ % 500 == 0) {
                    log.info(String.format("Processed %d of %d", index, total));
                }
            }
            submissionTrackerDao.persist(new FixupCommentary("GPLIM-5678 Back-fill processingLocation and datatype"));
        }
    }

    @Test(enabled = false)
    public void gplim5408BackfillLocationAndType(){
        userBean.loginOSUser();

        List<SubmissionTracker> allTrackers = submissionTrackerDao.findTrackersMissingDatatypeAndLocation();

        Map<String, SubmissionTracker> submissionTrackersByUuid = new HashMap<>();
        for (SubmissionTracker submissionTracker : allTrackers) {
            submissionTrackersByUuid.put(submissionTracker.createSubmissionIdentifier(), submissionTracker);
        }
        int index = 1;
        Set<String> keys = submissionTrackersByUuid.keySet();
        int total = keys.size();
        if (!keys.isEmpty()) {
            Collection<SubmissionStatusDetailBean> submissionStatus =
                submissionService.getSubmissionStatus(keys.toArray(new String[0]));

            for (SubmissionStatusDetailBean submissionStatusDetailBean : submissionStatus) {
                SubmissionTracker submissionTracker =
                    submissionTrackersByUuid.get(submissionStatusDetailBean.getUuid());
                if (submissionTracker != null) {
                    submissionTracker.setDataType(submissionStatusDetailBean.getSubmissionDatatype());
                    submissionTracker.setProcessingLocation(SubmissionBioSampleBean.ON_PREM);
                } else {
                    log.info(
                        String.format("SubmissionTracker not found for %s", submissionStatusDetailBean.getUuid()));
                }
                if (index++ % 500 == 0) {
                    log.info(String.format("Processed %d of %d", index, total));
                }
            }
            submissionTrackerDao.persist(new FixupCommentary("GPLIM-5408 Back-fill processingLocation and datatype"));
        }
    }

    @Test(enabled = false)
    public void gplim4254BackfillProject() {
        userBean.loginOSUser();

        /*
         * The problem that GPLIM-4254 fixes was discovered when some submissions were posted for samples in RP-12. In
         * these cases, there were two files available in Bass for each sample (one from DNA and one from RNA). Mercury
         * arbitrarily picked one of them, ignoring the other, and submitted that file. This map of sample -> project
         * was extracted from the actual submissions sent to Epsilon 9 on 8-3-2016. It contains the appropriate projects
         * to back-fill for these samples. For all other existing submission trackers in Mercury, there is no ambiguity
         * so we can re-fetch the data from Bass to determine the correct project to back-fill.
         */
        Map<String, String> projectBySampleForRp12 = new HashMap<>();
        projectBySampleForRp12.put("Testes_DFCI_65-Tumor",          "C1566");
        projectBySampleForRp12.put("Testes_DFCI_91-Tumor",          "C1566");
        projectBySampleForRp12.put("Testes_DFCI_16-Tumor1_RNA",     "G89388");
        projectBySampleForRp12.put("Testes_DFCI_13-Tumor1_RNA",     "G89388");
        projectBySampleForRp12.put("Testes_DFCI_12-Tumor1_RNA",     "G89388");
        projectBySampleForRp12.put("Testes_DFCI_85-Tumor",          "C1566");
        projectBySampleForRp12.put("Testes_DFCI_53-Tumor",          "C1566");
        projectBySampleForRp12.put("Testes_DFCI_19-Tumor1_RNA",     "G89388");
        projectBySampleForRp12.put("Testes_DFCI_70-Tumor",          "C1566");
        projectBySampleForRp12.put("Testes_DFCI_31-Tumor4_RNA",     "G89388");
        projectBySampleForRp12.put("Testes_DFCI_88-Tumor",          "C1566");
        projectBySampleForRp12.put("Testes_DFCI_79-Tumor",          "C1566");
        projectBySampleForRp12.put("Testes_DFCI_60-Tumor",          "C1566");
        projectBySampleForRp12.put("Testes_DFCI_68-Tumor",          "C1566");
        projectBySampleForRp12.put("Testes_DFCI_62-Tumor",          "C1566");
        projectBySampleForRp12.put("Testes_DFCI_9-Tumor1_RNA",      "G89388");
        projectBySampleForRp12.put("Testes_DFCI_3-Tumor2_RNA",      "G89388");
        projectBySampleForRp12.put("Testes_DFCI_4-Tumor2_RNA",      "G89388");
        projectBySampleForRp12.put("Testes_DFCI_17-Tumor1_RNA",     "G89388");
        projectBySampleForRp12.put("Testes_DFCI_5-Tumor1_RNA",      "G89388");
        projectBySampleForRp12.put("Testes_DFCI_42-Tumor",          "C1566");
        projectBySampleForRp12.put("Testes_DFCI_26-Tumor1_RNA",     "G89388");
        projectBySampleForRp12.put("Testes_DFCI_20-Tumor1_RNA",     "G89388");
        projectBySampleForRp12.put("Testes_DFCI_31-Tumor2_RNA",     "G89388");
        projectBySampleForRp12.put("Testes_DFCI_78-Tumor",          "C1566");
        projectBySampleForRp12.put("Testes_DFCI_67-Tumor",          "C1566");
        projectBySampleForRp12.put("Testes_DFCI_21-Tumor1_RNA",     "G89388");
        projectBySampleForRp12.put("Testes_DFCI_7-Tumor3_RNA",      "G89388");
        projectBySampleForRp12.put("Testes_DFCI_30-Tumor1_RNA",     "G89388");
        projectBySampleForRp12.put("Testes_DFCI_73-Tumor",          "C1566");
        projectBySampleForRp12.put("Testes_DFCI_6-Tumor1_RNA",      "G89388");
        projectBySampleForRp12.put("Testes_DFCI_45-Tumor",          "C1566");
        projectBySampleForRp12.put("Testes_DFCI_7-Tumor1_RNA",      "G89388");
        projectBySampleForRp12.put("Testes_DFCI_90-Tumor",          "C1566");

        // Fetch all existing submission trackers and group them by research project (for efficiency).
        List<SubmissionTracker> allTrackers = submissionTrackerDao.findAll(SubmissionTracker.class);
        Multimap<ResearchProject, SubmissionTracker> trackersByResearchProject = ArrayListMultimap.create();
        for (SubmissionTracker tracker : allTrackers) {
            trackersByResearchProject.put(tracker.getResearchProject(), tracker);
        }

        for (ResearchProject researchProject : trackersByResearchProject.keySet()) {

            // This shouldn't exist in production, but needs to be skipped while testing the back-fill.
            if (researchProject.getJiraTicketKey().equals("RP-SubmissionTrackerDaoTest") ||
                    researchProject.getJiraTicketKey().equals("RP-testRP")) {
                continue;
            }

            log.info("Back-filling submissions for: " + researchProject.getJiraTicketKey());

            Collection<SubmissionTracker> trackers = trackersByResearchProject.get(researchProject);

            Map<String, String> projectBySample;
            if (researchProject.getJiraTicketKey().equals("RP-12")) {
                projectBySample = projectBySampleForRp12;
            } else {
                /*
                 * Fetch submission DTOs for the research project and index them by sample name. Cannot index by
                 * submission tuple because a tuple from an existing tracker will not have a project yet and will
                 * therefore not match. However, until project is back-filled, and since we're looking at one research
                 * project at a time, and as long as we've only handled BAM file types, sample ID should be sufficiently
                 * unique for existing trackers. The available version may be different than what was submitted, but the
                 * project will be the same.
                 */
                List<SubmissionDto> submissionDtos =
                        submissionDtoFetcher.fetch(researchProject, MessageReporter.UNUSED);
                projectBySample = new HashMap<>();
                for (SubmissionDto submissionDto : submissionDtos) {
                    String sampleName = submissionDto.getSampleName();
                    if (projectBySample.containsKey(sampleName)) {
                        throw new RuntimeException(
                                "Unexpected duplicate submission available for sample: " + sampleName);
                    }
                    projectBySample.put(sampleName, submissionDto.getAggregationProject());
                }
            }

            // Finally, back-fill project for existing trackers.
            for (SubmissionTracker tracker : trackers) {
                String sampleName = tracker.getSubmittedSampleName();
                String project = projectBySample.get(sampleName);
                if (project == null) {
                    throw new RuntimeException("Could not find project for sample: " + sampleName);
                }
                tracker.setProject(project);
            }
        }

        submissionTrackerDao.persist(new FixupCommentary(
                "GPLIM-4254 Backfill of (aggregation) project for all existing submission trackers"));
    }
}
