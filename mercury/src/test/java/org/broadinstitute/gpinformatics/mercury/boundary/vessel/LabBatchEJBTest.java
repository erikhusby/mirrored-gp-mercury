package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author Scott Matthews
 *         Date: 12/7/12
 *         Time: 4:31 PM
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class LabBatchEjbTest extends ContainerTest {

    public static final String STUB_TEST_PDO_KEY = "PDO-999";

    @Inject
    private LabBatchEjb labBatchEJB;

    @Inject
    private UserTransaction utx;

    @Inject
    private LabBatchDAO labBatchDAO;

    private LinkedHashMap<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
    private String            workflowName;
    private ArrayList<String> pdoNames;
    private String            scottmat;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {

        if (utx == null) {
            return;
        }

        utx.begin();

        pdoNames = new ArrayList<String>();
        Collections.addAll(pdoNames, STUB_TEST_PDO_KEY);

        workflowName = "Exome Express";

        List<String> vesselSampleList = new ArrayList<String>(6);

        Collections.addAll(vesselSampleList, "SM-423", "SM-243", "SM-765", "SM-143", "SM-9243", "SM-118");

        // starting rack
        for (int sampleIndex = 1; sampleIndex <= vesselSampleList.size(); sampleIndex++) {
            String barcode = "R" + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex;
            String bspStock = vesselSampleList.get(sampleIndex - 1);
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(STUB_TEST_PDO_KEY, bspStock));
            mapBarcodeToTube.put(barcode, bspAliquot);
        }
        scottmat = "scottmat";
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        if (utx == null) {
            return;
        }

        utx.rollback();

    }


    @Test
    public void testCreateLabBatch() throws Exception {

        LabBatch testBatch =
                labBatchEJB.createLabBatch(new HashSet<LabVessel>(mapBarcodeToTube.values()), scottmat, "LCSET-123");

        final String batchName = testBatch.getBatchName();

        labBatchDAO.flush();
        labBatchDAO.clear();

        LabBatch testFind = labBatchDAO.findByName(batchName);

        Assert.assertNotNull(testFind);
        Assert.assertNotNull(testFind.getJiraTicket());
        Assert.assertNotNull(testFind.getJiraTicket().getTicketName());
        Assert.assertNotNull(testFind.getStartingLabVessels());
        Assert.assertEquals(6, testFind.getStartingLabVessels().size());
        Assert.assertEquals(batchName, testFind.getBatchName());
    }

    @Test
    public void testCreateLabBatchWithValues() throws Exception {

        String batchName = "Test lab Batch Name";
        final String description = "Test of New user input Batch Description";

        Date now = new Date();
        Calendar cal = Calendar.getInstance();

        cal.setTime(now);

        cal.add(Calendar.DATE, 21);

        Date future = cal.getTime();

        SimpleDateFormat dFormatter = new SimpleDateFormat("MM/dd/yy");

        String futureDate = dFormatter.format(future);

        LabBatch batchInput =new LabBatch(batchName,new HashSet<LabVessel>(mapBarcodeToTube.values()));
        batchInput.setBatchDescription(description);
        batchInput.setDueDate(future);

        labBatchEJB.createLabBatch(batchInput, scottmat);

        batchName = batchInput.getBatchName();

        labBatchDAO.flush();
        labBatchDAO.clear();

        LabBatch testFind = labBatchDAO.findByName(batchName);

        Assert.assertNotNull(testFind);
        Assert.assertNotNull(testFind.getJiraTicket());
        Assert.assertNotNull(testFind.getJiraTicket().getTicketName());
        Assert.assertNotNull(testFind.getStartingLabVessels());
        Assert.assertEquals(6, testFind.getStartingLabVessels().size());
        Assert.assertEquals(batchName, testFind.getBatchName());
    }
}
