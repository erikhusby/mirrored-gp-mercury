package org.broadinstitute.sequel.control.jira;

import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.sequel.control.AbstractJerseyClientService;
import org.broadinstitute.sequel.control.jira.issue.CreateRequest;
import org.broadinstitute.sequel.control.jira.issue.CreateResponse;
import org.codehaus.jackson.map.ObjectMapper;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class JiraServiceImpl extends AbstractJerseyClientService implements JiraService {
    
    @Inject
    private JiraConnectionParameters connectionParameters;

    private Log logger = LogFactory.getLog(JiraServiceImpl.class);

    public JiraServiceImpl() {
        super(true);
    }


    @Override
    public CreateResponse createIssue(String key, CreateRequest.Fields.Issuetype.IssuetypeName issuetypeName, String summary, String description) throws IOException {

        CreateRequest request = CreateRequest.create(key, issuetypeName, summary, description);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(baos, request);

        String urlString = "http://%s:%d/rest/api/2/issue/";
        urlString = String.format(urlString, connectionParameters.getHostname(), connectionParameters.getPort());

        logger.warn("URL is " + urlString);

        WebResource webResource = getClient(connectionParameters).resource(urlString);

        GenericType<CreateResponse> createResponseGenericType = new GenericType<CreateResponse>() {};

        logger.warn("create request is " + baos.toString());

        final CreateResponse createResponse =
                webResource.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).post(createResponseGenericType, baos.toString());

        logger.warn("create response is " + createResponse);

        return createResponse;

    }
}
