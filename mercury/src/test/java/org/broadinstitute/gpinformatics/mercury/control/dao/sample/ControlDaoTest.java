package org.broadinstitute.gpinformatics.mercury.control.dao.sample;

import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Scott Matthews
 *         Date: 2/21/13
 *         Time: 1:50 PM
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class ControlDaoTest extends StubbyContainerTest {

    public ControlDaoTest(){}

    @Inject
    ControlDao controlDao;

    @Inject
    UserTransaction utx;
    private int initialActiveSize;
    private int initialInactiveSize;
    private int initialActivePositiveSize;
    private int initalInactivePositiveSize;
    private int initalInactiveNegativeSize;
    private int initialActiveNegativeSize;

    @BeforeMethod
    public void setUp() throws Exception {
        if (utx == null) {
            return;
        }
        utx.begin();

        initialActiveSize = controlDao.findAllActive().size();
        initialInactiveSize = controlDao.findAllInactive().size();
        initialActivePositiveSize = controlDao.findAllActiveControlsByType(Control.ControlType.POSITIVE).size();
        initalInactivePositiveSize = controlDao.findAllInactiveControlsByType(Control.ControlType.POSITIVE).size();
        initalInactiveNegativeSize = controlDao.findAllInactiveControlsByType(Control.ControlType.NEGATIVE).size();
        initialActiveNegativeSize = controlDao.findAllActiveControlsByType(Control.ControlType.NEGATIVE).size();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (utx == null) {
            return;
        }
        utx.rollback();
    }

    public void testSimpleSave() throws Exception {

        final String testId = "Test_collaborator_id";
        Control testCtrl = new Control(testId, Control.ControlType.POSITIVE);

        controlDao.persist(testCtrl);
        controlDao.flush();
        controlDao.clear();

        Control newTestCtrl = controlDao.findByCollaboratorParticipantId(testId);

        Assert.assertNotNull(newTestCtrl);

        Assert.assertEquals(testId, newTestCtrl.getCollaboratorParticipantId());
        Assert.assertEquals(testId, newTestCtrl.getBusinessKey());

        Assert.assertEquals(Control.ControlType.POSITIVE, newTestCtrl.getType());

        Assert.assertEquals(Control.ControlState.ACTIVE, newTestCtrl.getState());

        List<Control> listOfOne = controlDao.findAllActive();
        Assert.assertEquals(1 + initialActiveSize, listOfOne.size());
        listOfOne = controlDao.findAllInactive();
        Assert.assertEquals(0 + initialInactiveSize, listOfOne.size());
        listOfOne = controlDao.findAllActiveControlsByType(Control.ControlType.POSITIVE);
        Assert.assertEquals(1 + initialActivePositiveSize, listOfOne.size());
        listOfOne = controlDao.findAllActiveControlsByType(Control.ControlType.NEGATIVE);
        Assert.assertEquals(0 + initialActiveNegativeSize, listOfOne.size());
        listOfOne = controlDao.findAllInactiveControlsByType(Control.ControlType.POSITIVE);
        Assert.assertEquals(0 + initalInactivePositiveSize, listOfOne.size());
        listOfOne = controlDao.findAllInactiveControlsByType(Control.ControlType.NEGATIVE);
        Assert.assertEquals(0 + initalInactiveNegativeSize, listOfOne.size());

    }

    public void testSimpleInactiveSave() throws Exception {

        final String testId = "Test_collaborator_id";
        Control testCtrl = new Control(testId, Control.ControlType.POSITIVE);
        testCtrl.setState(Control.ControlState.INACTIVE);

        controlDao.persist(testCtrl);
        controlDao.flush();
        controlDao.clear();

        Control newTestCtrl = controlDao.findByCollaboratorParticipantId(testId);

        Assert.assertNull(newTestCtrl);

        newTestCtrl = controlDao.findInactiveByCollaboratorParticipantId(testId);
        Assert.assertNotNull(newTestCtrl);

        List<Control> listOfOne = controlDao.findAllActive();
        Assert.assertEquals(0 + initialActiveSize, listOfOne.size());
        listOfOne = controlDao.findAllInactive();
        Assert.assertEquals(1 + initialInactiveSize, listOfOne.size());
        listOfOne = controlDao.findAllActiveControlsByType(Control.ControlType.POSITIVE);
        Assert.assertEquals(0 + initialActivePositiveSize, listOfOne.size());
        listOfOne = controlDao.findAllActiveControlsByType(Control.ControlType.NEGATIVE);
        Assert.assertEquals(0 + initialActiveNegativeSize, listOfOne.size());
        listOfOne = controlDao.findAllInactiveControlsByType(Control.ControlType.POSITIVE);
        Assert.assertEquals(1 + initalInactivePositiveSize, listOfOne.size());
        listOfOne = controlDao.findAllInactiveControlsByType(Control.ControlType.NEGATIVE);
        Assert.assertEquals(0 + initalInactiveNegativeSize, listOfOne.size());
    }

    public void testMultipleSave() throws Exception {

        final String testId1 = "Test_collaborator_id_1";
        final String testId2 = "Test_collaborator_id_2";
        final String testId3 = "Test_collaborator_id_3";
        final String testId4 = "Test_collaborator_id_4";
        final String testId5 = "Test_collaborator_id_5";

        Control testCtrl1 = new Control(testId1, Control.ControlType.POSITIVE);
        Control testCtrl2 = new Control(testId2, Control.ControlType.NEGATIVE);
        Control testCtrl3 = new Control(testId3, Control.ControlType.POSITIVE);
        Control testCtrl4 = new Control(testId4, Control.ControlType.NEGATIVE);
        Control testCtrl5 = new Control(testId5, Control.ControlType.POSITIVE);

        List<Control> listOfOriginals = new ArrayList<>(5);
        Collections.addAll(listOfOriginals, testCtrl1, testCtrl2, testCtrl3, testCtrl4, testCtrl5);

        controlDao.persistAll(listOfOriginals);
        controlDao.flush();
        controlDao.clear();

        Control newTestCtrl = controlDao.findByCollaboratorParticipantId(testId1);

        Assert.assertNotNull(newTestCtrl);

        List<Control> listOfOne = controlDao.findAllActive();
        Assert.assertEquals(5 + initialActiveSize, listOfOne.size());
        listOfOne = controlDao.findAllInactive();
        Assert.assertEquals(0 + initialInactiveSize, listOfOne.size());
        listOfOne = controlDao.findAllActiveControlsByType(Control.ControlType.POSITIVE);
        Assert.assertEquals(3 + initialActivePositiveSize, listOfOne.size());
        listOfOne = controlDao.findAllActiveControlsByType(Control.ControlType.NEGATIVE);
        Assert.assertEquals(2 + initialActiveNegativeSize, listOfOne.size());
        listOfOne = controlDao.findAllInactiveControlsByType(Control.ControlType.POSITIVE);
        Assert.assertEquals(0 + initalInactivePositiveSize, listOfOne.size());
        listOfOne = controlDao.findAllInactiveControlsByType(Control.ControlType.NEGATIVE);
        Assert.assertEquals(0 + initalInactiveNegativeSize, listOfOne.size());
    }

    public void testSimpleInactiveDuplicateSave() throws Exception {

        final String testId = "Test_collaborator_id";
        Control testCtrl = new Control(testId, Control.ControlType.POSITIVE);
        testCtrl.setState(Control.ControlState.INACTIVE);

        controlDao.persist(testCtrl);
        controlDao.flush();
        controlDao.clear();

        Control newTestCtrl = controlDao.findByCollaboratorParticipantId(testId);

        Assert.assertNull(newTestCtrl);

        Control testCtrlDupe = new Control(testId, Control.ControlType.POSITIVE);

        controlDao.persist(testCtrlDupe);
        controlDao.flush();
        controlDao.clear();

        Control newTestCtrlDupe = controlDao.findByCollaboratorParticipantId(testId);
        Assert.assertNotNull(newTestCtrlDupe);

        List<Control> listOfOne = controlDao.findAllActive();
        Assert.assertEquals(1 + initialActiveSize, listOfOne.size());
        listOfOne = controlDao.findAllInactive();
        Assert.assertEquals(1 + initialInactiveSize, listOfOne.size());
        listOfOne = controlDao.findAllActiveControlsByType(Control.ControlType.POSITIVE);
        Assert.assertEquals(1 + initialActivePositiveSize, listOfOne.size());
        listOfOne = controlDao.findAllActiveControlsByType(Control.ControlType.NEGATIVE);
        Assert.assertEquals(0 + initialActiveNegativeSize, listOfOne.size());
        listOfOne = controlDao.findAllInactiveControlsByType(Control.ControlType.POSITIVE);
        Assert.assertEquals(1 + initalInactivePositiveSize, listOfOne.size());
        listOfOne = controlDao.findAllInactiveControlsByType(Control.ControlType.NEGATIVE);
        Assert.assertEquals(0 + initalInactiveNegativeSize, listOfOne.size());

        try {
            Control testCtrlDupe2 = new Control(testId, Control.ControlType.POSITIVE);

            controlDao.persist(testCtrlDupe2);
            controlDao.flush();
            controlDao.clear();

            Assert.fail("Attempt to Save Dupe succeeded when it should not have.");

        } catch (Exception e) {

        }
    }
}
