package org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest;

import org.broadinstitute.bsp.client.collection.SampleCollection;
import org.broadinstitute.bsp.client.sample.MaterialInfo;
import org.broadinstitute.bsp.client.site.Site;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;
import org.broadinstitute.bsp.client.workrequest.WorkRequestManager;
import org.broadinstitute.bsp.client.workrequest.WorkRequestResponse;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;

import javax.inject.Inject;

/**
 * High-level APIs for using the BSP work request service to create sample kit requests.
 */
public class BSPKitRequestService {

    private final BSPWorkRequestClientService bspWorkRequestClientService;

    private final BSPManagerFactory bspManagerFactory;

    private final BSPUserList bspUserList;

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
     * @param site               the site that the kit should be shipped to
     * @param numberOfSamples    the number of samples to put in the kit
     * @param materialInfo       materialInfo of the kit request
     * @param notificationList   comma separated list of e-mails
     *
     * @return the BSP work request ID
     */
    public String createAndSubmitKitRequestForPDO(ProductOrder productOrder, Site site, long numberOfSamples,
                                                  MaterialInfo materialInfo, SampleCollection collection,
                                                  String notificationList, long organismId) {
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

        SampleKitWorkRequest sampleKitWorkRequest = BSPWorkRequestFactory.buildBspKitWorkRequest(workRequestName,
                requesterId, productOrder.getBusinessKey(), primaryInvestigatorId, projectManagerId,
                externalCollaboratorId, site, numberOfSamples, materialInfo, collection, notificationList,
                organismId);
        WorkRequestResponse createResponse = sendKitRequest(sampleKitWorkRequest);
        WorkRequestResponse submitResponse = submitKitRequest(createResponse.getWorkRequestBarcode());
        return submitResponse.getWorkRequestBarcode();
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
