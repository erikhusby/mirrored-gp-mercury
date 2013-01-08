package org.broadinstitute.gpinformatics.mercury.test;

import org.testng.annotations.Test;

/**
 * Test that samples can go partway through a workflow, be marked for rework, go to a previous
 * step, and then move to completion.
 */
public class ReworkTest {
    @Test
    public void testX() {
        // Advance to Pond Pico

        // Mark 2 samples rework
        // How to verify marked?  Users want to know how many times a MercurySample has been reworked
        // Advance rest of samples to end
        // Verify that repeated transition is flagged as error on non-reworked samples
        // Re-enter 2 samples at Pre-LC? (Re-entry points in a process are enabled / disabled on a per product basis)
        // Can rework one sample in a pool?  No.
    }
}
