package org.broadinstitute.gpinformatics.infrastructure.submission;

import com.sun.jersey.api.client.Client;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.inject.Inject;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class SubmissionsService extends AbstractJerseyClientService {

    private final SubmissionConfig submissionsConfig;

    @Inject
    public SubmissionsService(SubmissionConfig submissionsConfig) {
        this.submissionsConfig = submissionsConfig;
    }


    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, submissionsConfig);
    }

}
