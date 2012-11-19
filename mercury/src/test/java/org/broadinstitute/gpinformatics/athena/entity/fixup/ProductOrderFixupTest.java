package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.PROD;

/**
 * This "test" is an example of how to fixup some data.  Each fix method includes the JIRA ticket ID.
 * Set @Test(enabled=false) after running once.
 */
public class ProductOrderFixupTest extends Arquillian {

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    JiraService jiraService;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(PROD, "prod");
    }

    /**
     * Fixed up data per JIRA ticket.
     */
    @Test(enabled = false)
    public void fixupGplim123() {
        ProductOrder productOrder = productOrderDao.findById(1001L);
        productOrder.setComments(productOrder.getComments() + " fixup");
        // The entity is already persistent, this call to persist is solely to begin and end a transaction, so the
        // change gets flushed.  This is an artifact of the test environment.
        productOrderDao.persist(productOrder);
    }

    /**
     * Change quote for a PDO, see http://prodinfojira.broadinstitute.org:8080/jira/browse/GPLIM-365
     * @throws Exception
     */
    @Test(enabled = false)
    public void change_quote_for_pdo() throws Exception {
        String jiraKey = "PDO-58";
        String newQuote = "STC8F2";
        ProductOrder productOrder = productOrderDao.findByBusinessKey(jiraKey);

        Map<String,CustomFieldDefinition> jiraFields = ServiceAccessUtility.getJiraCustomFields();
        Set<CustomField> customFields = new HashSet<CustomField>();

        for (Map.Entry<String, CustomFieldDefinition> stringCustomFieldDefinitionEntry : jiraFields.entrySet()) {
            System.out.println(stringCustomFieldDefinitionEntry.getKey());
            if (stringCustomFieldDefinitionEntry.getKey().equals("Quote ID")) {
                CustomField quoteCustomField = new CustomField(stringCustomFieldDefinitionEntry.getValue(),newQuote, CustomField.SingleFieldType.TEXT);
                customFields.add(quoteCustomField);
            }
        }

        jiraService.updateIssue(jiraKey,customFields);
        productOrder.setQuoteId(newQuote);
        // The entity is already persistent, this call to persist is solely to begin and end a transaction, so the
        // change gets flushed.  This is an artifact of the test environment.
        productOrderDao.persist(productOrder);
    }
}
