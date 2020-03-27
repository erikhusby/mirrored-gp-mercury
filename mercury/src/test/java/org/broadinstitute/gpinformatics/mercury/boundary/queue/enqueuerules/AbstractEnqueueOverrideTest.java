package org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules;

import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueuePriority;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.jetbrains.annotations.NotNull;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;

public abstract class AbstractEnqueueOverrideTest {

    AbstractEnqueueOverride enqueueOverride;

    /**
     * Expected to instantiate the enqueueOverride object.
     */
    public abstract void setup();

    /**
     * Test logic for testing the queue specific priority checks.
     */
    @SuppressWarnings("unused") // This is a test in the child classes.
    public abstract void queueSpecificCheckForSpecialPriorityTypeTest();

    /**
     * Verifies that the QueuePriorityOrder never contains STANDARD or ALTERED
     */
    @Test(groups = DATABASE_FREE)
    public final void getQueuePriorityOrderTest() {

        for (QueuePriority queuePriority : enqueueOverride.getQueuePriorityOrder()) {
            Assert.assertNotEquals(queuePriority, AbstractEnqueueOverride.getDefaultPriority());
            Assert.assertNotEquals(queuePriority, QueuePriority.ALTERED);
        }
    }

    @Test(groups = DATABASE_FREE)
    public final void useDefaultOrderingTest() {
        QueueGrouping queueGrouping = new QueueGrouping();

        long defaultId = 5000;
        queueGrouping.setQueueGroupingId(defaultId);

        enqueueOverride.useDefaultOrdering(queueGrouping);

        Assert.assertEquals(queueGrouping.getQueueGroupingId(), queueGrouping.getSortOrder());
    }

    @Test(groups = DATABASE_FREE)
    public final void determineQueuePriorityByUniqueness_QueuedPriorTest() {
        HashSet<Long> uniqueVesselIdsAlreadyInQueue = new HashSet<>();

        QueueGrouping queueGrouping = new QueueGrouping();
        List<QueueEntity> queuedEntities = new ArrayList<>();
        for (long i = 1; i < 5; i++) {
            LabVessel labVessel = generateHighestPriorityLevelLabVessel(i);
            // Mark samples as having been seen before
            uniqueVesselIdsAlreadyInQueue.add(i);
            queuedEntities.add(new QueueEntity(queueGrouping, labVessel));
        }
        queueGrouping.setQueuedEntities(queuedEntities);
        QueuePriority queuePriority = enqueueOverride.determineQueuePriority(queueGrouping, uniqueVesselIdsAlreadyInQueue);

        // Verify priority is standard as expected.
        Assert.assertEquals(queuePriority, QueuePriority.STANDARD);
    }

    @Test(groups = DATABASE_FREE)
    public final void determineQueuePriorityByUniqueness_NeverQueuedTest() {

        HashSet<Long> uniqueVesselIdsAlreadyInQueue = new HashSet<>();

        QueueGrouping queueGrouping = new QueueGrouping();
        List<QueueEntity> queuedEntities = new ArrayList<>();
        for (long i = 1; i < 5; i++) {
            LabVessel labVessel = generateHighestPriorityLevelLabVessel(i);
            queuedEntities.add(new QueueEntity(queueGrouping, labVessel));
        }
        queueGrouping.setQueuedEntities(queuedEntities);
        QueuePriority queuePriority = enqueueOverride.determineQueuePriority(queueGrouping, uniqueVesselIdsAlreadyInQueue);

        // Verify priority is standard as expected.
        if (enqueueOverride.getQueuePriorityOrder().length > 0) {
            Assert.assertEquals(queuePriority, enqueueOverride.getQueuePriorityOrder()[0]);
        } else {
            Assert.assertEquals(queuePriority, AbstractEnqueueOverride.getDefaultPriority());
        }
    }

    @Test(groups = DATABASE_FREE)
    public final void determineQueuePriorityByUniqueness_MixedTest() {
        HashSet<Long> uniqueVesselIdsAlreadyInQueue = new HashSet<>();

        QueueGrouping queueGrouping = new QueueGrouping();
        List<QueueEntity> queuedEntities = new ArrayList<>();
        for (long i = 1; i < 5; i++) {
            LabVessel labVessel = generateHighestPriorityLevelLabVessel(i);
            // Mark samples as having been seen before if they are an odd number.
            if (i % 2 == 1) {
                uniqueVesselIdsAlreadyInQueue.add(i);
            }
            queuedEntities.add(new QueueEntity(queueGrouping, labVessel));
        }
        queueGrouping.setQueuedEntities(queuedEntities);
        QueuePriority queuePriority = enqueueOverride.determineQueuePriority(queueGrouping, uniqueVesselIdsAlreadyInQueue);

        // Verify priority is standard as expected.
        if (enqueueOverride.getQueuePriorityOrder().length > 0) {
            Assert.assertEquals(queuePriority, enqueueOverride.getQueuePriorityOrder()[0]);
        } else {
            Assert.assertEquals(queuePriority, AbstractEnqueueOverride.getDefaultPriority());
        }
    }

    /**
     * The purpose of this is to thoroughly test the first aspect of the determineQueuePriority(QueueGrouping) method.
     *
     * This test should be implemented in such a way as the first entity in the list be expected to be STANDARD,
     * then following that each priority possibility in ascending order.
     */
    @Test(groups = DATABASE_FREE)
    public abstract void determineQueuePriority_MercurySamplesTest();

    /**
     * The purpose of this is to thoroughly test the second aspect of the determineQueuePriority(QueueGrouping) method.
     *
     * This test should be implemented in such a way as the first entity in the list be expected to be STANDARD,
     * then following that each priority possibility in ascending order.
     */
    @Test(groups = DATABASE_FREE)
    public abstract void determineQueuePriority_SampleInstanceV2Test();

    /**
     * Should generate a lab vessel specific to the Override being tested which is the highest possible priority.
     *
     * @param labVesselIdToSet  LabVesselID to put into the lab vessel.
     * @return                  Generated LabVessel
     */
    @NotNull
    protected abstract LabVessel generateHighestPriorityLevelLabVessel(long labVesselIdToSet);
}
