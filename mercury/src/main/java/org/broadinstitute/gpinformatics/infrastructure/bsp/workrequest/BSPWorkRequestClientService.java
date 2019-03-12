package org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.workrequest.WorkRequest;
import org.broadinstitute.bsp.client.workrequest.WorkRequestManager;
import org.broadinstitute.bsp.client.workrequest.WorkRequestResponse;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJaxRsClientService;

import javax.inject.Inject;
import javax.ws.rs.client.Client;

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
public class BSPWorkRequestClientService extends AbstractJaxRsClientService {

    private final Log log = LogFactory.getLog(BSPWorkRequestClientService.class);

    private final BSPConfig bspConfig;

    public BSPWorkRequestClientService() {
        this(null);
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
            response = bspWorkRequestManager.create(workRequest.getRequestUser(), workRequest);
        } else {
            response = bspWorkRequestManager.update(workRequest.getRequestUser(), workRequest);
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

    protected WorkRequestResponse submitWorkRequest(String wrBarcode, WorkRequestManager bspWorkRequestManager) {
        WorkRequestResponse submissionResponse = bspWorkRequestManager.submit(wrBarcode);

        // this REALLY should not happen if we've gotten this far; I've only
        // ever seen this for mismatched client/server jars and we should
        // have found out about that in our initial connection to the WR
        // manager.
        if (submissionResponse == null) {

            final String msg = String.format("Error submitting BSP Plating Work Request '%s'", wrBarcode);

            log.error(msg);
            throw new RuntimeException(msg);
        }

        // log.warn("Skipping submission response check due to bogus BSP errors!");

        if (!submissionResponse.isSuccess()) {

            final String msg = String.format(
                    "Found errors attempting to submit BSP WR %s: %s",
                    wrBarcode, submissionResponse.getErrors());

            log.error(msg);
            throw new RuntimeException(submissionResponse.getErrors().toString());
        }

        // success!
        log.info("Submission successful!");

        return submissionResponse;
    }
}
