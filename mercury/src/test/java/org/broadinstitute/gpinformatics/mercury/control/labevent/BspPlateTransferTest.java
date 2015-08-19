package org.broadinstitute.gpinformatics.mercury.control.labevent;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.DenatureToDilutionTubeHandler;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.EventHandlerSelector;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.FlowcellLoadedHandler;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.FlowcellMessageHandler;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.SamplesDaughterPlateHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExtractionsBloodJaxbBuilder;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Date;

@Test(groups = {TestGroups.DATABASE_FREE})
public class BspPlateTransferTest extends BaseEventTest {
    private Date runDate = new Date();
    private boolean isPosted = false;

    private DenatureToDilutionTubeHandler denatureToDilutionTubeHandler =
            EasyMock.createMock(DenatureToDilutionTubeHandler.class);
    private FlowcellMessageHandler flowcellMessageHandler =
            EasyMock.createMock(FlowcellMessageHandler.class);
    private FlowcellLoadedHandler flowcellLoadedHandler =
            EasyMock.createMock(FlowcellLoadedHandler.class);
    private Object[] mocks = {denatureToDilutionTubeHandler, flowcellMessageHandler, flowcellLoadedHandler};

    private SamplesDaughterPlateHandler samplesDaughterPlateHandler = new SamplesDaughterPlateHandler() {
        public void postToBsp(StationEventType stationEvent, String bspRestUrl) {
            isPosted = true;
        }
    };

    private EventHandlerSelector testMe = new EventHandlerSelector(denatureToDilutionTubeHandler,
            flowcellMessageHandler, samplesDaughterPlateHandler, flowcellLoadedHandler, null);

    private ExtractionsBloodJaxbBuilder builder = new ExtractionsBloodJaxbBuilder(getBettaLimsMessageTestFactory(),
            "sendToBspTest" + runDate.getTime(), Collections.singletonList("A12341234"), "barcode1",
            "barcode2", "barcode3", Collections.singletonList("A56785678")).invoke();

    @BeforeMethod
    public void setup() {
        EasyMock.reset(mocks);
        EasyMock.replay(mocks);
        isPosted = false;
    }

    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testBlood() throws Exception {
        testMe.applyEventSpecificHandling(
                new LabEvent(LabEventType.BLOOD_CRYOVIAL_EXTRACTION, runDate, "HAM", 1L, 1L, "app"),
                builder.getBloodCryovialTransfer());
        Assert.assertTrue(isPosted);
        EasyMock.verify(mocks);
    }

    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testNoSend() throws Exception {
        testMe.applyEventSpecificHandling(
                new LabEvent(LabEventType.A_BASE, runDate, "HAM", 1L, 1L, "app"),
                builder.getBloodCryovialTransfer());
        Assert.assertFalse(isPosted);
        EasyMock.verify(mocks);
    }
}
