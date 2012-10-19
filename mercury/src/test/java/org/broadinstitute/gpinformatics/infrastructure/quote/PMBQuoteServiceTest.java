package org.broadinstitute.gpinformatics.infrastructure.quote;

/**
 * Based on test from Mercury
 */

import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;


@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class PMBQuoteServiceTest extends Arquillian {

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Inject
    private PMBQuoteService pmbQuoteService;


    @Inject
    private Log log;


    // test works but the method under test doesn't seem to be used so why slow down our builds
    @Test(enabled = false)
    public void test_get_a_quote() throws Exception {

        Quote quote = pmbQuoteService.getQuoteByAlphaId("DNA23H");

        Assert.assertNotNull(quote);
        Assert.assertEquals("NIAID (CO 5035331)", quote.getName());
        Assert.assertEquals("5035331", quote.getQuoteFunding().getFundingLevel().getFunding().getCostObject());
        Assert.assertEquals("GENSEQCTR_(NIH)NIAID",quote.getQuoteFunding().getFundingLevel().getFunding().getGrantDescription());
        Assert.assertEquals(Funding.FUNDS_RESERVATION,quote.getQuoteFunding().getFundingLevel().getFunding().getFundingType());
        Assert.assertEquals("DNA23H",quote.getAlphanumericId());

        quote = pmbQuoteService.getQuoteByAlphaId("DNA3A9");
        Assert.assertEquals("HARVARD UNIVERSITY",quote.getQuoteFunding().getFundingLevel().getFunding().getInstitute());
        Assert.assertEquals(Funding.PURCHASE_ORDER,quote.getQuoteFunding().getFundingLevel().getFunding().getFundingType());
        Assert.assertEquals("DNA3A9",quote.getAlphanumericId());

    }

    // test works but the method under test doesn't seem to be used so why slow down our builds
    @Test(enabled = false)
    public void test_get_all_quotes_for_sequencing() throws Exception {

        Quotes quotes = pmbQuoteService.getAllQuotes();

        Assert.assertNotNull(quotes);
        Assert.assertFalse(quotes.getQuotes().isEmpty());
        Set<String> grants = new HashSet<String>();
        Set<String> fundingTypes = new HashSet<String>();
        Set<String> pos = new HashSet<String>();
        for (Quote quote : quotes.getQuotes()) {
            if (quote.getQuoteFunding() != null) {
                if (quote.getQuoteFunding().getFundingLevel() != null) {
                    if (quote.getQuoteFunding().getFundingLevel().getFunding() != null) {
                        Funding funding = quote.getQuoteFunding().getFundingLevel().getFunding();
                        fundingTypes.add(funding.getFundingType());
                        //System.out.println(funding.getFundingType());
                        if (Funding.FUNDS_RESERVATION.equals(funding.getFundingType())) {
                            grants.add(funding.getGrantDescription());
                        }
                        else if (Funding.PURCHASE_ORDER.equals(funding.getFundingType())) {
                            pos.add(funding.getPurchaseOrderNumber());
                        }
                    }
                }

            }
        }
        Assert.assertEquals(fundingTypes.size(), 2);   // includes null fundingType
        Assert.assertTrue(fundingTypes.contains(Funding.FUNDS_RESERVATION));
        Assert.assertTrue(fundingTypes.contains(Funding.PURCHASE_ORDER));
    }


    // this method needs to work for retrieving price items but something is wrong:
    //
    // java.lang.AssertionError: javax.ws.rs.WebApplicationException: javax.xml.bind.UnmarshalException:
    // unexpected element (uri:"", local:"response"). Expected elements are <{}PriceItem>,<{}PriceList>
    //
    // at the command line the following produces no results:
    //
    // curl --user 'rnordin@broadinstitute.org:Squ1d_us3r' 'http://quoteqa.broadinstitute.org:8080/quotes/ws/portals/private/get_price_list'

    @Test
    public void testPriceItems() {

        try {
            for (QuotePlatformType quotePlatformType : QuotePlatformType.values()) {
                log.info("Beginning fetch for " + quotePlatformType);
                pmbQuoteService.getPlatformPriceItems(quotePlatformType);
                log.info("Ending fetch for " + quotePlatformType);
            }
        }
        catch (Exception e) {
            Assert.fail(e.toString());
        }

    }

}
