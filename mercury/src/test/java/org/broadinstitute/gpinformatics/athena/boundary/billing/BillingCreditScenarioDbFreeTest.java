/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2020 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@Test(groups = DATABASE_FREE)
public class BillingCreditScenarioDbFreeTest {
    public void testMultipleSequentialScenarios() throws Exception {
        ProductOrder productOrder = ProductOrderTestFactory.buildWholeGenomeProductOrder(2);
        String sapQuoteId = String.valueOf(System.currentTimeMillis());
        productOrder.setQuoteId(sapQuoteId);

        QuoteImportItem quiteImportItem = createLedgers(productOrder, 2, BigDecimal.ONE, BillingSession.SUCCESS);
        Collection<BillingCredit> billingCredits = null;
        try {
            billingCredits = BillingCredit.setupSapCredits(quiteImportItem);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), is(BillingAdaptor.CREDIT_QUANTITY_INVALID));
        }

        quiteImportItem = createLedgers(productOrder, 1, BigDecimal.ONE.negate(), StringUtils.EMPTY);
        billingCredits = BillingCredit.setupSapCredits(quiteImportItem);
        assertThat(billingCredits, hasSize(1));
        BillingCredit singleCredit = billingCredits.iterator().next();
        assertThat(singleCredit.getReturnLines(), hasSize(1));
        BillingCredit.LineItem singleLineItem = singleCredit.getReturnLines().iterator().next();
        assertThat(singleLineItem.getQuantity(), equalTo(BigDecimal.ONE));
        assertThat(quiteImportItem.totalPriorBillingQuantity(), equalTo(BigDecimal.ONE));


    }

    private QuoteImportItem createLedgers(ProductOrder productOrder, int numberOfLedgers, BigDecimal ledgerQuantity, String billingMessage) {
        List<LedgerEntry> ledgerEntries = new ArrayList<>();
        Date workCompleteDate=new Date();

        productOrder.getSamples().forEach(productOrderSample -> {
            if (ledgerEntries.size() < numberOfLedgers) {
                LedgerEntry ledgerEntry =
                    new LedgerEntry(productOrderSample, productOrder.getProduct(), workCompleteDate, ledgerQuantity);
                ledgerEntry.setBillingMessage(billingMessage);
                ledgerEntry.setQuoteId(productOrderSample.getProductOrder().getQuoteId());

                productOrderSample.getLedgerItems().add(ledgerEntry);
                ledgerEntries.add(ledgerEntry);
            }
        });
        new BillingSession(System.currentTimeMillis(), new HashSet<>(ledgerEntries));
        QuoteImportItem quoteImportItem =
            new QuoteImportItem(productOrder.getQuoteId(), null, null, ledgerEntries, workCompleteDate,
                productOrder.getProduct(), productOrder);
        return quoteImportItem;
    }
}
