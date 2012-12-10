package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientProducer;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Scott Matthews
 *         Date: 12/7/12
 *         Time: 4:31 PM
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class LabBatchEjbTest extends ContainerTest {

    public static final String STUB_TEST_PDO_KEY = "PDO-999";
    private ProductOrder testPdo;

    @Inject
    AthenaClientService athenaClientService;

    @Inject
    LabBatchEjb labBatchEJB;

    @Inject
    private UserTransaction utx;


    private LinkedHashMap<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
    private String            workflowName;
    private ArrayList<String> pdoNames;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {

        if (utx == null) {
            return;
        }

        utx.begin();

        pdoNames = new ArrayList<String>();
        Collections.addAll(pdoNames, STUB_TEST_PDO_KEY);

        testPdo = athenaClientService.retrieveProductOrderDetails(STUB_TEST_PDO_KEY);

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
                labBatchEJB.createLabBatch(new HashSet<LabVessel>(mapBarcodeToTube.values()), "scottmat", null);

    }
}
