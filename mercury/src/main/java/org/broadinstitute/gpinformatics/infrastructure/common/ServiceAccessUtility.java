package org.broadinstitute.gpinformatics.infrastructure.common;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * @author Scott Matthews
 *         Date: 10/11/12
 *         Time: 12:17 PM
 */
public class ServiceAccessUtility {

    public static BSPSampleDTO getSampleName(String sampleName) {

        BSPSampleDTO foundServiceObject = null;

        try {
            InitialContext initialContext = new InitialContext();
            try{
                BeanManager beanManager = (BeanManager) initialContext.lookup("java:comp/BeanManager");
                Bean bean = beanManager.getBeans(BSPSampleDataFetcher.class).iterator().next();
                CreationalContext ctx = beanManager.createCreationalContext(bean);
                BSPSampleDataFetcher bspSampleSearchService =
                        (BSPSampleDataFetcher) beanManager.getReference(bean, bean.getClass(), ctx);
                foundServiceObject = bspSampleSearchService.fetchSingleSampleFromBSP(sampleName);
            } finally {
                initialContext.close();
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }

        return foundServiceObject;
    }
    public static Map<String, BSPSampleDTO> getSampleNames(Collection<String> sampleNames) {

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

    public static CreateIssueResponse createJiraTicket (String projectPrefix,
                                                        CreateIssueRequest.Fields.Issuetype issuetype, String summary,
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

    public static Map<String, CustomFieldDefinition> getJiraCustomFields (CreateIssueRequest.Fields.Project jiraProject,
                                                                          CreateIssueRequest.Fields.Issuetype projectIssue)
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
                customFields = jiraService.getCustomFields(jiraProject, projectIssue);
            } finally {
                initialContext.close();
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }

        return customFields;

    }


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
}
