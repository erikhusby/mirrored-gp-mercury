package org.broadinstitute.gpinformatics.infrastructure.common;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.IssueTransitionResponse;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

/**
 * ServiceAccessUtility is a helper class to enable objects that are not capable of injecting Integration layer
 * service objects the ability to invoke methods contained in the integration layer.
 *
 * @author Scott Matthews
 *         Date: 10/11/12
 *         Time: 12:17 PM
 */
public class ServiceAccessUtility {

    private abstract static class Caller<RESULT_TYPE, API_CLASS> {

        abstract RESULT_TYPE call(API_CLASS apiInstance);

        public RESULT_TYPE apiCall(Type classType) {

            RESULT_TYPE foundServiceObject = null;

            try {
                InitialContext initialContext = new InitialContext();
                try {
                    BeanManager beanManager = (BeanManager) initialContext.lookup("java:comp/BeanManager");
                    Bean<?> bean = beanManager.getBeans(classType).iterator().next();
                    CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
                    @SuppressWarnings("unchecked")
                    API_CLASS apiInstance = (API_CLASS) beanManager.getReference(bean, bean.getClass(), ctx);
                    foundServiceObject = call(apiInstance);
                } finally {
                    initialContext.close();
                }
            } catch (NamingException e) {
                throw new RuntimeException(e);
            }

            return foundServiceObject;
        }
    }

    /**
     * Returns the base URL, including context path, of this Mercury deployment.
     *
     * @return the base Mercury URL
     */
    public static String getMercuryUrl() {
        return (new Caller<String, MercuryConfig>() {
            @Override
            String call(MercuryConfig apiInstance) {
                return apiInstance.getUrl();
            }
        }).apiCall(MercuryConfig.class);
    }

    /**
     * getSampleDtoByName exposes an integration layer service call to retrieve a BSP Sample DTo based on a sample
     * name
     *
     * @param sampleName a BSP sample name for which a user wishes to find BSP Meta Data
     * @return an instance of a BSP Sample DTO
     */
    public static BSPSampleDTO getSampleDtoByName(final String sampleName) {
        return (new Caller<BSPSampleDTO, BSPSampleDataFetcher>() {
            @Override
            BSPSampleDTO call(BSPSampleDataFetcher apiInstance) {
                return apiInstance.fetchSingleSampleFromBSP(sampleName);
            }
        }).apiCall(BSPSampleDataFetcher.class);
    }

    public static BspUser getBspUserForId(final long userId) {
        return (new Caller<BspUser, BSPUserList>() {
            @Override
            BspUser call(BSPUserList apiInstance) {
                return apiInstance.getById(userId);
            }
        }).apiCall(BSPUserList.class);
    }

