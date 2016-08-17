package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.LabEventSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.*;

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderSampleJsonFactoryTest {

    public static final String PACKAGE_DATE = "05/01/2014";
    public static final String RECEIPT_DATE = "05/05/2014";
    private ProductOrderSampleJsonFactory factory;

    @BeforeMethod
    public void setUp() throws Exception {
        factory = new ProductOrderSampleJsonFactory();
    }

    public void testNonBspSampleToJson() throws JSONException {
        ProductOrderSample productOrderSample = new ProductOrderSample("123", 2L);

        JSONObject jsonObject = factory.toJson(productOrderSample);

        assertThat((Long) jsonObject.get(BspSampleData.SAMPLE_ID), equalTo(2L));
        assertThat((String) jsonObject.get(BspSampleData.COLLABORATOR_SAMPLE_ID), equalTo(""));
        assertThat((String) jsonObject.get(BspSampleData.PATIENT_ID), equalTo(""));
        assertThat((String) jsonObject.get(BspSampleData.COLLABORATOR_PARTICIPANT_ID), equalTo(""));
        assertThat((String) jsonObject.get(BspSampleData.VOLUME), equalTo(""));
        assertThat((String) jsonObject.get(BspSampleData.CONCENTRATION), equalTo(""));
        assertThat((String) jsonObject.get(BspSampleData.JSON_RIN_KEY), equalTo(""));
        assertThat((String) jsonObject.get(BspSampleData.JSON_RQS_KEY), equalTo(""));
        assertThat((String) jsonObject.get(BspSampleData.PICO_DATE), equalTo(""));
        assertThat((String) jsonObject.get(BspSampleData.TOTAL), equalTo(""));
        assertThat((String) jsonObject.get(BspSampleData.HAS_SAMPLE_KIT_UPLOAD_RACKSCAN_MISMATCH), equalTo(""));
        assertThat((String) jsonObject.get(BspSampleData.PACKAGE_DATE), equalTo(""));
        assertThat((String) jsonObject.get(BspSampleData.RECEIPT_DATE), equalTo(""));
    }

    public void testBspSampleToJson() throws JSONException, ParseException {
        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
        data.put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, "collaborator sample");
        data.put(BSPSampleSearchColumn.PARTICIPANT_ID, "participant");
        data.put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, "collaborator participant");
        data.put(BSPSampleSearchColumn.VOLUME, "3");
        data.put(BSPSampleSearchColumn.CONCENTRATION, "1.2");
        data.put(BSPSampleSearchColumn.RIN, "1.2-3");
        data.put(BSPSampleSearchColumn.RQS, "5.0");
        data.put(BSPSampleSearchColumn.PICO_RUN_DATE, "05/23/2014");
        data.put(BSPSampleSearchColumn.TOTAL_DNA, "2.3");
        data.put(BSPSampleSearchColumn.RACKSCAN_MISMATCH, "true");
        SampleData bspSampleData = new BspSampleData(data);
        LabVessel tube = new BarcodedTube("0123");
        setPackageDate(tube, PACKAGE_DATE);
        setReceiptDate(tube, RECEIPT_DATE);
        String sampleName = "SM-1234";
        ProductOrderSample productOrderSample = new ProductOrderSample(sampleName, bspSampleData, 2L);
        MercurySample testSample = new MercurySample(sampleName, MercurySample.MetadataSource.BSP);
        testSample.addLabVessel(tube);
        productOrderSample.setMercurySample(testSample);
        productOrderSample.setLabEventSampleDTO(new LabEventSampleDTO(Collections.singleton(tube), sampleName));

        JSONObject jsonObject = factory.toJson(productOrderSample);

        assertThat((Long) jsonObject.get(BspSampleData.SAMPLE_ID), equalTo(2L));
        assertThat((String) jsonObject.get(BspSampleData.COLLABORATOR_SAMPLE_ID), equalTo("collaborator sample"));
        assertThat((String) jsonObject.get(BspSampleData.PATIENT_ID), equalTo("participant"));
        assertThat((String) jsonObject.get(BspSampleData.COLLABORATOR_PARTICIPANT_ID),
                equalTo("collaborator participant"));
        assertThat((Double) jsonObject.get(BspSampleData.VOLUME), equalTo(3.0));
        assertThat((Double) jsonObject.get(BspSampleData.CONCENTRATION), equalTo(1.2));
        assertThat((String) jsonObject.get(BspSampleData.JSON_RIN_KEY), equalTo("1.2-3"));
        assertThat((Double) jsonObject.get(BspSampleData.JSON_RQS_KEY), equalTo(5.0));
        assertThat((String) jsonObject.get(BspSampleData.PICO_DATE), equalTo("05/23/2014"));
        assertThat((Double) jsonObject.get(BspSampleData.TOTAL), equalTo(2.3));
        assertThat((Boolean) jsonObject.get(BspSampleData.HAS_SAMPLE_KIT_UPLOAD_RACKSCAN_MISMATCH), equalTo(true));
        assertThat((String) jsonObject.get(BspSampleData.PACKAGE_DATE), equalTo(PACKAGE_DATE));
        assertThat((String) jsonObject.get(BspSampleData.RECEIPT_DATE), equalTo(RECEIPT_DATE));
    }

    public void testBspSampleNoPico() throws JSONException {
        SampleData bspSampleData = new BspSampleData(new HashMap<BSPSampleSearchColumn, String>());
        ProductOrderSample productOrderSample = new ProductOrderSample("SM-1234", bspSampleData, 2L);

        JSONObject jsonObject = factory.toJson(productOrderSample);

        assertThat((String) jsonObject.get(BspSampleData.PICO_DATE), equalTo("No Pico"));
    }

    private void setReceiptDate(LabVessel tube, String date) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        tube.addInPlaceEvent(new LabEvent(LabEventType.SAMPLE_RECEIPT,
                dateFormat.parse(date), "testBspSampleToJson", 0L, 1L, "ProductOrderSampleJsonFactoryTest"));
    }

    private void setPackageDate(LabVessel tube, String date) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        tube.addInPlaceEvent(new LabEvent(LabEventType.SAMPLE_PACKAGE,
                dateFormat.parse(date), "testBspSampleToJson", 0L, 1L, "ProductOrderSampleJsonFactoryTest"));
    }
}
