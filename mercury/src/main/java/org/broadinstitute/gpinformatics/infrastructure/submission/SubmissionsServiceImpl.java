package org.broadinstitute.gpinformatics.infrastructure.submission;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;
import org.broadinstitute.gpinformatics.mercury.control.JerseyUtils;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class SubmissionsServiceImpl implements SubmissionsService{

    private final SubmissionConfig submissionsConfig;

    @Inject
    public SubmissionsServiceImpl(SubmissionConfig submissionsConfig) {
        this.submissionsConfig = submissionsConfig;
    }


    @Override
    public SubmissionStatusResults getSubmissionStatus(String... uuids) {

        Map<String, List<String>> submissionParameters = new HashMap<>();

        submissionParameters.put("uuid", Arrays.asList(uuids));

        ClientResponse response =
                JerseyUtils.getWebResource(submissionsConfig.getUrl(), MediaType.APPLICATION_JSON_TYPE,
                        submissionParameters).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        return response.getEntity(SubmissionStatusResults.class);
    }
}
