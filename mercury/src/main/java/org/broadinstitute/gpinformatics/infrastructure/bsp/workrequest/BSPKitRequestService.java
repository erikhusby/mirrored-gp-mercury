package org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequestDefinitionInfo;
import org.broadinstitute.bsp.client.workrequest.WorkRequestManager;
import org.broadinstitute.bsp.client.workrequest.WorkRequestResponse;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKitDetail;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * High-level APIs for using the BSP work request service to create sample kit requests.
 */
@Dependent
public class BSPKitRequestService implements Serializable {

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
     * @param productOrder the product order to create the kit request from
     *
     * @return the BSP work request ID
     */
    public String createAndSubmitKitRequestForPDO(ProductOrder productOrder) {

        Long primaryInvestigatorId = null;
        Long externalCollaboratorId = null;
        ResearchProject researchProject = productOrder.getResearchProject();
        if (researchProject.getBroadPIs().length > 0) {
            BspUser broadPi = bspUserList.getById(researchProject.getBroadPIs()[0]);
            primaryInvestigatorId = broadPi.getDomainUserId();
            externalCollaboratorId = primaryInvestigatorId;
        }

        // The creator should be the project manager, if there is one, otherwise, will take the actual creator as
        // the project manager and hope for the best. Project Managers are required for the collaboration portal,
        // so this will work. For initiation PDOs, this is what was happening, generally.
        BspUser creator = bspUserList.getById(productOrder.getCreatedBy());
        if (researchProject.getProjectManagers().length > 0) {
            creator = bspUserList.getById(researchProject.getProjectManagers()[0]);
        }

        String workRequestName = String.format("%s - %s", productOrder.getName(), productOrder.getBusinessKey());
        String requesterId = creator.getUsername();

        ProductOrderKit kit = productOrder.getProductOrderKit();
        List<SampleKitWorkRequestDefinitionInfo> kitDefinitions = new ArrayList<>(kit.getKitOrderDetails().size());

        for (ProductOrderKitDetail kitDetail : kit.getKitOrderDetails()) {
            SampleKitWorkRequestDefinitionInfo kitWorkRequestDefinitionInfo =
                    BSPWorkRequestFactory.buildBspKitWRDefinitionInfo(kitDetail, SampleKitWorkRequest.MoleculeType.DNA);
            kitDefinitions.add(kitWorkRequestDefinitionInfo);
        }

        SampleKitWorkRequest sampleKitWorkRequest = BSPWorkRequestFactory.buildBspKitWorkRequest(workRequestName,
                requesterId, productOrder.getBusinessKey(), primaryInvestigatorId, creator.getDomainUserId(),
                externalCollaboratorId, kit.getSiteId(),
                kit.getSampleCollectionId(), getEmailList(kit.getNotificationIds()),
                kit.getComments(), kit.isExomeExpress(), kit.getTransferMethod(), kitDefinitions);
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
     * @param sampleKitWorkRequest the kit request
     *
     * @return the BSP work request service response
     */
    public WorkRequestResponse sendKitRequest(SampleKitWorkRequest sampleKitWorkRequest) {
        WorkRequestManager bspWorkRequestManager = bspManagerFactory.createWorkRequestManager();
        return bspWorkRequestClientService.createOrUpdateWorkRequest(bspWorkRequestManager, sampleKitWorkRequest);
    }

    /**
     * Requests that an existing work request be "submitted" in BSP.
     *
     * @param workRequestBarcode the ID of the work request in BSP
     *
     * @return the BSP work request service response
     */
    public WorkRequestResponse submitKitRequest(String workRequestBarcode) {
        WorkRequestManager bspWorkRequestManager = bspManagerFactory.createWorkRequestManager();
        return bspWorkRequestClientService.submitWorkRequest(workRequestBarcode, bspWorkRequestManager);
    }
}
