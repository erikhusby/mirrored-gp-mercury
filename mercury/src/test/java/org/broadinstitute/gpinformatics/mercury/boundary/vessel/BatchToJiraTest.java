package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Test(groups = TestGroups.ALTERNATIVES, singleThreaded = true)
@Dependent
public class BatchToJiraTest extends Arquillian {

    public BatchToJiraTest(){}

    @Inject
    private LabBatchEjb batchEjb;

    @Inject
    private ReworkEjb reworkEjb;

    @Inject
    private JiraService jiraService;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private UserTransaction transaction;

    @BeforeMethod
    public void setUp() throws Exception {
        if (transaction == null) {
            return;
        }
        transaction.begin();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        // Skip if no injections, since we're not running in container.
        if (transaction == null) {
            return;
        }
        transaction.rollback();
    }

    /**
     * Use test deployment here to talk to the actual jira
     *
     * @return
     */
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.TEST);
    }

    private String getGssrFieldFromJiraTicket(JiraIssue issue) throws IOException {
        Map<String, CustomFieldDefinition> gssrField =
                jiraService.getCustomFields(LabBatch.TicketFields.GSSR_IDS.getName());
        String gssrIdsText =
                (String) jiraService.getIssueFields(issue.getKey(), gssrField.values()).getFields().values().iterator()
                        .next();
        return gssrIdsText;
    }

    @Test(enabled = true)
    public void testJiraCreationFromBatch() throws Exception {
        String expectedGssrText = "SM-01\nSM-02";
        Set<LabVessel> startingVessels = new HashSet<>();
        String tube1Label = "Starter01";
        String tube2Label = "Rework01";

        LabVessel tube1 = new BarcodedTube(tube1Label);
        tube1.addSample(new MercurySample("SM-01", MercurySample.MetadataSource.BSP));
        startingVessels.add(tube1);

        LabVessel tube2 = new BarcodedTube(tube2Label);
        tube2.addSample(new MercurySample("SM-02", MercurySample.MetadataSource.BSP));
        labVesselDao.persistAll(Arrays.asList(tube1, tube2));

        labVesselDao.flush();
        labVesselDao.clear();

        tube1 = labVesselDao.findByIdentifier(tube1Label);
        tube2 = labVesselDao.findByIdentifier(tube2Label);

        LabEvent event = new LabEvent(LabEventType.DENATURE_TO_FLOWCELL_TRANSFER, new Date(), "TEST-LAND", 0L, 101L,
                "batchToJiraTest");
        tube2.addInPlaceEvent(event);
        LabBatch batch = new LabBatch("Test batch 2", startingVessels, LabBatch.LabBatchType.WORKFLOW);
        ProductOrder stubTestPDO = ProductOrderTestFactory.createDummyProductOrder(LabBatchEJBTest.STUB_TEST_PDO_KEY);

        Bucket bucket = new Bucket("Test");
        batch.addBucketEntry(new BucketEntry(tube1, stubTestPDO, bucket, BucketEntry.BucketEntryType.PDO_ENTRY));

        batchEjb.batchToJira("andrew", null, batch, CreateFields.IssueType.EXOME_EXPRESS, CreateFields.ProjectType.LCSET_PROJECT);

        JiraIssue ticket = jiraService.getIssue(batch.getJiraTicket().getTicketId());

        String gssrIdsText = getGssrFieldFromJiraTicket(ticket);

        assertThat(gssrIdsText, notNullValue());
        assertThat(gssrIdsText.trim(), equalTo("SM-01"));

        // now try it with SM-02 as a rework
        // FIXME find a different way to do this.  This method, addReworkToBatch, is not a production used method.
        reworkEjb.addReworkToBatch(batch, tube2Label, "scottmat");
        batch.addBucketEntry(new BucketEntry(tube2, stubTestPDO, bucket, BucketEntry.BucketEntryType.REWORK_ENTRY));
        batchEjb.batchToJira("andrew", null, batch, CreateFields.IssueType.EXOME_EXPRESS, CreateFields.ProjectType.LCSET_PROJECT);

        ticket = jiraService.getIssue(batch.getJiraTicket().getTicketId());
        gssrIdsText = getGssrFieldFromJiraTicket(ticket);
        assertThat(gssrIdsText.trim(), equalTo(expectedGssrText.trim()));
    }

}
