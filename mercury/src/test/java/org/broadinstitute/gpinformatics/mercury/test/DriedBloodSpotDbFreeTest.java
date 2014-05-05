package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabEventBean;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabEventResource;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchResource;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.TubeBean;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventRefDataFetcher;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.test.builders.DriedBloodSpotEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.DriedBloodSpotJaxbBuilder;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test messaging for BSP Dried Blood Spot Extraction
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class DriedBloodSpotDbFreeTest {
    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("MMddHHmmss");

    public void testEndToEnd() {
        // import batch and tubes
        String timestamp = timestampFormat.format(new Date());
        LabBatchResource labBatchResource = new LabBatchResource();
        List<TubeBean> tubeBeans = new ArrayList<>();
        for(int rackPosition = 1; rackPosition <= 96; rackPosition++) {
            String barcode = "SM-FTA" + rackPosition + timestamp;
            tubeBeans.add(new TubeBean(barcode, null));
        }

        String batchId = "BP-2";
        Map<String, BarcodedTube> mapBarcodeToTube = new LinkedHashMap<>();
        Map<MercurySample, MercurySample> mapSampleToSample = new LinkedHashMap<>();
        LabBatch labBatch = labBatchResource.buildLabBatch(new LabBatchBean(batchId, "DBS", tubeBeans),
                mapBarcodeToTube, mapSampleToSample/*, null*/);

        DriedBloodSpotJaxbBuilder driedBloodSpotJaxbBuilder = new DriedBloodSpotJaxbBuilder(
                new ArrayList<>(mapBarcodeToTube.keySet()), labBatch.getBatchName(), timestamp);
        driedBloodSpotJaxbBuilder.buildJaxb();
        DriedBloodSpotEntityBuilder driedBloodSpotEntityBuilder = new DriedBloodSpotEntityBuilder(
                driedBloodSpotJaxbBuilder, labBatch, mapBarcodeToTube);
        driedBloodSpotEntityBuilder.buildEntities();

        LabEventResource labEventResource = new LabEventResource();
        List<LabEventBean> labEventBeans = labEventResource.buildLabEventBeans(
                new ArrayList<>(labBatch.getLabEvents()),
                new LabEventRefDataFetcher() {
                   @Override
                   public BspUser getOperator(String userId) {
                       BSPUserList testList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());
                       return testList.getByUsername(userId);
                   }

                   @Override
                   public BspUser getOperator(Long bspUserId) {
                       BSPUserList testList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());
                       return testList.getById(bspUserId);
                   }

                   @Override
                   public LabBatch getLabBatch(String labBatchName) {
                       return null;
                   }
               });
//        Assert.assertEquals("Wrong number of messages", 10, labEventBeans.size());
    }

}
