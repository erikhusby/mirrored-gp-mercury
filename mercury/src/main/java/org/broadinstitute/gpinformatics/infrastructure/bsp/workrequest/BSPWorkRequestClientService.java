package org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest;

import com.sun.jersey.api.client.Client;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.workrequest.SeqPlatingWorkRequest;
import org.broadinstitute.bsp.client.workrequest.WorkRequest;
import org.broadinstitute.bsp.client.workrequest.WorkRequestManager;
import org.broadinstitute.bsp.client.workrequest.WorkRequestResponse;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.inject.Inject;

/**
 * Common code for BSP service clients for working with various types of BSP work requests.
 *
 * This is currently used as both a parent class for
 * {@link org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPPlatingRequestServiceImpl} and as a dependency
 * for {@link BSPKitRequestService}. Eventually, it should probably be a dependency for both classes, which should help
 * to make each easier to test. However, as more functionality is extracted from BSPPlatingRequestServiceImpl into this
 * class, keeping it as a parent class makes the refactoring convenient. Once all of the actual web service client code
 * has been extracted, BSPPlatingRequestServiceImpl can use this class as a dependency instead of a parent class.
 */
public class BSPWorkRequestClientService extends AbstractJerseyClientService {

    private final Log log = LogFactory.getLog(BSPWorkRequestClientService.class);

    private final BSPConfig bspConfig;

    public BSPWorkRequestClientService() {
        bspConfig = null;
    }

    @Inject
    public BSPWorkRequestClientService(BSPConfig bspConfig) {
        this.bspConfig = bspConfig;
    }

    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, bspConfig);
    }

    protected WorkRequestResponse createOrUpdateWorkRequest(WorkRequestManager bspWorkRequestManager,
                                                            WorkRequest workRequest) {
        WorkRequestResponse response;

        if (workRequest.getBarCode() == null) {
            response = bspWorkRequestManager.create(workRequest);
        } else {
            response = bspWorkRequestManager.update(workRequest);
        }

        if (response == null) {
            String message = "null response from PlatingRequestManager trying to create/update plating work request " + workRequest.getWorkRequestName();
            log.error(message);
            throw new RuntimeException(message);
        }

        if (!response.isSuccess()) {
            String message = String.format("Errors trying to create/update plating work request \"%s\": %s", workRequest.getWorkRequestName(), response.getErrors().toString());
            log.error(message);
            throw new RuntimeException(message);
        }
        return response;
    }
}
