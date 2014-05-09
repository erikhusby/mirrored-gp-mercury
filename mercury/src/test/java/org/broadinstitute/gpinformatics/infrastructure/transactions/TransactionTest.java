package org.broadinstitute.gpinformatics.infrastructure.transactions;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for our expectations of how the extended persistence context should interact with container managed
 * transactions.
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class TransactionTest extends ContainerTest {

    public static final BigDecimal DESIRED_VOLUME = BigDecimal.valueOf(3);
    @Inject
    private VanillaDao dao;

    @Inject
    private TransactionTestEjb ejb;

    private Long plateId;

    @BeforeMethod
    public void setUp() {
        if (dao == null) {
            return;
        }

        String plateBarcode = "TransactionTest" + System.currentTimeMillis();
        StaticPlate plate = new StaticPlate(plateBarcode, StaticPlate.PlateType.Eppendorf96);
        dao.persist(plate);
        plateId = plate.getLabVesselId();

        dao.flush();
        dao.clear();
    }

    /**
     * This test currently fails. The desired behavior is for this test to succeed without changing the implementation
     * of {@link TransactionTestEjb#updateVolume(StaticPlate, BigDecimal)}.
     */
    public void testModifyEntityWithoutEngagingEntityManager() {
        StaticPlate plate = findPlate();
        ejb.updateVolume(plate, DESIRED_VOLUME);

        clear();
        assertVolume();
    }

    public void testFlushEntityChangesMadeBeforeTransactionWithoutEngagingEntityManager() {
        StaticPlate plate = findPlate();
        plate.setVolume(DESIRED_VOLUME);
        ejb.doNothing();

        clear();
        assertVolume();
    }

    public void testFlushEntityChangesMadeBeforeTransactionWithFind() {
        StaticPlate plate = findPlate();
        plate.setVolume(DESIRED_VOLUME);
        ejb.doSomething();

        clear();
        assertVolume();
    }

    public void testModifyEntityWithDaoFind() {
        ejb.updateVolumeWithFind(plateId, DESIRED_VOLUME);

        clear();
        assertVolume();
    }

    public void testModifyEntityWithDaoPersist() {
        StaticPlate plate = findPlate();
        ejb.updateVolumeWithPersist(plate, DESIRED_VOLUME);

        clear();
        assertVolume();
    }

    public void testModifyEntityWithUnrelatedFind() {
        StaticPlate plate = findPlate();
        ejb.updateVolumeWithUnrelatedFind(plate, DESIRED_VOLUME);

        clear();
        assertVolume();
    }

    private void clear() {
        // No flush, because we need to observe the behavior of the EJB method, not flush() (which should be MANDATORY, not REQUIRED!)
        dao.clear();
    }

    private StaticPlate findPlate() {
        return dao.findById(StaticPlate.class, plateId);
    }

    private void assertVolume() {
        StaticPlate plate = findPlate();
        assertThat(plate.getVolume(), equalTo(DESIRED_VOLUME));
    }
}
