package org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest;

import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;

/**
 * A factory class for creating BSP WorkRequest objects of various types (e.g., SampleKitWorkRequest) from data
 * collected by Mercury.
 */
public class BSPWorkRequestFactory {

    /**
     * Creates a {@link SampleKitWorkRequest} suitable for posting to the BSP work request creation service.
     *
     * TODO: clarify PI and PM for the request
     *
     * @param workRequestName          the name of the work request; must be unique in BSP
     * @param requestUser              the user making the request
     * @param productOrderId           the product order associated with the request
     * @param primaryInvestigatorId    the domain user ID for ???
     * @param projectManagerId         the domain user ID for ???
     * @param siteId                   the BSP ID of the site that the sample kits should be shipped to
     * @param numberOfSamples          the total number of samples that the kit should contain
     * @return a new SampleKitWorkRequest
     */
    public static SampleKitWorkRequest buildBspKitWorkRequest(String workRequestName, String requestUser,
                                                              String productOrderId, long primaryInvestigatorId,
                                                              long projectManagerId,
                                                              long siteId, Long numberOfSamples) {
        SampleKitWorkRequest workRequest = new SampleKitWorkRequest(
                primaryInvestigatorId, // primaryInvestigatorId
                projectManagerId, // projectManagerId
                null, // externalCollaboratorId
                null, // barCode
                workRequestName, // workRequestName
                requestUser, // requestUser
                productOrderId, // pdoId
                null, // status
                null, // notificationList
                null, // errors
                null, // warnings
                null, // info
                null, // moleculeType
                siteId, // siteId
                numberOfSamples, // numberOfSamples
                SampleKitWorkRequest.TransferMethod.SHIP_OUT // transferMethod
        );

        return workRequest;
    }
}
