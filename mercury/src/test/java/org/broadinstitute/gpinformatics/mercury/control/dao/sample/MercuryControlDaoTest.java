package org.broadinstitute.gpinformatics.mercury.control.dao.sample;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercuryControl;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class MercuryControlDaoTest extends ContainerTest {

    @Inject
    MercuryControlDao mercuryControlDao;

    @Inject
    UserTransaction utx;

    @BeforeMethod
    public void setUp() throws Exception {

        if (utx == null) {
            return;
        }

        utx.begin();
    }

    @AfterMethod
    public void tearDown() throws Exception {

        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    public void testSimpleSave() throws Exception {

        int initialActiveSize = mercuryControlDao.findAllActive().size();
        int initialInactiveSize = mercuryControlDao.findAllInactive().size();
        int initialActivePositiveSize = mercuryControlDao.findAllActivePositiveControls().size();
        int initialActiveNegativeSize = mercuryControlDao.findAllActiveNegativeControls().size();
        int initalInactivePositiveSize = mercuryControlDao.findAllInactivePositiveControls().size();
        int initalInactiveNegativeSize = mercuryControlDao.findAllInactiveNegativeControls().size();


        final String testId = "Test_collaborator_id";
        MercuryControl testCtrl = new MercuryControl(testId, MercuryControl.ControlType.POSITIVE);

        mercuryControlDao.persist(testCtrl);
        mercuryControlDao.flush();
        mercuryControlDao.clear();

        MercuryControl newTestCtrl = mercuryControlDao.findBySampleId(testId);

        Assert.assertNotNull(newTestCtrl);

        Assert.assertEquals(testId, newTestCtrl.getCollaboratorSampleId());

        Assert.assertEquals(MercuryControl.ControlType.POSITIVE, newTestCtrl.getType());

        Assert.assertEquals(MercuryControl.ControlState.ACTIVE, newTestCtrl.getState());

        List<MercuryControl> listOfOne = mercuryControlDao.findAllActive();
        Assert.assertEquals(1 + initialActiveSize, listOfOne.size());
        listOfOne = mercuryControlDao.findAllInactive();
        Assert.assertEquals(0 + initialInactiveSize, listOfOne.size());
        listOfOne = mercuryControlDao.findAllActivePositiveControls();
        Assert.assertEquals(1+initialActivePositiveSize, listOfOne.size());
        listOfOne = mercuryControlDao.findAllActiveNegativeControls();
        Assert.assertEquals(0+initialActiveNegativeSize, listOfOne.size());
        listOfOne = mercuryControlDao.findAllInactivePositiveControls();
        Assert.assertEquals(0+initalInactivePositiveSize, listOfOne.size());
        listOfOne = mercuryControlDao.findAllInactiveNegativeControls();
        Assert.assertEquals(0+initalInactiveNegativeSize, listOfOne.size());

    }

    public void testSimpleInactiveSave() throws Exception {

        int initialActiveSize = mercuryControlDao.findAllActive().size();
        int initialInactiveSize = mercuryControlDao.findAllInactive().size();
        int initialActivePositiveSize = mercuryControlDao.findAllActivePositiveControls().size();
        int initialActiveNegativeSize = mercuryControlDao.findAllActiveNegativeControls().size();
        int initalInactivePositiveSize = mercuryControlDao.findAllInactivePositiveControls().size();
        int initalInactiveNegativeSize = mercuryControlDao.findAllInactiveNegativeControls().size();


        final String testId = "Test_collaborator_id";
        MercuryControl testCtrl = new MercuryControl(testId, MercuryControl.ControlType.POSITIVE);
        testCtrl.toggleState();

        mercuryControlDao.persist(testCtrl);
        mercuryControlDao.flush();
        mercuryControlDao.clear();

        MercuryControl newTestCtrl = mercuryControlDao.findBySampleId(testId);

        Assert.assertNull(newTestCtrl);

        newTestCtrl = mercuryControlDao.findInactiveBySampleId(testId);
        Assert.assertNotNull(newTestCtrl);

        List<MercuryControl> listOfOne = mercuryControlDao.findAllActive();
        Assert.assertEquals(0+initialActiveSize, listOfOne.size());
        listOfOne = mercuryControlDao.findAllInactive();
        Assert.assertEquals(1+initialInactiveSize, listOfOne.size());
        listOfOne = mercuryControlDao.findAllActivePositiveControls();
        Assert.assertEquals(0+initialActivePositiveSize, listOfOne.size());
        listOfOne = mercuryControlDao.findAllActiveNegativeControls();
        Assert.assertEquals(0+initialActiveNegativeSize, listOfOne.size());
        listOfOne = mercuryControlDao.findAllInactivePositiveControls();
        Assert.assertEquals(1+initalInactivePositiveSize, listOfOne.size());
        listOfOne = mercuryControlDao.findAllInactiveNegativeControls();
        Assert.assertEquals(0+initalInactiveNegativeSize, listOfOne.size());
    }

    public void testMultipleSave() throws Exception {

        int initialActiveSize = mercuryControlDao.findAllActive().size();
        int initialInactiveSize = mercuryControlDao.findAllInactive().size();
        int initialActivePositiveSize = mercuryControlDao.findAllActivePositiveControls().size();
        int initialActiveNegativeSize = mercuryControlDao.findAllActiveNegativeControls().size();
        int initalInactivePositiveSize = mercuryControlDao.findAllInactivePositiveControls().size();
        int initalInactiveNegativeSize = mercuryControlDao.findAllInactiveNegativeControls().size();

        final String testId1 = "Test_collaborator_id_1";
        final String testId2 = "Test_collaborator_id_2";
        final String testId3 = "Test_collaborator_id_3";
        final String testId4 = "Test_collaborator_id_4";
        final String testId5 = "Test_collaborator_id_5";

        MercuryControl testCtrl1 = new MercuryControl(testId1, MercuryControl.ControlType.POSITIVE);
        MercuryControl testCtrl2 = new MercuryControl(testId2, MercuryControl.ControlType.NEGATIVE);
        MercuryControl testCtrl3 = new MercuryControl(testId3, MercuryControl.ControlType.POSITIVE);
        MercuryControl testCtrl4 = new MercuryControl(testId4, MercuryControl.ControlType.NEGATIVE);
        MercuryControl testCtrl5 = new MercuryControl(testId5, MercuryControl.ControlType.POSITIVE);

        List<MercuryControl> listOfOriginals = new ArrayList<MercuryControl>(5);
        Collections.addAll(listOfOriginals, testCtrl1, testCtrl2, testCtrl3, testCtrl4, testCtrl5);

        mercuryControlDao.persistAll(listOfOriginals);
        mercuryControlDao.flush();
        mercuryControlDao.clear();

        MercuryControl newTestCtrl = mercuryControlDao.findBySampleId(testId1);

        Assert.assertNotNull(newTestCtrl);

        List<MercuryControl> listOfOne = mercuryControlDao.findAllActive();
        Assert.assertEquals(5 + initialActiveSize, listOfOne.size());
        listOfOne = mercuryControlDao.findAllInactive();
        Assert.assertEquals(0 + initialInactiveSize, listOfOne.size());
        listOfOne = mercuryControlDao.findAllActivePositiveControls();
        Assert.assertEquals(3 + initialActivePositiveSize, listOfOne.size());
        listOfOne = mercuryControlDao.findAllActiveNegativeControls();
        Assert.assertEquals(2 + initialActiveNegativeSize, listOfOne.size());
        listOfOne = mercuryControlDao.findAllInactivePositiveControls();
        Assert.assertEquals(0 + initalInactivePositiveSize, listOfOne.size());
        listOfOne = mercuryControlDao.findAllInactiveNegativeControls();
        Assert.assertEquals(0 + initalInactiveNegativeSize, listOfOne.size());
    }

    public void testSimpleInactiveDuplicateSave() throws Exception {

        int initialActiveSize = mercuryControlDao.findAllActive().size();
        int initialInactiveSize = mercuryControlDao.findAllInactive().size();
        int initialActivePositiveSize = mercuryControlDao.findAllActivePositiveControls().size();
        int initialActiveNegativeSize = mercuryControlDao.findAllActiveNegativeControls().size();
        int initalInactivePositiveSize = mercuryControlDao.findAllInactivePositiveControls().size();
        int initalInactiveNegativeSize = mercuryControlDao.findAllInactiveNegativeControls().size();


        final String testId = "Test_collaborator_id";
        MercuryControl testCtrl = new MercuryControl(testId, MercuryControl.ControlType.POSITIVE);
        testCtrl.toggleState();

        mercuryControlDao.persist(testCtrl);
        mercuryControlDao.flush();
        mercuryControlDao.clear();

        MercuryControl newTestCtrl = mercuryControlDao.findBySampleId(testId);

        Assert.assertNull(newTestCtrl);

        MercuryControl testCtrlDupe = new MercuryControl(testId, MercuryControl.ControlType.POSITIVE);

        mercuryControlDao.persist(testCtrlDupe);
        mercuryControlDao.flush();
        mercuryControlDao.clear();

        MercuryControl newTestCtrlDupe = mercuryControlDao.findBySampleId(testId);
        Assert.assertNotNull(newTestCtrlDupe);

        List<MercuryControl> listOfOne = mercuryControlDao.findAllActive();
        Assert.assertEquals(1+initialActiveSize, listOfOne.size());
        listOfOne = mercuryControlDao.findAllInactive();
        Assert.assertEquals(1+initialInactiveSize, listOfOne.size());
        listOfOne = mercuryControlDao.findAllActivePositiveControls();
        Assert.assertEquals(1+initialActivePositiveSize, listOfOne.size());
        listOfOne = mercuryControlDao.findAllActiveNegativeControls();
        Assert.assertEquals(0+initialActiveNegativeSize, listOfOne.size());
        listOfOne = mercuryControlDao.findAllInactivePositiveControls();
        Assert.assertEquals(1+initalInactivePositiveSize, listOfOne.size());
        listOfOne = mercuryControlDao.findAllInactiveNegativeControls();
        Assert.assertEquals(0+initalInactiveNegativeSize, listOfOne.size());

        try {
            MercuryControl testCtrlDupe2 = new MercuryControl(testId, MercuryControl.ControlType.POSITIVE);

            mercuryControlDao.persist(testCtrlDupe2);
            mercuryControlDao.flush();
            mercuryControlDao.clear();

            Assert.fail();

        } catch (Exception e) {

        }

    }

}
