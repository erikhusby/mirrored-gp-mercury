package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class LabMetricParserTest extends ContainerTest {

    private String GOOD_QUANT_UPLOAD_FILE = "quant_upload_good.xlsx";

    @Inject
    private TwoDBarcodedTubeDAO vesselDao;

    @Inject
    private LabMetricParser parser;

    private Map<String, LabVessel> barcodeToTubeMap = new HashMap<>();
    private Map<String, Double> barcodeToQuant = new HashMap<>();
    private InputStream resourceFile;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container.
        if (vesselDao == null ||
            parser == null) {
            return;
        }

//        barcodeToTubeMap.put("2402938482", new TwoDBarcodedTube("2402938482"));
//        barcodeToTubeMap.put("2208428758", new TwoDBarcodedTube("2208428758"));
//        barcodeToTubeMap.put("3559709487", new TwoDBarcodedTube("3559709487"));
//        barcodeToTubeMap.put("3938342818", new TwoDBarcodedTube("3938342818"));
//        barcodeToTubeMap.put("3585528276", new TwoDBarcodedTube("3585528276"));
//        barcodeToTubeMap.put("3132943337", new TwoDBarcodedTube("3132943337"));
//        barcodeToTubeMap.put("8815228500", new TwoDBarcodedTube("8815228500"));
//        barcodeToTubeMap.put("5936483766", new TwoDBarcodedTube("5936483766"));
//        barcodeToTubeMap.put("4621329996", new TwoDBarcodedTube("4621329996"));
//        barcodeToTubeMap.put("9085949196", new TwoDBarcodedTube("9085949196"));
//        barcodeToTubeMap.put("4069756425", new TwoDBarcodedTube("4069756425"));
//        barcodeToTubeMap.put("3850486410", new TwoDBarcodedTube("3850486410"));
//        barcodeToTubeMap.put("5761812024", new TwoDBarcodedTube("5761812024"));
//        barcodeToTubeMap.put("4047896363", new TwoDBarcodedTube("4047896363"));
//        barcodeToTubeMap.put("5142352881", new TwoDBarcodedTube("5142352881"));

        barcodeToQuant.put("SGMTEST2402938482", 32.42d);
        barcodeToQuant.put("SGMTEST2208428758", 54.22d);
        barcodeToQuant.put("SGMTEST3559709487", 17.76d);
        barcodeToQuant.put("SGMTEST3938342818", 16.22d);
        barcodeToQuant.put("SGMTEST3585528276", 62.74d);
        barcodeToQuant.put("SGMTEST3132943337", 99.11d);
        barcodeToQuant.put("SGMTEST8815228500", 42.09d);
        barcodeToQuant.put("SGMTEST5936483766", 28.04d);
        barcodeToQuant.put("SGMTEST4621329996", 95.05d);
        barcodeToQuant.put("SGMTEST9085949196", 41.21d);
        barcodeToQuant.put("SGMTEST4069756425", 71.66d);
        barcodeToQuant.put("SGMTEST3850486410", 59.02d);
        barcodeToQuant.put("SGMTEST5761812024", 50.44d);
        barcodeToQuant.put("SGMTEST4047896363", 33.95d);
        barcodeToQuant.put("SGMTEST5142352881", 38.44d);

        for(Map.Entry<String, Double> quantEntry:barcodeToQuant.entrySet()) {
            TwoDBarcodedTube testTube = new TwoDBarcodedTube(quantEntry.getKey());
            vesselDao.persist(testTube);

        }

        vesselDao.flush();
        vesselDao.clear();

        resourceFile = Thread.currentThread().getContextClassLoader().getResourceAsStream(GOOD_QUANT_UPLOAD_FILE);

    }


    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container.
        if (vesselDao == null ||
            parser == null) {
            return;
        }

        for(Map.Entry<String, Double> quantEntry:barcodeToQuant.entrySet()) {
            TwoDBarcodedTube testTube =  vesselDao.findByBarcode(quantEntry.getKey());
                    vesselDao.remove(testTube);
        }

        vesselDao.flush();
        vesselDao.clear();
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testQuantParser() throws InvalidFormatException, IOException, ValidationException {

        Collection<LabMetric> createdMetrics = parser.processUploadFile(resourceFile, LabMetric.MetricType.ECO_QPCR);

        Assert.assertEquals(createdMetrics.size(), barcodeToQuant.size());

        for(LabMetric testMetric:createdMetrics) {

            Assert.assertEquals(testMetric.getValue(),
                    BigDecimal.valueOf(barcodeToQuant.get(testMetric.getLabVessel().getLabel())));
        }

    }

}
