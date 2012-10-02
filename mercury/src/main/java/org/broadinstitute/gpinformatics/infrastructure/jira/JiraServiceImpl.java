package org.broadinstitute.gpinformatics.infrastructure.jira;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJsonJerseyClientService;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldJsonParser;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.Visibility;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.comment.AddCommentRequest;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.comment.AddCommentResponse;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Impl
public class JiraServiceImpl extends AbstractJsonJerseyClientService implements JiraService {


    @Inject
    private JiraConfig jiraConfig;

    @Inject
    private Log log;


    private String baseUrl;


    public JiraServiceImpl() {}


    /**
     * Non-CDI constructor
     *
     * @param jiraConfig
     */
    public JiraServiceImpl(JiraConfig jiraConfig) {
        this.jiraConfig = jiraConfig;
        this.log = LogFactory.getLog(JiraServiceImpl.class);
    }

    @Override
    protected void customizeConfig(ClientConfig clientConfig) {
        supportJson(clientConfig);
    }

    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, jiraConfig);
    }



    private String getBaseUrl() {

        if (baseUrl == null) {

            String urlString = "http://%s:%d/rest/api/2";
            baseUrl = String.format(urlString, jiraConfig.getHost(), jiraConfig.getPort());

        }

        return baseUrl;
    }





    @Override
    public CreateIssueResponse createIssue(String projectPrefix, CreateIssueRequest.Fields.Issuetype issueType,
                                           String summary, String description,
                                           Collection<CustomField> customFields) throws IOException {

        CreateIssueRequest issueRequest = CreateIssueRequest.create(projectPrefix, issueType, summary, description,customFields);

        String urlString = getBaseUrl() + "/issue/";
        log.debug("createIssue URL is " + urlString);

        /**
         * To debug this, use curl like so:
         * $ curl  -u squid:squid -X POST -d @curl -H "Content-Type: application/json" http://vsquid00.broadinstitute.org:8020/rest/api/2/issue/
         *
         * Where @curl is a file name and contains something like:
         * {"fields":{"description":"Description created from Mercury","project":{"key":"LCSET"},"customfield_10020":"doofus","customfield_10011":"9999","summary":"Summary created from Mercury","issuetype":{"name":"Whole Exome (HybSel)"}}}
         */

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

        log.debug("addComment URL is " + urlString);
        
        WebResource webResource = getJerseyClient().resource(urlString);

        // don't really care about the response, not sure why JIRA sends us back so much stuff...
        post(webResource, addCommentRequest, new GenericType<AddCommentResponse>() {
        });

    }


    @Override
    public List<CustomFieldDefinition> getCustomFields(CreateIssueRequest.Fields.Project project, CreateIssueRequest.Fields.Issuetype issueType) throws IOException {
        if (project == null) {
            throw new NullPointerException("project cannot be null");
        }
        if (issueType == null) {
            throw new NullPointerException("issueType cannot be null");
        }

        String urlString = getBaseUrl() + "/field";

        String jsonResponse = getJerseyClient().resource(urlString)
                .get(String.class);

        return CustomFieldJsonParser.parseCustomFields(jsonResponse);
    }

    @Override
    public String createTicketUrl(String jiraTicketName) {
        return "http://" + jiraConfig.getHost() + ":" + jiraConfig.getPort() + "/browse/" + jiraTicketName;
    }
}
