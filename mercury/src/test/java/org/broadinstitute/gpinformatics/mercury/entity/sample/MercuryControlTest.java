package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;

/**
 * @author Scott Matthews
 *         Date: 2/21/13
 *         Time: 10:52 AM
 */
public class MercuryControlTest {

    private final String na12878 = "NA12878";

    @Test(groups = DATABASE_FREE)
    public void testControlCreation() {

        MercuryControl testCtrl = new MercuryControl(na12878, MercuryControl.ControlType.POSITIVE);

        Assert.assertNotNull(testCtrl);

        Assert.assertEquals(na12878, testCtrl.getCollaboratorSampleId());

        Assert.assertEquals(MercuryControl.ControlType.POSITIVE, testCtrl.getType());

        Assert.assertEquals(MercuryControl.ControlState.ACTIVE, testCtrl.getState());

    }

    @Test(groups = DATABASE_FREE)
    public void testStateSwitch() {

        final String sampleId = "Dummy_Collaborator_1";

        MercuryControl testCtrl = new MercuryControl(sampleId, MercuryControl.ControlType.POSITIVE);

        Assert.assertNotNull(testCtrl);

        Assert.assertEquals(sampleId, testCtrl.getCollaboratorSampleId());

        Assert.assertEquals(MercuryControl.ControlState.ACTIVE, testCtrl.getState());

        testCtrl.toggleState();

        Assert.assertEquals(MercuryControl.ControlState.INACTIVE, testCtrl.getState());

    }

}
