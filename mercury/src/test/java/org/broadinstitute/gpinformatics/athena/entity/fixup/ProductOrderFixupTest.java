package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.apache.commons.logging.Log;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.RiskItemDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.RiskItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Operator;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriteria;
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
import javax.persistence.Query;
import java.io.IOException;
import java.util.*;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * This "test" is an example of how to fixup some data.  Each fix method includes the JIRA ticket ID.
 * Set @Test(enabled=false) after running once.
 */
public class ProductOrderFixupTest extends Arquillian {

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private RiskItemDao riskItemDao;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    @Inject
    JiraService jiraService;

    @Inject
    Log log;

    @Inject
    private BSPUserList bspUserList;

    // When you run this on prod, change to PROD and prod
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
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
        if (false) {
            Map<String, CustomFieldDefinition> jiraFields = jiraService.getCustomFields();
            Set<CustomField> customFields = new HashSet<CustomField>();

            for (Map.Entry<String, CustomFieldDefinition> stringCustomFieldDefinitionEntry : jiraFields.entrySet()) {
                log.info(stringCustomFieldDefinitionEntry.getKey());
                if (stringCustomFieldDefinitionEntry.getKey().equals("Quote ID")) {
                    CustomField quoteCustomField = new CustomField(stringCustomFieldDefinitionEntry.getValue(), newQuoteStr);
                    customFields.add(quoteCustomField);
                }
            }

            jiraService.updateIssue(jiraKey,customFields);
        }
        log.info("Attempting to change Quote ID on product order " + productOrder.getJiraTicketKey() + " from " + productOrder.getQuoteId() + " to " +
                newQuoteStr);
        productOrder.setQuoteId(newQuoteStr);
        // The entity is already persistent, this call to persist is solely to begin and end a transaction, so the
        // change gets flushed.  This is an artifact of the test environment.
        productOrderDao.persist(productOrder);
        log.info("Changed Quote ID on product order " + productOrder.getJiraTicketKey() + " from " + productOrder.getQuoteId() + " to " +
                newQuoteStr);
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
        if (!bspUser.getFirstName().equals("Elizabeth") || !bspUser.getLastName().equals("Nickerson")) {
            throw new RuntimeException("Wrong person found: " + bspUser);
        }

