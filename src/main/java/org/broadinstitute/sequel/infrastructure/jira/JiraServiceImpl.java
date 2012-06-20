package org.broadinstitute.sequel.infrastructure.jira;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.sequel.control.AbstractJsonJerseyClientService;
import org.broadinstitute.sequel.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.sequel.infrastructure.jira.customfields.CustomFieldJsonParser;
import org.broadinstitute.sequel.infrastructure.jira.issue.ChangeStringField;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueResponse;
import org.broadinstitute.sequel.infrastructure.jira.issue.Visibility;
import org.broadinstitute.sequel.infrastructure.jira.issue.comment.AddCommentRequest;
import org.broadinstitute.sequel.infrastructure.jira.issue.comment.AddCommentResponse;
import org.codehaus.jackson.map.ObjectMapper;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Default
public class JiraServiceImpl extends AbstractJsonJerseyClientService implements JiraService {

    private Log logger = LogFactory.getLog(JiraServiceImpl.class);

    private JiraConnectionParameters connectionParameters;

    private String baseUrl;

    @Inject
    public JiraServiceImpl(JiraConnectionParameters connectionParameters) {
        this.connectionParameters = connectionParameters;
    }

    @Override
    protected void customizeConfig(ClientConfig clientConfig) {
        supportJson(clientConfig);
    }

    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, connectionParameters);
    }



    private String getBaseUrl() {

        if (baseUrl == null) {

            String urlString = "http://%s:%d/rest/api/2";
            baseUrl = String.format(urlString, connectionParameters.getHostname(), connectionParameters.getPort());

        }

        return baseUrl;
    }





    @Override
    public CreateIssueResponse createIssue(String projectPrefix, CreateIssueRequest.Fields.Issuetype issueType, String summary, String description) throws IOException {

        CreateIssueRequest issueRequest = CreateIssueRequest.create(projectPrefix, issueType, summary, description);

        String urlString = getBaseUrl() + "/issue/";
        logger.debug("createIssue URL is " + urlString);


        WebResource webResource = getJerseyClient().resource(urlString);


        return post(webResource, issueRequest, new GenericType<CreateIssueResponse>() {});



    }


    @Override
    public void addComment(String key, String body) throws IOException {
        addComment(key, body, null, null);
    }

    
    @Override
    public void addComment(String key, String body, Visibility.Type visibilityType, Visibility.Value visibilityValue) throws IOException {

        AddCommentRequest addCommentRequest;
        
        if (visibilityType != null && visibilityValue != null)
            addCommentRequest = AddCommentRequest.create(key, body, visibilityType, visibilityValue);
        else
            addCommentRequest = AddCommentRequest.create(key, body);


        String urlString = getBaseUrl() + "/issue/" + key + "/comment";

        logger.debug("addComment URL is " + urlString);
        
        WebResource webResource = getJerseyClient().resource(urlString);

        // don't really care about the response, not sure why JIRA sends us back so much stuff...
        post(webResource, addCommentRequest, new GenericType<AddCommentResponse>() {
        });

    }


    @Override
    public List<CustomField> getCustomFields(CreateIssueRequest.Fields.Project project, CreateIssueRequest.Fields.Issuetype issueType) throws IOException {
        if (project == null) {
            throw new NullPointerException("project cannot be null");
        }
        if (issueType == null) {
            throw new NullPointerException("issueType cannot be null");
        }

        String urlString = getBaseUrl() + "/issue/createmeta?projectKeys=" + project.getKey() + "&issueTypeNames=" + issueType.getJiraName() + "&expand=projects.issuetypes.fields";

        String jsonResponse = getJerseyClient().resource(urlString).get(String.class);

        return CustomFieldJsonParser.parseCustomFields(jsonResponse);
    }

}
