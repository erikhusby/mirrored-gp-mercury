package org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequestDefinitionInfo;
import org.broadinstitute.bsp.client.workrequest.WorkRequestManager;
import org.broadinstitute.bsp.client.workrequest.WorkRequestResponse;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * High-level APIs for using the BSP work request service to create sample kit requests.
 */
public class BSPKitRequestService {

    private final BSPWorkRequestClientService bspWorkRequestClientService;

    private final BSPManagerFactory bspManagerFactory;

    private final BSPUserList bspUserList;

    private static final String EMAIL_LIST_DELIMITER = ", ";

    @Inject
    public BSPKitRequestService(BSPWorkRequestClientService bspWorkRequestClientService,
                                BSPManagerFactory bspManagerFactory, BSPUserList bspUserList) {
        this.bspWorkRequestClientService = bspWorkRequestClientService;
        this.bspManagerFactory = bspManagerFactory;
        this.bspUserList = bspUserList;
    }

    /**
     * Creates a kit request work request in BSP for the given product order, site, and number of samples, and then
     * submits that request in BSP. This leaves it in a state in BSP where it is ready to be accepted, which fits the
     * BSP lab's current workflow. The additional details required by BSP are extracted from the given PDO and site or
     * are defaulted based on the current requirements (e.g., DNA kits shipped to the site's shipping contact).
     *
     * @param productOrder       the product order to create the kit request from
     *  @return the BSP work request ID
     */
    public String createAndSubmitKitRequestForPDO(ProductOrder productOrder) {
        BspUser creator = bspUserList.getById(productOrder.getCreatedBy());

        Long primaryInvestigatorId = null;
        Long externalCollaboratorId = null;
        ResearchProject researchProject = productOrder.getResearchProject();
        if (researchProject.getBroadPIs().length > 0) {
            BspUser broadPi = bspUserList.getById(researchProject.getBroadPIs()[0]);
            primaryInvestigatorId = broadPi.getDomainUserId();
            externalCollaboratorId = primaryInvestigatorId;
        }

        long projectManagerId;
        if (researchProject.getProjectManagers().length > 0) {
            BspUser projectManager = bspUserList.getById(researchProject.getProjectManagers()[0]);
            projectManagerId = projectManager.getDomainUserId();
        } else {
            projectManagerId = creator.getDomainUserId();
        }

        String workRequestName = String.format("%s - %s", productOrder.getName(), productOrder.getBusinessKey());
        String requesterId = creator.getUsername();

        ProductOrderKit kit = productOrder.getProductOrderKit();
        SampleKitWorkRequestDefinitionInfo kitWorkRequestDefinitionInfo =
                BSPWorkRequestFactory.buildBspKitWRDefinitionInfo(kit.getNumberOfSamples(), kit.getMaterialInfo(),
                        kit.getOrganismId(), kit.getPostReceiveOptions(), SampleKitWorkRequest.MoleculeType.DNA);
        SampleKitWorkRequest sampleKitWorkRequest = BSPWorkRequestFactory.buildBspKitWorkRequest(workRequestName,
                requesterId, productOrder.getBusinessKey(), primaryInvestigatorId, projectManagerId,
                externalCollaboratorId, kit.getSiteId(),
                kit.getSampleCollectionId(), getEmailList(kit.getNotificationIds()),
                kit.getComments(), kit.isExomeExpress(), kit.getTransferMethod(), kitWorkRequestDefinitionInfo);
        WorkRequestResponse createResponse = sendKitRequest(sampleKitWorkRequest);
        WorkRequestResponse submitResponse = submitKitRequest(createResponse.getWorkRequestBarcode());
        return submitResponse.getWorkRequestBarcode();
    }

    /**
     * Returns a comma separated list of e-mail addresses.
     */
    private String getEmailList(Long[] notificationIds) {
        List<String> emailList = new ArrayList<>(notificationIds.length);
        for (Long notificationId : notificationIds) {
            emailList.add(bspUserList.getById(notificationId).getEmail());
        }

        return StringUtils.join(emailList, EMAIL_LIST_DELIMITER);
    }

    /**
     * Sends the given kit request to BSP to create a work request.
     *
     * @param sampleKitWorkRequest    the kit request
     * @return the BSP work request service response
     */
    public WorkRequestResponse sendKitRequest(SampleKitWorkRequest sampleKitWorkRequest) {
        WorkRequestManager bspWorkRequestManager = bspManagerFactory.createWorkRequestManager();
        return bspWorkRequestClientService.createOrUpdateWorkRequest(bspWorkRequestManager, sampleKitWorkRequest);
    }

    /**
     * Requests that an existing work request be "submitted" in BSP.
     *
     * @param workRequestBarcode    the ID of the work request in BSP
     * @return the BSP work request service response
     */
    public WorkRequestResponse submitKitRequest(String workRequestBarcode) {
        WorkRequestManager bspWorkRequestManager = bspManagerFactory.createWorkRequestManager();
        return bspWorkRequestClientService.submitWorkRequest(workRequestBarcode, bspWorkRequestManager);
    }
}
