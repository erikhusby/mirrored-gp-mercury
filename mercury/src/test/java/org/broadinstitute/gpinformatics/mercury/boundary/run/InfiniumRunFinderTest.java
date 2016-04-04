package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSetVolumeConcentration;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSetVolumeConcentrationProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.InfiniumEntityBuilder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * User: jowalsh
 * Date: 4/2/16
 */
public class InfiniumRunFinderTest {

    private InfiniumRunFinder runFinder;

    @BeforeMethod
    public void setUp() throws Exception {
        int numSamples = 94;
        ProductOrder productOrder = ProductOrderTestFactory.buildInfiniumProductOrder(numSamples);
        List<StaticPlate> plates = LabEventTest.buildSamplePlates(productOrder, "AmpPlate");
        StaticPlate plate = plates.get(0);
        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);
        BSPUserList testUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());
        BSPSetVolumeConcentration bspSetVolumeConcentration = BSPSetVolumeConcentrationProducer.stubInstance();
        LabEventFactory  labEventFactory = new LabEventFactory(testUserList, bspSetVolumeConcentration);
        LabEventHandler labEventHandler = new LabEventHandler();
        InfiniumEntityBuilder infiniumEntityBuilder = new InfiniumEntityBuilder(
                bettaLimsMessageTestFactory, labEventFactory, labEventHandler, plate, "Inf").invoke();

        runFinder = new InfiniumRunFinder();

        LabEventDao labEventDao = mock(LabEventDao.class);
        when(labEventDao.findByEventType(LabEventType.INFINIUM_XSTAIN)).thenReturn(
                infiniumEntityBuilder.getxStainEvents());
        runFinder.setLabEventDao(labEventDao);
    }

    @Test
    public void testFind() throws Exception {
        List<LabVessel> chips = runFinder.listPendingXStainChips();
        assertNotNull(chips);
    }
}