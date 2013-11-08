package org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest;

import org.broadinstitute.bsp.client.collection.SampleCollection;
import org.broadinstitute.bsp.client.sample.MaterialInfo;
import org.broadinstitute.bsp.client.site.Site;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;

import java.util.Collections;

/**
 * A factory class for creating BSP WorkRequest objects of various types (e.g., SampleKitWorkRequest) from data
 * collected by Mercury.
 */
public class BSPWorkRequestFactory {

    /**
     * Creates a {@link SampleKitWorkRequest} suitable for posting to the BSP work request creation service.
     *
     *
     *
     * @param workRequestName        the name of the work request; must be unique in BSP
     * @param requestUser            the user making the request
     * @param productOrderId         the product order associated with the request
     * @param primaryInvestigatorId  the domain user ID for the Broad PI
     * @param projectManagerId       the domain user ID for the Broad PM
     * @param externalCollaboratorId the domain user ID for the external collaborator
     * @param site                   the site that the sample kits will be shipped to
     * @param numberOfSamples        the total number of samples that the kit should contain
     * @param materialInfo           the material type
     * @param collection             the collection to use for the sample kit work task
     * @param organism               the organism that the user selected
     * @param notificationList       the comma separated list of users to notify via completion
     *
     * @return a new SampleKitWorkRequest
     */
    public static SampleKitWorkRequest buildBspKitWorkRequest(String workRequestName, String requestUser,
                                                              String productOrderId, Long primaryInvestigatorId,
                                                              Long projectManagerId, Long externalCollaboratorId,
                                                              Site site, long numberOfSamples,
                                                              MaterialInfo materialInfo, SampleCollection collection,
                                                              String organism, String notificationList) {

        return new SampleKitWorkRequest(
                primaryInvestigatorId,
                projectManagerId,
                externalCollaboratorId,
                null, // barCode
                workRequestName,
                requestUser,
                productOrderId,
                null, // status
                notificationList, // notificationList
                Collections.<String>emptyList(), // errors
                Collections.<String>emptyList(), // warnings
                Collections.<String>emptyList(), // info
                SampleKitWorkRequest.MoleculeType.DNA, // moleculeType
                site.getId(),
                numberOfSamples,
                collection.getCollectionId(),
                SampleKitWorkRequest.TransferMethod.SHIP_OUT, // transferMethod
                materialInfo
        );
    }
}
