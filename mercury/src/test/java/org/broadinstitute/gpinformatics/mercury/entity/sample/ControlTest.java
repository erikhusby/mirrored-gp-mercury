package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;

/**
 * @author Scott Matthews
 *         Date: 2/21/13
 *         Time: 10:52 AM
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ControlTest {

    private final String na12878 = "NA12878";

    @Test(groups = DATABASE_FREE)
    public void testControlCreation() {

        Control testCtrl = new Control(na12878, Control.ControlType.POSITIVE);

        Assert.assertNotNull(testCtrl);

        Assert.assertEquals(na12878, testCtrl.getCollaboratorParticipantId());
        Assert.assertEquals(na12878, testCtrl.getBusinessKey());

        Assert.assertEquals(Control.ControlType.POSITIVE, testCtrl.getType());

        Assert.assertEquals(Control.ControlState.ACTIVE, testCtrl.getState());

        final String sampleId2 = "dummy_part_2";

        testCtrl.setCollaboratorParticipantId(sampleId2);

        Assert.assertEquals(testCtrl.getCollaboratorParticipantId(), sampleId2);
        Assert.assertEquals(testCtrl.getBusinessKey(), sampleId2);
    }

    @Test(groups = DATABASE_FREE)
    public void testJustAccessors() throws Exception {

        final String testId = "Test_Sample_ID";

        Control testCtrl = new Control();

        Assert.assertEquals(testCtrl.getState(), Control.ControlState.ACTIVE);

        testCtrl.setCollaboratorParticipantId(testId);

        Assert.assertEquals(testCtrl.getCollaboratorParticipantId(), testId);
        Assert.assertEquals(testCtrl.getBusinessKey(), testId);

        testCtrl.setType(Control.ControlType.NEGATIVE);

        Assert.assertEquals(testCtrl.getType(), Control.ControlType.NEGATIVE);

        testCtrl.setType(Control.ControlType.POSITIVE);

        Assert.assertEquals(testCtrl.getType(), Control.ControlType.POSITIVE);

        testCtrl.setState(Control.ControlState.ACTIVE);

        Assert.assertEquals(testCtrl.getState(), Control.ControlState.ACTIVE);

        testCtrl.setState(Control.ControlState.INACTIVE);

        Assert.assertEquals(testCtrl.getState(), Control.ControlState.INACTIVE);
        Assert.assertFalse(testCtrl.isActive());
    }

    @Test(groups = DATABASE_FREE, expectedExceptions = InformaticsServiceException.class)
    public void testStateEnum() {

        Assert.assertEquals(Control.ControlState.ACTIVE.getDisplayName(), "Active");
        Control.ControlState testState = Control.ControlState.findByDisplayName("Active");

        Assert.assertEquals(testState, Control.ControlState.ACTIVE);

        Assert.assertEquals(Control.ControlState.INACTIVE.getDisplayName(), "Inactive");
        testState = Control.ControlState.findByDisplayName("Inactive");

        Assert.assertEquals(testState, Control.ControlState.INACTIVE);

        testState = Control.ControlState.findByDisplayName("Dummy");
    }

    @Test(groups = DATABASE_FREE, expectedExceptions = InformaticsServiceException.class)
    public void testTypeEnum() {

        Assert.assertEquals(Control.ControlType.POSITIVE.getDisplayName(), "Positive");
        Control.ControlType testState = Control.ControlType.findByDisplayName("Positive");

        Assert.assertEquals(testState, Control.ControlType.POSITIVE);

        Assert.assertEquals(Control.ControlType.NEGATIVE.getDisplayName(), "Negative");
        testState = Control.ControlType.findByDisplayName("Negative");

        Assert.assertEquals(testState, Control.ControlType.NEGATIVE);

        testState = Control.ControlType.findByDisplayName("Dummy");
    }

}
