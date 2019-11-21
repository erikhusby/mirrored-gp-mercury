package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKitDetail;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPInterfaceException;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.TEST;

@Test(groups = TestGroups.ALTERNATIVES)
@Dependent
public class ProductOrderEjbJiraTransitionTest extends Arquillian {

    public ProductOrderEjbJiraTransitionTest(){}

    private String PDO;
    private static final String OLD_PDO = "PDO-4458";

    private static final ProductOrderEjb.JiraTransition TARGET_STATE = ProductOrderEjb.JiraTransition.OPEN;

    @Inject
    private JiraService jiraService;

    @Inject
    private ProductOrderEjb productOrderEjb;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    UserBean userBean;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(TEST);
    }

    @BeforeMethod
    public void setupJiraStatus() throws IOException, SAPInterfaceException {
        if (jiraService != null) {
            resetJiraTicketState();
        }
    }

    @Test
    public void testCancellation() throws Exception {
        productOrderEjb.transitionJiraTicket(PDO, ProductOrderEjb.JiraResolution.CANCELLED,ProductOrderEjb.JiraTransition.CANCEL, "testing");
        Assert.assertEquals(jiraService.getIssue(PDO).getResolution().toUpperCase(),ProductOrderEjb.JiraResolution.CANCELLED.toString().toUpperCase());
    }

    private void resetJiraTicketState() throws IOException, SAPInterfaceException {

        userBean.login("scottmat");

        ProductOrder newProductOrder = initializeProductOrder();

        PDO = newProductOrder.getBusinessKey();

        productOrderEjb.transitionJiraTicket(PDO, null,ProductOrderEjb.JiraTransition.DEVELOPER_EDIT, "testing");
        productOrderEjb.transitionJiraTicket(PDO, null,TARGET_STATE, "testing");
        Assert.assertEquals(getJiraTicketState(),TARGET_STATE.getStateName(),"Could not reset state of ticket " + PDO + " to " + TARGET_STATE + ".  Maybe the" +
                                                                             "workflow has changed?  Or the pdo ticket was updated out of band?");
    }

    private ProductOrder initializeProductOrder() throws IOException, SAPInterfaceException {
        ProductOrder oldProductOrder = productOrderDao.findByBusinessKey(OLD_PDO);

        List<ProductOrderSample> newOrderSamples = new ArrayList<>();

        for(ProductOrderSample oldSample:oldProductOrder.getSamples()) {
            ProductOrderSample sample = new ProductOrderSample(oldSample.getName());
            sample.setMetadataSource(oldSample.getMetadataSource());
            newOrderSamples.add(sample);
        }

        ProductOrder newProductOrder = new ProductOrder(userBean.getBspUser().getUserId(), oldProductOrder.getTitle() + (new Date()).getTime(),
                newOrderSamples,oldProductOrder.getQuoteId(), oldProductOrder.getProduct(),
                oldProductOrder.getResearchProject());

        newProductOrder.setSkipRegulatoryReason("JustBecause");


        newProductOrder.setAttestationConfirmed(true);
        try {
            productOrderEjb.persistProductOrder(ProductOrder.SaveType.CREATING,newProductOrder,
                    Collections.<String>emptyList(), Collections.<ProductOrderKitDetail>emptyList());
        } catch (QuoteNotFoundException|SAPInterfaceException e) {
            Assert.fail();
        }
        MessageCollection justToGetBy = new MessageCollection();
        productOrderEjb.placeProductOrder(newProductOrder.getProductOrderId(), newProductOrder.getBusinessKey(),
                justToGetBy);
        return newProductOrder;
    }

    private String getJiraTicketState() throws IOException{
        return (String)((Map)jiraService.getIssue(PDO).getField("Status")).get("name");
    }
}
