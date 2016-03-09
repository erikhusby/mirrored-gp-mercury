package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProjects;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;

import javax.enterprise.inject.Alternative;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Stub
@Alternative
public class SubmissionsServiceStub implements SubmissionsService {
    public static final String TEST_PROJECT_NAME = "Primary submission";
    public static final String STUB_UPDATE_DATE = "Dec 17, 2001 9:30 AM";
    public static final SubmissionRepository ACTIVE_REPO = new SubmissionRepository("ACTIVE_REPO", "Active Repository", true);
    public static final SubmissionRepository INACTIVE_REPO = new SubmissionRepository("INACTIVE_REPO", "Inactive Repository", false);


    private Date getDateOfLastUpdate() {
        Date dateLastUpdate = null;
        try {
            dateLastUpdate = DateUtils.parseDate(SubmissionDto.DATE_FORMAT.getPattern(), STUB_UPDATE_DATE);
        } catch (ParseException e) {
            throw new NullPointerException("Cannot parse " + STUB_UPDATE_DATE + " with " + SubmissionDto.DATE_FORMAT.getPattern());
        }
        return dateLastUpdate;
    }

    @Override
    public Collection<SubmissionStatusDetailBean> getSubmissionStatus(String... uuids) {
        SubmissionStatusResultBean results = new SubmissionStatusResultBean();

        for (String uuid : uuids) {
            SubmissionStatusDetailBean detail =
                    new SubmissionStatusDetailBean(uuid, SubmissionStatusDetailBean.Status.SUBMITTED.getKey(),
                            getDateOfLastUpdate());
            results.getSubmissionStatuses().add(detail);
        }
        for (String uuid : uuids) {
            SubmissionStatusDetailBean detail =
                    new SubmissionStatusDetailBean(uuid, SubmissionStatusDetailBean.Status.FAILURE.getKey(),
                            getDateOfLastUpdate(), "And error was returned from NCBI");
            results.getSubmissionStatuses().add(detail);
        }


        return results.getSubmissionStatuses();
    }

    @Override
    public Collection<BioProject> getAllBioProjects() {
        BioProjects bioProjects = new BioProjects(
                new BioProject(generateTestName("PRJ"), generateTestName("phs"), TEST_PROJECT_NAME),
                new BioProject(generateTestName("PRJ"), generateTestName("phs"), TEST_PROJECT_NAME),
                new BioProject(generateTestName("PRJ"), generateTestName("phs"), TEST_PROJECT_NAME),
                new BioProject(generateTestName("PRJ"), generateTestName("phs"), TEST_PROJECT_NAME),
                new BioProject(generateTestName("PRJ"), generateTestName("phs"), TEST_PROJECT_NAME),
                new BioProject(generateTestName("PRJ"), generateTestName("phs"), TEST_PROJECT_NAME)
        );
        return bioProjects.getBioprojects();
    }

    @Override
    public Collection<SubmissionStatusDetailBean> postSubmissions(SubmissionRequestBean submission) {
        SubmissionStatusResultBean results = new SubmissionStatusResultBean();

        for (SubmissionBean submissionBean : submission.getSubmissions()) {
            String uuid = submissionBean.getUuid();
            SubmissionStatusDetailBean detail =
                    new SubmissionStatusDetailBean(uuid, SubmissionStatusDetailBean.Status.SUBMITTED.getKey(),
                            getDateOfLastUpdate());
            results.getSubmissionStatuses().add(detail);
            detail = new SubmissionStatusDetailBean(uuid, SubmissionStatusDetailBean.Status.FAILURE.getKey(),
                    getDateOfLastUpdate(), uuid + " failed");
            results.getSubmissionStatuses().add(detail);
        }
        return results.getSubmissionStatuses();
    }

    @Override
    public Collection<String> getSubmissionSamples(BioProject bioProject) {
        return Arrays.asList(
                generateTestName("csid_"), generateTestName("csid_"), generateTestName("csid_"),
                generateTestName("csid_"), generateTestName("csid_"), generateTestName("csid_"));
    }

    private static String generateTestName(String prefix) {
        return String.format("%s%d", prefix, new Random().nextInt(9999));
    }

    @Override
    public List<SubmissionRepository> getSubmissionRepositories() {
        return Arrays.asList(ACTIVE_REPO, INACTIVE_REPO);
    }

    @Override
    public List<SubmissionLibraryDescriptor> getSubmissionLibraryDescriptors() {
        return null;
    }

    @Override
    public SubmissionRepository findRepositoryByKey(String key) {
        return null;
    }

    @Override
    public SubmissionRepository repositorySearch(String siteName) {
        return null;
    }

    @Override
    public SubmissionLibraryDescriptor findSubmissionTypeByKey(String selectedSubmissionDescriptor) {
        return null;
    }
}
