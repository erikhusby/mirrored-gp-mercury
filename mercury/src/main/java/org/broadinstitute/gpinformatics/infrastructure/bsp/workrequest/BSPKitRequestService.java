package org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest;

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
 *
 */
public class BSPKitRequestService {

    private BSPWorkRequestClientService bspWorkRequestClientService;

    private BSPManagerFactory bspManagerFactory;

    private BSPUserList bspUserList;

    @Inject
    public BSPKitRequestService(BSPWorkRequestClientService bspWorkRequestClientService,
                                BSPManagerFactory bspManagerFactory, BSPUserList bspUserList) {
        this.bspWorkRequestClientService = bspWorkRequestClientService;
        this.bspManagerFactory = bspManagerFactory;
        this.bspUserList = bspUserList;
    }

    public String createAndSubmitKitRequestForPDO(ProductOrder productOrder, Long siteId, Long numberOfSamples) {
        BspUser creator = bspUserList.getById(productOrder.getCreatedBy());

        Long primaryInvestigatorId = null;
        Long externalCollaboratorId = null;
        ResearchProject researchProject = productOrder.getResearchProject();
        if (researchProject.getBroadPIs().length > 0) {
            BspUser broadPi = bspUserList.getById(researchProject.getBroadPIs()[0]);
            primaryInvestigatorId = broadPi.getDomainUserId();
            externalCollaboratorId = broadPi.getDomainUserId();
        }

        Long projectManagerId;
        if (researchProject.getProjectManagers().length > 0) {
            BspUser projectManager = bspUserList.getById(researchProject.getProjectManagers()[0]);
            projectManagerId = projectManager.getDomainUserId();
        } else {
            projectManagerId = creator.getDomainUserId();
        }

        String workRequestName = String.format("%s - %s", productOrder.getName(), productOrder.getBusinessKey());
        String requesterId = creator.getUsername();

        SampleKitWorkRequest sampleKitWorkRequest =
                new SampleKitWorkRequest(primaryInvestigatorId, projectManagerId, externalCollaboratorId, null,
                        workRequestName, requesterId, productOrder.getBusinessKey(), null, null, null, null, null,
                        SampleKitWorkRequest.MoleculeType.DNA, siteId, numberOfSamples,
                        SampleKitWorkRequest.TransferMethod.SHIP_OUT);
        WorkRequestResponse createResponse = sendKitRequest(sampleKitWorkRequest);
        WorkRequestResponse submitResponse = submitKitRequest(createResponse.getWorkRequestBarcode());
        return submitResponse.getWorkRequestBarcode();
    }

    public WorkRequestResponse sendKitRequest(SampleKitWorkRequest sampleKitWorkRequest) {
        WorkRequestManager bspWorkRequestManager = bspManagerFactory.createWorkRequestManager();
        WorkRequestResponse response =
                bspWorkRequestClientService.createOrUpdateWorkRequest(bspWorkRequestManager, sampleKitWorkRequest);
        return response;
    }

    public WorkRequestResponse submitKitRequest(String workRequestBarcode) {
        WorkRequestManager bspWorkRequestManager = bspManagerFactory.createWorkRequestManager();
        WorkRequestResponse response = bspWorkRequestClientService.submitWorkRequest(workRequestBarcode,
                bspWorkRequestManager);
        return response;
    }
}
