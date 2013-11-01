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
     * @param workRequestName          the name of the work request; must be unique in BSP
     * @param requestUser              the user making the request
     * @param productOrderId           the product order associated with the request
     * @param primaryInvestigatorId    the domain user ID for the Broad PI
     * @param projectManagerId         the domain user ID for the Broad PM
     * @param externalCollaboratorId   the domain user ID for the external collaborator
     * @param siteId                   the BSP ID of the site that the sample kits should be shipped to
     * @param numberOfSamples          the total number of samples that the kit should contain
     * @return a new SampleKitWorkRequest
     */
    public static SampleKitWorkRequest buildBspKitWorkRequest(String workRequestName, String requestUser,
                                                              String productOrderId, Long primaryInvestigatorId,
                                                              Long projectManagerId, Long externalCollaboratorId,
                                                              long siteId, long numberOfSamples) {

        return new SampleKitWorkRequest(
                primaryInvestigatorId, // primaryInvestigatorId
                projectManagerId, // projectManagerId
                externalCollaboratorId, // externalCollaboratorId
                null, // barCode
                workRequestName, // workRequestName
                requestUser, // requestUser
                productOrderId, // pdoId
                null, // status
                null, // notificationList
                null, // errors
                null, // warnings
                null, // info
                SampleKitWorkRequest.MoleculeType.DNA, // moleculeType
                siteId, // siteId
                numberOfSamples, // numberOfSamples
                SampleKitWorkRequest.TransferMethod.SHIP_OUT // transferMethod
        );
    }
}
