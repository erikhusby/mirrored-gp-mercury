package org.broadinstitute.gpinformatics.infrastructure.jira;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldJsonParser;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.Visibility;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.comment.AddCommentRequest;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.comment.AddCommentResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.IssueTransitionRequest;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.IssueTransitionResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.Transition;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJsonJerseyClientService;
import org.codehaus.jackson.map.ObjectMapper;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
        log = LogFactory.getLog(JiraServiceImpl.class);
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
            baseUrl = jiraConfig.getUrlBase() + "/rest/api/2";
        }
        return baseUrl;
    }

    @Override
    public CreateIssueResponse createIssue(String projectPrefix, String reporter, CreateIssueRequest.Fields.Issuetype issueType,
                                           String summary, String description,
                                           Collection<CustomField> customFields) throws IOException {

        CreateIssueRequest issueRequest = CreateIssueRequest.create(projectPrefix, reporter, issueType, summary, description,customFields);

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
    public void addComment(String key, String body, Visibility.Type visibilityType,
                           Visibility.Value visibilityValue) throws IOException {

        AddCommentRequest addCommentRequest;
        
        if (visibilityType != null && visibilityValue != null) {
            addCommentRequest = AddCommentRequest.create(body, visibilityType, visibilityValue);
        } else {
            addCommentRequest = AddCommentRequest.create(body);
        }


        String urlString = getBaseUrl() + "/issue/" + key + "/comment";

        log.debug("addComment URL is " + urlString);
        
        WebResource webResource = getJerseyClient().resource(urlString);

        // don't really care about the response, not sure why JIRA sends us back so much stuff...
        post(webResource, addCommentRequest, new GenericType<AddCommentResponse>() {
        });

    }

    @Override
    public void addLink(AddIssueLinkRequest.LinkType type,
                        String sourceIssueIn,
                        String targetIssueIn) throws IOException {
        addLink(type, sourceIssueIn, targetIssueIn, null, null, null);
    }

    @Override
    public void addLink(AddIssueLinkRequest.LinkType type, String sourceIssueIn,
                        String targetIssueIn,
                        String commentBody,
                        Visibility.Type availabilityType,
                        Visibility.Value availabilityValue) throws IOException {

        AddIssueLinkRequest linkRequest;

        if (commentBody != null && availabilityType != null && availabilityValue != null) {
            linkRequest = AddIssueLinkRequest.create(type, sourceIssueIn, targetIssueIn,
                    commentBody, availabilityType, availabilityValue);
        } else {
            linkRequest = AddIssueLinkRequest.create(type, sourceIssueIn, targetIssueIn);
        }

        String urlString = getBaseUrl() + "/issueLink";

        WebResource webResource = getJerseyClient().resource(urlString);
        post(webResource, linkRequest);
    }

    @Override
    public void addWatcher(String key, String watcherId) throws IOException{
        WebResource webResource = getJerseyClient().resource(getBaseUrl() + "/issue/" + key + "/watchers");
        post(webResource, watcherId);
    }

    @Override
    public Map<String, CustomFieldDefinition> getRequiredFields(@Nonnull CreateIssueRequest.Fields.Project project,
                                                                @Nonnull CreateIssueRequest.Fields.Issuetype issueType) throws IOException {
        if (project == null) {
            throw new NullPointerException("jira project cannot be null");
        }
        if (issueType == null) {
            throw new NullPointerException("issueType cannot be null");
        }

        String urlString = getBaseUrl() + "/issue/createmeta";

        String jsonResponse = getJerseyClient().resource(urlString)
                .queryParam("projectKeys", project.getKey())
                .queryParam("issueTypeNames", issueType.getJiraName())
                .queryParam("expand", "projects.issuetypes.fields")
                .get(String.class);

        return CustomFieldJsonParser.parseRequiredFields(jsonResponse);
    }

    @Override
    public Map<String, CustomFieldDefinition> getCustomFields() throws IOException {
        String urlString = getBaseUrl() + "/field";

        String jsonResponse = getJerseyClient().resource(urlString).get(String.class);
        return CustomFieldJsonParser.parseCustomFields(jsonResponse);
    }

    @Override
    public String createTicketUrl(String jiraTicketName) {

        // Until such a time as I can remove this method, it's delegated to Jira Config
        return jiraConfig.createTicketUrl(jiraTicketName);
    }

    @Override
    public IssueTransitionResponse findAvailableTransitions(String jiraIssueKey) {
        String urlString = getBaseUrl() + "/" + jiraIssueKey + "/transitions";

        WebResource webResource =
                getJerseyClient().resource(urlString).queryParam("expand", "transitions.fields");

        return get(webResource, new GenericType<IssueTransitionResponse>(){});
    }

    @Override
    public void postNewTransition(String jiraIssueKey, IssueTransitionRequest jiraIssueTransition) throws IOException {
        String urlString = getBaseUrl() + "/" + jiraIssueKey + "/transitions";
        WebResource webResource = getJerseyClient().resource(urlString);
        post(webResource, jiraIssueTransition);
    }

    @Override
    public void postNewTransition(String jiraIssueKey, String transitionId) throws IOException {
        IssueTransitionRequest jiraIssueTransition = new IssueTransitionRequest(new Transition(transitionId));

        String urlString = getBaseUrl() + "/" + jiraIssueKey + "/transitions";
        WebResource webResource = getJerseyClient().resource(urlString);
        post(webResource, jiraIssueTransition);
    }

    // Validate the current user by using the JIRA user/search API.  We have a match if we get a response back
    // whose name exactly matches the name we're searching for.  The API excludes inactive users by default.
    @Override
    public boolean isValidUser(String username) {
        String urlString = getBaseUrl() + "/user/search";

        String jsonResponse = getJerseyClient().resource(urlString).
                queryParam("username", username).
                queryParam("maxResults", "1").get(String.class);
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> response = new ObjectMapper().readValue(jsonResponse, List.class);
            if (!response.isEmpty()) {
                String foundName = response.get(0).get("name");
                // JIRA usernames are not case sensitive.
                return foundName.equalsIgnoreCase(username);
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }
}
