package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
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

        final String sampleId2 = "dummy_part_2";

        testCtrl.setCollaboratorSampleId(sampleId2);

        Assert.assertEquals(testCtrl.getCollaboratorSampleId(), sampleId2);
    }

    @Test(groups = DATABASE_FREE)
    public void testJustAccessors() throws Exception {

        final String testId = "Test_Sample_ID";

        MercuryControl testCtrl = new MercuryControl();

        Assert.assertEquals(testCtrl.getState(), MercuryControl.ControlState.ACTIVE);

        testCtrl.setCollaboratorSampleId(testId);

        Assert.assertEquals(testCtrl.getCollaboratorSampleId(), testId);

        testCtrl.setType(MercuryControl.ControlType.NEGATIVE);

        Assert.assertEquals(testCtrl.getType(), MercuryControl.ControlType.NEGATIVE);

        testCtrl.setType(MercuryControl.ControlType.POSITIVE.getDisplayName());

        Assert.assertEquals(testCtrl.getType(), MercuryControl.ControlType.POSITIVE);

        testCtrl.setState(MercuryControl.ControlState.ACTIVE);

        Assert.assertEquals(testCtrl.getState(), MercuryControl.ControlState.ACTIVE);

        testCtrl.setState(MercuryControl.ControlState.INACTIVE);

        Assert.assertEquals(testCtrl.getState(), MercuryControl.ControlState.INACTIVE);
        Assert.assertTrue(testCtrl.isActive());
    }

    @Test(groups = DATABASE_FREE, expectedExceptions = InformaticsServiceException.class)
    public void testStateEnum() {

        Assert.assertEquals(MercuryControl.ControlState.ACTIVE.getDisplayName(), "Active");
        MercuryControl.ControlState testState = MercuryControl.ControlState.findByDisplayName("Active");

        Assert.assertEquals(testState, MercuryControl.ControlState.ACTIVE);

        Assert.assertEquals(MercuryControl.ControlState.INACTIVE.getDisplayName(), "Inactive");
        testState = MercuryControl.ControlState.findByDisplayName("Inactive");

        Assert.assertEquals(testState, MercuryControl.ControlState.INACTIVE);

        testState = MercuryControl.ControlState.findByDisplayName("Dummy");
    }

    @Test(groups = DATABASE_FREE, expectedExceptions = InformaticsServiceException.class)
    public void testTypeEnum() {

        Assert.assertEquals(MercuryControl.ControlType.POSITIVE.getDisplayName(), "Positive");
        MercuryControl.ControlType testState = MercuryControl.ControlType.findByDisplayName("Positive");

        Assert.assertEquals(testState, MercuryControl.ControlType.POSITIVE);

        Assert.assertEquals(MercuryControl.ControlType.NEGATIVE.getDisplayName(), "Negative");
        testState = MercuryControl.ControlType.findByDisplayName("Negative");

        Assert.assertEquals(testState, MercuryControl.ControlType.NEGATIVE);

        testState = MercuryControl.ControlType.findByDisplayName("Dummy");
    }

}