    /**
     * getSampleDtoByNames exposes an integration layer service call to retrieve a BSP Sample DTo based on a collection
     * of sample names
     *
     * @param sampleNames Collection of Sample Names for which the user wishes to find BSP Meta Data
     * @return a Map of BSP Sample Dtos indexed by the Sample Name associated with the DTO
     */
    public static Map<String, BSPSampleDTO> getSampleDtoByNames ( Collection<String> sampleNames ) {

        Map<String, BSPSampleDTO> foundServiceObject = null;

        try {
            InitialContext initialContext = new InitialContext();
            try{
                BeanManager beanManager = (BeanManager) initialContext.lookup("java:comp/BeanManager");
                Bean bean = beanManager.getBeans(BSPSampleDataFetcher.class).iterator().next();
                CreationalContext ctx = beanManager.createCreationalContext(bean);
                BSPSampleDataFetcher bspSampleSearchService =
                        (BSPSampleDataFetcher) beanManager.getReference(bean, bean.getClass(), ctx);
                foundServiceObject = bspSampleSearchService.fetchSamplesFromBSP(sampleNames);
            } finally {
                initialContext.close();
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }

        return foundServiceObject;
    }

    // todo jmt use this in some of the other methods in this class
    /**
     * Gets a CDI bean of the given type
     * @param beanType  class of the bean
     * @param <T> type of the bean
     * @return CDI bean
     */
    public static <T> T getBean(Class<T> beanType) {
        try {
            InitialContext initialContext = new InitialContext();
            try{
                BeanManager beanManager = (BeanManager) initialContext.lookup("java:comp/BeanManager");
                Bean<?> bean = beanManager.getBeans(beanType).iterator().next();
                CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
                return (T) beanManager.getReference(bean, beanType, ctx);
            } finally {
                initialContext.close();
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * createdJiraTicket exposes a method by which a the createJiraTicket method on the Integration layerJira service
     * can be called
     *
     * @param projectPrefix String representing the Jira Project a ticket is to be created.  This is the prefix of all
     *                      tickets created in this project
     * @param issuetype the Issue Type of the Project the user wishes to create
     * @param summary a Brief summary that will go along with the created Issue
     * @param description a Brief description of what the new issue will represent
     * @param customFields a Collection of additional custom fields to be set during the Issue Creation Process
     * @return an response object that will contain relevant creation information such as the new unique Jira Key of the
     * created ticket
     * @throws IOException
     */
    public static CreateIssueResponse createJiraTicket (String projectPrefix,
                                                        CreateFields.Issuetype issuetype, String summary,
                                                        String description, Collection<CustomField> customFields)
            throws IOException {

        CreateIssueResponse createdJiraTicket = null;

        try {
            InitialContext initialContext = new InitialContext();
            try{
                BeanManager beanManager = (BeanManager) initialContext.lookup("java:comp/BeanManager");
                Bean bean = beanManager.getBeans(JiraService.class).iterator().next();
                CreationalContext ctx = beanManager.createCreationalContext(bean);
                JiraService jiraService =
                        (JiraService) beanManager.getReference(bean, bean.getClass(), ctx);
                createdJiraTicket = jiraService.createIssue(projectPrefix, issuetype, summary, description, customFields);
            } finally {
                initialContext.close();
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }

        return createdJiraTicket;
    }

    public static void updateJiraTicket(final String key, final Collection<CustomField> customFields) throws IOException {
//        getJiraService().updateIssue(key, customFields);
        (new Caller<Void, JiraService>() {
            @Override
            Void call(JiraService apiInstance) {
                try {
                    apiInstance.updateIssue(key, customFields);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        }).apiCall(JiraService.class);
    }

    public static String createTicketUrl(final String key) {
        return (new Caller<String, JiraService>() {
            @Override
            String call(JiraService apiInstance) {
                return apiInstance.createTicketUrl(key);
            }
        }).apiCall(JiraService.class);
    }

    /**
     * getJiraCustomFields exposes a method by which a user can retrieve all custom fields defined within the Jira
     * System.  This is useful when posting a create request or an update request for a jira ticket
     *
     * @return a Map of all currently defined Custom fields in a {@link CustomFieldDefinition} object indexed by the
     * field name for easier access
     * @throws IOException
     */
    public static Map<String, CustomFieldDefinition> getJiraCustomFields ( )
            throws IOException{

        Map<String, CustomFieldDefinition> customFields = null;

        try {
            InitialContext initialContext = new InitialContext();
            try{
                BeanManager beanManager = (BeanManager) initialContext.lookup("java:comp/BeanManager");
                Bean bean = beanManager.getBeans(JiraService.class).iterator().next();
                CreationalContext ctx = beanManager.createCreationalContext(bean);
                JiraService jiraService =
                        (JiraService) beanManager.getReference(bean, bean.getClass(), ctx);
                customFields = jiraService.getCustomFields( );
            } finally {
                initialContext.close();
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }

        return customFields;

    }

    /**
     * addJiraComment exposes a method by which a user can attach a new comment to a jira ticket
     *
     * @param issueKey unique key for the Jira ticket to which the comment is to be attached
     * @param comment comment to attach to the jira ticket.
     * @throws IOException
     */
    public static void addJiraComment (String issueKey, String comment) throws IOException{
        try {
            InitialContext initialContext = new InitialContext();
            try{
                BeanManager beanManager = (BeanManager) initialContext.lookup("java:comp/BeanManager");
                Bean bean = beanManager.getBeans(JiraService.class).iterator().next();
                CreationalContext ctx = beanManager.createCreationalContext(bean);
                JiraService jiraService =
                        (JiraService) beanManager.getReference(bean, bean.getClass(), ctx);
                jiraService.addComment(issueKey, comment);
            } finally {
                initialContext.close();
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * addJiraWatcher exposes a method by which a user can add another Jira user as a watcher of a Jira Ticket
     *
     * @param issueKey unique key for the Jira Ticket to which the comment is to be attached
     * @param newWatcherId Broad ID of the Jira user who will be added as a watcher to the ticket represented by
     *                     issueKey
     * @throws IOException
     */
    public static void addJiraWatcher (String issueKey, String newWatcherId) throws IOException{
        try {
            InitialContext initialContext = new InitialContext();
            try{
                BeanManager beanManager = (BeanManager) initialContext.lookup("java:comp/BeanManager");
                Bean bean = beanManager.getBeans(JiraService.class).iterator().next();
                CreationalContext ctx = beanManager.createCreationalContext(bean);
                JiraService jiraService =
                        (JiraService) beanManager.getReference(bean, bean.getClass(), ctx);
                jiraService.addWatcher ( issueKey, newWatcherId );
            } finally {
                initialContext.close();
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * addJiraPublicLink exposes a method by which a user can add a link between two Jira Issues
     *
     * @param linkType the type of link to create
     * @param sourceIssueKey unique key for the Jira Ticket which will act as the issue initiating the link
     * @param targetIssueKey unique key for the Jira Ticket which will act as the target of the link
     * @throws IOException
     */
    public static void addJiraPublicLink (AddIssueLinkRequest.LinkType linkType,String sourceIssueKey, String targetIssueKey) throws IOException{
        try {
            InitialContext initialContext = new InitialContext();
            try{
                BeanManager beanManager = (BeanManager) initialContext.lookup("java:comp/BeanManager");
                Bean bean = beanManager.getBeans(JiraService.class).iterator().next();
                CreationalContext ctx = beanManager.createCreationalContext(bean);
                JiraService jiraService =
                        (JiraService) beanManager.getReference(bean, bean.getClass(), ctx);
                jiraService.addLink (linkType, sourceIssueKey, targetIssueKey);
            } finally {
                initialContext.close();
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * getTransitions exposes a method by which a user can obtain an inventory of all available workflow transitions
     * for a given Jira ticket in its current state
     *
     * @param jiraTicketKey unique key for the Jira Ticket which the user wishes to gain information on available
     *                      transition states
     * @return a response object detailing all currently available workflow transition states.
     * @throws IOException
     */
    public static IssueTransitionResponse getTransitions(String jiraTicketKey) throws IOException{

        IssueTransitionResponse response = null;

        try {
            InitialContext initialContext = new InitialContext();
            try{
                BeanManager beanManager = (BeanManager) initialContext.lookup("java:comp/BeanManager");
                Bean bean = beanManager.getBeans(JiraService.class).iterator().next();
                CreationalContext ctx = beanManager.createCreationalContext(bean);
                JiraService jiraService =
                        (JiraService) beanManager.getReference(bean, bean.getClass(), ctx);
                response = jiraService.findAvailableTransitions(jiraTicketKey);
            } finally {
                initialContext.close();
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }

        return response;
    }

    /**
     * postTransition exposes a method by which a user can transition a given Jira Ticket to a new Transition state
     *
     *
     * @param jiraTicketKey unique key for a Jira Ticket whos state is to be transitions
     * @param transitionId id representing the next transition state that the user wished to take the represented
     *                     Jira Ticket to.
     * @throws IOException
     */
    public static void postTransition(String jiraTicketKey, String transitionId) throws IOException {


        try {
            InitialContext initialContext = new InitialContext();
            try{
                BeanManager beanManager = (BeanManager) initialContext.lookup("java:comp/BeanManager");
                Bean bean = beanManager.getBeans(JiraService.class).iterator().next();
                CreationalContext ctx = beanManager.createCreationalContext(bean);
                JiraService jiraService =
                        (JiraService) beanManager.getReference(bean, bean.getClass(), ctx);
               jiraService.postNewTransition(jiraTicketKey, transitionId);
            } finally {
                initialContext.close();
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }

    }

}
