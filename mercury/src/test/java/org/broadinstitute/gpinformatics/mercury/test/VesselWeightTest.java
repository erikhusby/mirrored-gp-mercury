package org.broadinstitute.gpinformatics.mercury.test;

import org.testng.annotations.Test;

/**
 * Tests InitialTare and SampleReceipt messages
 */
public class VesselWeightTest {
    @Test
    public void testEndToEnd() {
        // The following is a plan for GPLIM-2079
        // todo add receptacleWeight (as opposed to sampleWeight) to bettalims.xsd receptacleType
        // todo add weight attribute to LabVessel
        // todo update LabVessel weight somewhere in LabEventFactory (location TBD)
        // todo add INITIAL_TARE to LabEventType
        // todo add weight to data warehouse?

        // todo add weight handling to LabEventFactory.updateVolumeConcentration
        // todo BSP add receptacleWeight to SetVolumeAndConcentration, use it to set "Tare Weight" annotation

        // Send an InitialTare message with receptacleWeight
        // Verify that BSP Tare Weight annotation is set
        // http://bsp/ws/bsp/sample/gettareweight?manufacturer_barcodes=1082117278
        // Send a SampleReceipt message with volume (and weight?)
        // Verify that BSP volume is set
    }
}