        for (String jiraKey : jiraKeys) {
            ProductOrder productOrder = productOrderDao.findByBusinessKey(jiraKey);
            productOrder.prepareToSave(bspUser, false);

            productOrderDao.persist(productOrder);
        }

    }

    @Test(enabled = false)
    public void removeSamplesFromPDO300() {
        List<String> samplesToRemove = Arrays.asList(
                "SM-3SFP5",
                "SM-3SFPA",
                "SM-3SDFV",
                "SM-3SDIE",
                "SM-3SDI2",
                "SM-3SJCZ",
                "SM-3SJCV",
                "SM-3SDIT",
                "SM-3SDIA",
                "SM-3SJCR",
                "SM-3SFP2",
                "SM-3SDTO",
                "SM-3SDT5",
                "SM-3SJD1",
                "SM-3SDIO",
                "SM-3SFPD",
                "SM-3SJCQ",
                "SM-3SJCO",
                "SM-3SJCT",
                "SM-3SDTB",
                "SM-3SJD3",
                "SM-3SFP1",
                "SM-3SFP3",
                "SM-3SFOX");

        String pdo="PDO-300";

        ProductOrder productOrder = productOrderDao.findByBusinessKey(pdo);

        List<ProductOrderSample> sampleList = productOrder.getSamples();

        Iterator<ProductOrderSample> sampleIterator = sampleList.iterator();
        while (sampleIterator.hasNext()) {
            ProductOrderSample sample = sampleIterator.next();

            if (samplesToRemove.contains(sample.getSampleName())) {
                sampleIterator.remove();
            }
        }

        productOrderDao.persist(productOrder);
    }

    @Test(enabled = false)
    public void removeSamplesFromDevTest() {
        List<String> samplesToRemove = Arrays.asList(
                "SM-3DV29",
                "SM-3DV2A",
                "SM-3DV2B",
                "SM-3DV2C",
                "SM-3DV2D",
                "SM-3DV2E",
                "SM-3DV2F");

        String pdo="PDO-49";

        ProductOrder productOrder = productOrderDao.findByBusinessKey(pdo);

        List<ProductOrderSample> sampleList = productOrder.getSamples();

        Iterator<ProductOrderSample> sampleIterator = sampleList.iterator();
        while (sampleIterator.hasNext()) {
            ProductOrderSample sample = sampleIterator.next();

            if (samplesToRemove.contains(sample.getSampleName())) {
                sampleIterator.remove();
            }
        }

        productOrderDao.persist(productOrder);
    }

    @Test(enabled = false)
    public void removeSamplesForGPLIM877() {
        List<String> samplesToRemove = Arrays.asList(
                "SM-1WJOV",
                "SM-1WJOX",
                "SM-1WJPC",
                "SM-1WJPD");

        String pdo="PDO-388";

        ProductOrder productOrder = productOrderDao.findByBusinessKey(pdo);

        List<ProductOrderSample> sampleList = productOrder.getSamples();

        Iterator<ProductOrderSample> sampleIterator = sampleList.iterator();
        while (sampleIterator.hasNext()) {
            ProductOrderSample sample = sampleIterator.next();

            if (samplesToRemove.contains(sample.getSampleName())) {
                sampleIterator.remove();
            }
        }

        productOrderDao.persist(productOrder);
    }

    @Test(enabled = false)
    public void setupOnRiskTestData() {

        String pdo="PDO-49";
        ProductOrder productOrder = productOrderDao.findByBusinessKey(pdo);

        RiskCriteria riskCriteria = new RiskCriteria(RiskCriteria.RiskCriteriaType.CONCENTRATION, Operator.LESS_THAN, "250.0");
        productOrder.getProduct().getRiskCriteriaList().add(riskCriteria);
        productDao.persist(productOrder.getProduct());

        // Populate on risk for every other sample
        int count = 0;
        for (ProductOrderSample sample : productOrder.getSamples()) {
            if ((count++ % 2) == 0) {
                RiskItem riskItem = new RiskItem(riskCriteria, "240.0");
                riskItem.setRemark("Bad Concentration found");
                sample.setRiskItems(Collections.singletonList(riskItem));
                productOrderSampleDao.persist(sample);
            }
        }
    }



    @Test(enabled = false)
    public void setPlacedDate() {

        Query query = productOrderDao.getEntityManager().createNativeQuery(
                "SELECT pdo_aud.jira_ticket_key, rev_info.REV_DATE FROM\n" +
                "  athena.product_order_aud pdo_aud, athena.product_order pdo, mercury.rev_info rev_info WHERE \n" +
                "  pdo.product_order_id = pdo_aud.product_order_id AND\n" +
                "  pdo_aud.rev = (\n" +
                "    SELECT MIN(pdo_aud2.rev) FROM athena.product_order_aud pdo_aud2 WHERE\n" +
                "      pdo_aud2.product_order_id = pdo.product_order_id AND pdo_aud2.jira_ticket_key IS NOT NULL\n" +
                "  ) AND\n" +
                "rev_info.REV_INFO_ID = pdo_aud.rev\n" +
                "  \n" +
                "ORDER BY pdo.created_date "
        );

        List<Object[]> resultList = query.getResultList();

        Map<String, Date> keyToDateMap = new HashMap<String, Date>();

        for (Object[] row : resultList) {
            String key = (String) row[0];
            Date date = (Date) row[1];
            keyToDateMap.put(key, date);
        }

        List<ProductOrder> productOrders = productOrderDao.findAll();
        for (ProductOrder productOrder : productOrders) {
            if (keyToDateMap.containsKey(productOrder.getBusinessKey())) {
                productOrder.setPlacedDate(keyToDateMap.get(productOrder.getBusinessKey()));
                productOrderDao.persist(productOrder);
            }
        }
    }
}
