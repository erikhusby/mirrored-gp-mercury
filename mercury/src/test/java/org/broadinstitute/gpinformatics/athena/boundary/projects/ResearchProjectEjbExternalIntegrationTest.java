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

package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionConfig;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionDto;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.hamcrest.CoreMatchers;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class ResearchProjectEjbExternalIntegrationTest {
    private BioProject bioProjectWithResults = new BioProject("PRJNA75723");
    private List<SubmissionDto> goodSubmissionDtos =
            getTestDataWithSamples("4304714212_K", "4377315018_E", "4304714040_C");

    private ResearchProjectEjb researchProjectEjb = new ResearchProjectEjb(null, null, null, null, null, null,
            new SubmissionsServiceImpl(SubmissionConfig.produce(Deployment.DEV)), null);

    @Test(expectedExceptions = NullPointerException.class)
    public void testValidateSubmissionSamplesNullSample() throws Exception {
        researchProjectEjb.validateSubmissionSamples(bioProjectWithResults, null);
        assertThat(null, CoreMatchers.notNullValue());
    }

    @Test(expectedExceptions = ValidationException.class)
    public void testValidateSubmissionSamplesNoSamplesMatch() throws ValidationException {
        researchProjectEjb.validateSubmissionSamples(bioProjectWithResults,
                getTestDataWithSamples("fake", "faker", "fakist"));
    }

    public void testValidationMessage() throws ValidationException {
        String[] fakeSampleNames = {"fake", "faker", "fakist"};
        try {
            researchProjectEjb
                    .validateSubmissionSamples(bioProjectWithResults, getTestDataWithSamples(fakeSampleNames));
        } catch (ValidationException e) {
            String expectedValidationMessage = "Some sample(s) have not been pre-accessioned and are not available for submission: ";
            assertThat(e.getMessage(), startsWith(expectedValidationMessage));

            for (String fakeSampleName : fakeSampleNames) {
                assertThat(e.getMessage(), containsString(fakeSampleName));
            }

        }
    }

    @Test(expectedExceptions = ValidationException.class)
    public void testValidateSubmissionSamplesEmptySampleList() throws Exception {
        researchProjectEjb.validateSubmissionSamples(bioProjectWithResults, getTestDataWithSamples(""));

    }

    @Test(expectedExceptions = ValidationException.class)
    public void testValidateSubmissionBadBioProject() throws Exception {
        researchProjectEjb.validateSubmissionSamples(new BioProject("you'll never find me."), goodSubmissionDtos);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testValidateSubmissionNullBioProject() throws Exception {
        researchProjectEjb.validateSubmissionSamples(null, goodSubmissionDtos);

        assertThat(null, CoreMatchers.notNullValue());
    }


    public void testValidateSubmissionAOK() throws Exception {
        researchProjectEjb.validateSubmissionSamples(bioProjectWithResults, goodSubmissionDtos);
    }

    private List<SubmissionDto> getTestDataWithSamples(String... sampleNames) {
        List<SubmissionDto> results = new ArrayList<>(sampleNames.length);
        for (String sampleName : sampleNames) {
            Map<BassDTO.BassResultColumn, String> valuesMap = new HashMap<>();
            valuesMap.put(BassDTO.BassResultColumn.sample, sampleName);
            valuesMap.put(BassDTO.BassResultColumn.version, "1");
            valuesMap.put(BassDTO.BassResultColumn.project, "RP-123");
            valuesMap.put(BassDTO.BassResultColumn.file_type, "bam");
            BassDTO bassDTO = new BassDTO(valuesMap);
            results.add(new SubmissionDto(bassDTO, null, Collections.<ProductOrder>emptyList(), null));
        }
        return results;
    }
}
