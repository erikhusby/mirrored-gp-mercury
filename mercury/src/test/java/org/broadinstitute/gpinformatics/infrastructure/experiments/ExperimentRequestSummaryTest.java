package org.broadinstitute.gpinformatics.infrastructure.experiments;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectId;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/30/12
 * Time: 5:22 PM
 * @deprecated  should no longer need this test class. Can delete soon.
 */
public class ExperimentRequestSummaryTest {

    private ExperimentRequestSummary experimentRequestSummary;

    public void setUp() throws Exception {
        experimentRequestSummary = new ExperimentRequestSummary(
                "An Experiment Title", new Person("athena"), new Date(), ExperimentType.WholeGenomeSequencing);
        experimentRequestSummary.setResearchProjectId(new ResearchProjectId("testResearchProject"));
    }

    public void tearDown() throws Exception {
    }

    public void testGetTitle() throws Exception {
        experimentRequestSummary.setTitle("ExpTitle");
        Assert.assertEquals(experimentRequestSummary.getTitle(), "ExpTitle");
    }

    public void testCreation() throws Exception {
        Assert.assertNotNull(experimentRequestSummary.getCreation());
        Assert.assertNotNull(experimentRequestSummary.getCreation().date);
        Assert.assertEquals(experimentRequestSummary.getCreation().person.getLogin(), "athena");
    }

    public void testGetRemoteId() throws Exception {
        Assert.assertNotNull(experimentRequestSummary.getExperimentId());
        Assert.assertTrue(experimentRequestSummary.getExperimentId().value.startsWith("DRAFT_"));
    }


    public void testGetModification() throws Exception {
        Assert.assertNotNull(experimentRequestSummary.getModification());
        Assert.assertNotNull(experimentRequestSummary.getModification().date);
        Assert.assertNotNull(experimentRequestSummary.getModification().person);
    }

    public void testGetExperimentType() throws Exception {
        Assert.assertEquals(experimentRequestSummary.getExperimentType(), ExperimentType.WholeGenomeSequencing);

    }

    public void testGetStatus() throws Exception {
        Assert.assertEquals(experimentRequestSummary.getStatus(), ExperimentRequestSummary.DRAFT_STATUS);
    }

    public void testGetResearchProjectId() throws Exception {
        Assert.assertNotNull(experimentRequestSummary.getResearchProjectId());
    }


}
