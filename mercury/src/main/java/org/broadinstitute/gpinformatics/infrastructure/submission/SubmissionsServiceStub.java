package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProjects;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Stub
@Alternative
@Dependent
public class SubmissionsServiceStub implements SubmissionsService {

    public SubmissionsServiceStub(){}

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
                    new SubmissionStatusDetailBean(uuid, SubmissionStatusDetailBean.Status.SUBMITTED,
                            SubmissionRepository.DEFAULT_REPOSITORY_DESCRIPTOR, SubmissionLibraryDescriptor.WHOLE_GENOME.getName(),
                            getDateOfLastUpdate());
            results.getSubmissionStatuses().add(detail);
        }
        for (String uuid : uuids) {
            SubmissionStatusDetailBean detail =
                    new SubmissionStatusDetailBean(uuid, SubmissionStatusDetailBean.Status.FAILURE,
                            SubmissionRepository.DEFAULT_REPOSITORY_DESCRIPTOR, SubmissionLibraryDescriptor.WHOLE_GENOME.getName(),
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
                    new SubmissionStatusDetailBean(uuid, SubmissionStatusDetailBean.Status.SUBMITTED,
                            SubmissionRepository.DEFAULT_REPOSITORY_DESCRIPTOR, SubmissionLibraryDescriptor.WHOLE_GENOME.getDescription(),
                            getDateOfLastUpdate());
            results.getSubmissionStatuses().add(detail);
            detail = new SubmissionStatusDetailBean(uuid, SubmissionStatusDetailBean.Status.FAILURE,
                    SubmissionRepository.DEFAULT_REPOSITORY_DESCRIPTOR, SubmissionLibraryDescriptor.WHOLE_GENOME.getDescription(),
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
        return Collections.singletonList(ProductFamily.defaultLibraryDescriptor());
    }

    @Override
    public SubmissionRepository findRepositoryByKey(String key) {
        return new SubmissionRepository(SubmissionRepository.DEFAULT_REPOSITORY_NAME,SubmissionRepository.DEFAULT_REPOSITORY_DESCRIPTOR);
    }

    @Override
    public SubmissionLibraryDescriptor findLibraryDescriptorTypeByKey(String key) {
        return null;
    }

    @Override
    public List<SubmissionTracker> findOrphans(Collection<SubmissionTracker> submissionTrackers) {
        return Collections.emptyList();
    }
}
