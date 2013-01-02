package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.apache.commons.logging.Log;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;

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

    @Inject
    Log log;

    @Inject
    private BSPUserList bspUserList;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(PROD, "prod");
    }

    /**
     * Fixed up data per JIRA ticketN.
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
    public void change_quote_for_pdo_58() throws Exception {
        String jiraKey = "PDO-58";
        String newQuote = "STC8F2";
        changeProductOrderQuoteId(jiraKey, newQuote);
    }

    /**
     * Change quote for a PDO, see http://prodinfojira.broadinstitute.org:8080/jira/browse/GPLIM-483
     * @throws Exception
     */
    @Test(enabled = false)
    public void change_quote_for_pdo_32() throws Exception {
        String jiraKey = "PDO-32";
        String newQuote = "GP85N";
        changeProductOrderQuoteId(jiraKey, newQuote);
    }

    /**
     * Change quote for a PDO, see http://prodinfojira.broadinstitute.org:8080/jira/browse/GPLIM-522
     * @throws Exception
     */
    @Test(enabled = false)
    public void change_quote_for_pdo_55() throws Exception {
        String jiraKey = "PDO-55";
        String newQuote = "MMM8GQ";
        changeProductOrderQuoteId(jiraKey, newQuote);
    }

    private void changeProductOrderQuoteId(String jiraKey, String newQuoteStr) throws IOException {
        ProductOrder productOrder = productOrderDao.findByBusinessKey(jiraKey);

        // comment out until we can update a Quote on a Jira ticket, need to extend the JiraService for transitions.
        if ( false ) {
            Map<String, CustomFieldDefinition> jiraFields = jiraService.getCustomFields();
            Set<CustomField> customFields = new HashSet<CustomField>();

            for (Map.Entry<String, CustomFieldDefinition> stringCustomFieldDefinitionEntry : jiraFields.entrySet()) {
                log.info(stringCustomFieldDefinitionEntry.getKey());
                if (stringCustomFieldDefinitionEntry.getKey().equals("Quote ID")) {
                    CustomField quoteCustomField = new CustomField(stringCustomFieldDefinitionEntry.getValue(),newQuoteStr, CustomField.SingleFieldType.TEXT);
                    customFields.add(quoteCustomField);
                }
            }

            jiraService.updateIssue(jiraKey,customFields);
        }
        log.info("Attempting to change Quote ID on product order " + productOrder.getJiraTicketKey() + " from " + productOrder.getQuoteId() + " to " +
                newQuoteStr );
        productOrder.setQuoteId(newQuoteStr);
        // The entity is already persistent, this call to persist is solely to begin and end a transaction, so the
        // change gets flushed.  This is an artifact of the test environment.
        productOrderDao.persist(productOrder);
        log.info("Changed Quote ID on product order " + productOrder.getJiraTicketKey() + " from " + productOrder.getQuoteId() + " to " +
                newQuoteStr );
    }


    /**
     * Clear the External Plating Addon from PDO-10
     * @throws Exception
     */
    @Test(enabled = false)
    public void clear_addons_for_pdo() throws Exception {
        String jiraKey = "PDO-10";

        ProductOrder productOrder = productOrderDao.findByBusinessKey(jiraKey);

        productOrder.setComments("");
        productOrder.updateAddOnProducts(new ArrayList<Product>());

        // The entity is already persistent, this call to persist is solely to begin and end a transaction, so the
        // change gets flushed.  This is an artifact of the test environment.
        productOrderDao.persist(productOrder);
    }


    @Test(enabled = false)
    public void reassignPDOsToElizabethNickerson() {
        String[] jiraKeys = new String[]{
                "PDO-132",
                "PDO-131",
                "PDO-130",
                "PDO-112",
                "PDO-108",
                "PDO-107",
                "PDO-13",
                "PDO-12",
                "PDO-9",
        };

        // at the time of this writing this resolves to the one user we want, but add some checks to make sure that
        // remains the case
        List<BspUser> bspUsers = bspUserList.find("Nickerson");
        if (bspUsers == null || bspUsers.isEmpty()) {
            throw new RuntimeException("No Nickersons found!");
        }

        if (bspUsers.size() > 1) {
            throw new RuntimeException("Too many Nickersons found!");
        }

        BspUser bspUser = bspUsers.get(0);
        if ( ! "Elizabeth".equals(bspUser.getFirstName()) || ! "Nickerson".equals(bspUser.getLastName())) {
            throw new RuntimeException("Wrong person found: " + bspUser);
        }

        for (String jiraKey : jiraKeys) {
            ProductOrder productOrder = productOrderDao.findByBusinessKey(jiraKey);
            productOrder.setCreatedBy(bspUser.getUserId());

            productOrderDao.persist(productOrder);
        }

    }

}
