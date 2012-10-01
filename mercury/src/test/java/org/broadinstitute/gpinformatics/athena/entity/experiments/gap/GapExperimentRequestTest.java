package org.broadinstitute.gpinformatics.athena.entity.experiments.gap;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentType;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.beans.IntrospectionException;
import java.util.Date;


/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 8/15/12
 * Time: 1:11 PM
 */
public class GapExperimentRequestTest {


    private GapExperimentRequest gapExperimentRequest;
    private ExperimentRequestSummary experimentRequestSummary;

    @BeforeMethod
    public void setUp() throws Exception {
        experimentRequestSummary = new ExperimentRequestSummary(
                "An Experiment Title", new Person("athena"),
                new Date(),
                ExperimentType.Genotyping
        );
        gapExperimentRequest = new GapExperimentRequest(experimentRequestSummary);
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }


    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testClass() throws IntrospectionException {

    }

    @Test
    public void testGetExperimentStatus() throws Exception {

    }

    @Test
    public void testGetExperimentPlanDTO() throws Exception {

    }

    @Test
    public void testGetGapGroupName() throws Exception {

    }

    @Test
    public void testSetGapGroupName() throws Exception {

    }

    @Test
    public void testGetGapProjectName() throws Exception {

    }

    @Test
    public void testSetGapProjectName() throws Exception {

    }

    @Test
    public void testGetBspQuote() throws Exception {

    }

    @Test
    public void testSetBspQuote() throws Exception {

    }

    @Test
    public void testGetGapQuote() throws Exception {

    }

    @Test
    public void testSetGapQuote() throws Exception {

    }

    @Test
    public void testGetTechnologyProduct() throws Exception {

    }

    @Test
    public void testSetTechnologyProduct() throws Exception {

    }

    @Test
    public void testGetPlatformProjectManagers() throws Exception {

    }

    @Test
    public void testGetProgramProjectManagers() throws Exception {

    }

    @Test
    public void testSetProgramProjectManagers() throws Exception {

    }

    @Test
    public void testGetSynopsis() throws Exception {

    }

    @Test
    public void testSetSynopsis() throws Exception {

    }

    @Test
    public void testSetTitle() throws Exception {

    }

    @Test
    public void testGetVersion() throws Exception {

    }

    @Test
    public void testGetExpectedKitReceiptDate() throws Exception {

    }

    @Test
    public void testSetExpectedKitReceiptDate() throws Exception {

    }

    @Test
    public void testCloneRequest() throws Exception {

    }

    @Test
    public void testExportToExcel() throws Exception {

    }

    @Test
    public void testAssociateWithResearchProject() throws Exception {

    }

    @Test
    public void testGetSamples() throws Exception {

    }

    @Test
    public void testEquals() throws Exception {

    }

    @Test
    public void testHashCode() throws Exception {

    }
}
