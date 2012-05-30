package org.broadinstitute.pmbridge.entity.experiments;

import junit.framework.Assert;
import org.broadinstitute.pmbridge.entity.common.Name;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.entity.person.RoleType;
import org.broadinstitute.pmbridge.entity.project.PlatformType;
import org.broadinstitute.pmbridge.entity.project.ResearchProject;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;

import static org.broadinstitute.pmbridge.TestGroups.UNIT;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/30/12
 * Time: 5:22 PM
 */
@Test(groups = {UNIT})
public class ExperimentRequestSummaryTest {

    private ExperimentRequestSummary experimentRequestSummary;

    @BeforeMethod
    public void setUp() throws Exception {
        experimentRequestSummary = new ExperimentRequestSummary  (
                 new Person("pmbridge", RoleType.PROGRAM_PM),
                 new Date(),
                 PlatformType.GSP
         );
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetTitle() throws Exception {
        Assert.assertNull( experimentRequestSummary.getTitle() );
        experimentRequestSummary.setTitle( new Name("ExpTitle"));
        Assert.assertEquals(experimentRequestSummary.getTitle().name, "ExpTitle");
    }

    @Test
    public void testCreation() throws Exception {
        Assert.assertNotNull( experimentRequestSummary.getCreation() );
        Assert.assertNotNull(experimentRequestSummary.getCreation().date);
        Assert.assertEquals(experimentRequestSummary.getCreation().person.getUsername(), "pmbridge");
    }

    @Test
    public void testGetRemoteId() throws Exception {
        Assert.assertNull( experimentRequestSummary.getRemoteId() );
    }

    @Test
    public void testGetLocalId() throws Exception {
        Assert.assertNotNull(experimentRequestSummary.getLocalId() );
    }

    @Test
    public void testGetModification() throws Exception {
        Assert.assertNotNull( experimentRequestSummary.getModification());
        Assert.assertNotNull( experimentRequestSummary.getModification().date);
        Assert.assertNotNull( experimentRequestSummary.getModification().person);
    }

    @Test
    public void testGetPlatformType() throws Exception {
        Assert.assertEquals( experimentRequestSummary.getPlatformType(), PlatformType.GSP);

    }

    @Test
    public void testGetStatus() throws Exception {
        Assert.assertEquals( experimentRequestSummary.getStatus(), ExperimentRequestSummary.DRAFT_STATUS );
    }

    @Test
    public void testGetResearchProjectId() throws Exception {
        Assert.assertEquals( experimentRequestSummary.getResearchProjectId(), ResearchProject.UNSPECIFIED_ID);
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
