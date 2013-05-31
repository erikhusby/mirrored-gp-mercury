package org.broadinstitute.gpinformatics.mercury.boundary.rapsheet;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Tests the ReworkEjb logic in a DBFree manner.  This test should cover the basic functionality while remaining
 * (relatively) speedy on execution
 */
public class ReworkEjbDaoFreeTest extends BaseEventTest {

    @Test(groups = TestGroups.DATABASE_FREE)
    public void reworkDaoFree() throws ValidationException {

        ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(1);
        AthenaClientServiceStub.addProductOrder(productOrder);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        ReworkEjb reworkEjb = new ReworkEjb();

        Collection<MercurySample> validationMessages =
                reworkEjb.getVesselRapSheet(mapBarcodeToTube.values().iterator().next(),
                        ReworkEntry.ReworkReason.UNKNOWN_ERROR, ReworkEntry.ReworkLevel.ONE_SAMPLE_HOLD_REST_BATCH,
                        LabEventType.PICO_PLATING_BUCKET, "", WorkflowName.EXOME_EXPRESS.getWorkflowName());

        Assert.assertEquals(1, validationMessages.size());

        Assert.assertEquals(productOrder.getSamples().iterator().next().getBspSampleName(),
                validationMessages.iterator().next().getBspSampleName());

    }

}
