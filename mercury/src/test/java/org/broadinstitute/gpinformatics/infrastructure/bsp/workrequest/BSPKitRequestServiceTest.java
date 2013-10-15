package org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest;

import org.testng.annotations.BeforeMethod;

/**
 */
public class BSPKitRequestServiceTest {

    private BSPKitRequestService bspKitRequestService;

    @BeforeMethod
    public void setUp() {
        bspKitRequestService = new BSPKitRequestService(null, null, null);
    }
}
