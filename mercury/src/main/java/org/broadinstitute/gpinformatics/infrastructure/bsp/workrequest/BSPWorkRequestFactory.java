package org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest;

import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.PostReceiveOption;
import org.broadinstitute.bsp.client.sample.MaterialInfoDto;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequestDefinitionInfo;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKitDetail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A factory class for creating BSP WorkRequest objects of various types (e.g., SampleKitWorkRequest) from data
 * collected by Mercury.
 */
public class BSPWorkRequestFactory {

    /**
     * Creates a {@link SampleKitWorkRequest} suitable for posting to the BSP work request creation service.
     *
     * @param workRequestName        the name of the work request; must be unique in BSP
     * @param requestUser            the user making the request
     * @param productOrderId         the product order associated with the request
     * @param primaryInvestigatorId  the domain user ID for the Broad PI
     * @param projectManagerId       the domain user ID for the Broad PM
     * @param externalCollaboratorId the domain user ID for the external collaborator
     * @param siteId                 the site that the sample kits will be shipped to
     * @param sampleCollectionId     the collection to use for the sample kit work task
     * @param notificationList       the comma separated list of users to notify on kit shipment.
     * @param notes                  comments to pass on to bsp
     * @param exExKit                flag if this is an exome express kit
     * @param transferMethod         How are they receiving the kit, pick up or delivery?
     * @param kitDefinitionInfo
     * @return a new SampleKitWorkRequest
     */
    public static SampleKitWorkRequest buildBspKitWorkRequest(String workRequestName, String requestUser,
                                                              String productOrderId, Long primaryInvestigatorId,
                                                              Long projectManagerId, Long externalCollaboratorId,
                                                              Long siteId,
                                                              Long sampleCollectionId,
                                                              String notificationList,
                                                              String notes,
                                                              boolean exExKit,
                                                              SampleKitWorkRequest.TransferMethod transferMethod,
                                                              List<SampleKitWorkRequestDefinitionInfo> kitDefinitionInfo) {

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
                siteId,
                sampleCollectionId,
                transferMethod, // transferMethod
                notes,
                exExKit,
                kitDefinitionInfo            //Kit Definition
        );
    }

    /**
     * Helper method for building a bspClient sample kit work request definition info for the purpose of sending
     * across a web service to bsp for building a sample initiation work request.
     *
     * @param numberOfSamples       Number of samples to be associated with the kit detail information
     * @param materialInfoDto       Type of source material to be associated with the kit detail information
     * @param organismId            Reference ID of the organism to be associated with the kit detail information
     * @param postReceiveOptions    instructions for processing the source material after it is received by the kit
     *                              team
     * @param molecularType         Molecular make up information to be associated with the kit detail information
     * @return
     */
    public static SampleKitWorkRequestDefinitionInfo buildBspKitWRDefinitionInfo(long numberOfSamples,
                                                                                 MaterialInfoDto materialInfoDto,
                                                                                 long organismId,
                                                                                 Set<PostReceiveOption> postReceiveOptions,
                                                                                 SampleKitWorkRequest.MoleculeType molecularType) {

        return new SampleKitWorkRequestDefinitionInfo(numberOfSamples, molecularType, materialInfoDto, organismId,
                new ArrayList<>(postReceiveOptions));
    }

    /**
     * Helper method for building a bspClient sample kit work request definition info for the purpose of sending
     * across a web service to bsp for building a sample initiation work request.
     *
     * @param kitDetail         an instance of a ProductOrderKitDetail entity that contains the sample kit request
     *                          data
     * @param molecularType     Molecular make up information to be associated with the kit detail information
     * @return
     */
    public static SampleKitWorkRequestDefinitionInfo buildBspKitWRDefinitionInfo(ProductOrderKitDetail kitDetail,
                                                                                 SampleKitWorkRequest.MoleculeType molecularType) {

        return buildBspKitWRDefinitionInfo(kitDetail.getNumberOfSamples(), kitDetail.getMaterialInfo(),
                kitDetail.getOrganismId(), kitDetail.getPostReceiveOptions(), molecularType);
    }
}
