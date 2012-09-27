package org.broadinstitute.gpinformatics.athena.entity.experiments;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/30/12
 * Time: 5:22 PM
 */
@Test(groups = {TestGroups.DATABASE_FREE})
public class ExperimentRequestSummaryTest {

    private ExperimentRequestSummary experimentRequestSummary;

    @BeforeMethod
    public void setUp() throws Exception {
        experimentRequestSummary = new ExperimentRequestSummary(
                "An Experiment Title", new Person("athena"),
                new Date(),
                ExperimentType.WholeGenomeSequencing
        );
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetTitle() throws Exception {
        experimentRequestSummary.setTitle("ExpTitle");
        Assert.assertEquals(experimentRequestSummary.getTitle(), "ExpTitle");
    }

    @Test
    public void testCreation() throws Exception {
        Assert.assertNotNull(experimentRequestSummary.getCreation());
        Assert.assertNotNull(experimentRequestSummary.getCreation().date);
        Assert.assertEquals(experimentRequestSummary.getCreation().person.getLogin(), "athena");
    }

    @Test
    public void testGetRemoteId() throws Exception {
        Assert.assertNotNull(experimentRequestSummary.getExperimentId());
        Assert.assertTrue(experimentRequestSummary.getExperimentId().value.startsWith("DRAFT_"));
    }

//    @Test
//    public void testGetLocalId() throws Exception {
//        Assert.assertNotNull(experimentRequestSummary.getLocalId() );
//    }

    @Test
    public void testGetModification() throws Exception {
        Assert.assertNotNull(experimentRequestSummary.getModification());
        Assert.assertNotNull(experimentRequestSummary.getModification().date);
        Assert.assertNotNull(experimentRequestSummary.getModification().person);
    }

    @Test
    public void testGetExperimentType() throws Exception {
        Assert.assertEquals(experimentRequestSummary.getExperimentType(), ExperimentType.WholeGenomeSequencing);

    }

    @Test
    public void testGetStatus() throws Exception {
        Assert.assertEquals(experimentRequestSummary.getStatus(), ExperimentRequestSummary.DRAFT_STATUS);
    }

    @Test
    public void testGetResearchProjectId() throws Exception {
        Assert.assertNotNull(experimentRequestSummary.getResearchProjectId());
    }

    @Test
    public void testSetRemoteId() throws Exception {
    }

    @Test
    public void testSetModification() throws Exception {
    }

    @Test
    public void testSetCreation() throws Exception {
    }

    @Test
    public void testSetStatus() throws Exception {
    }

    @Test
    public void testSetResearchProjectId() throws Exception {
    }

}
