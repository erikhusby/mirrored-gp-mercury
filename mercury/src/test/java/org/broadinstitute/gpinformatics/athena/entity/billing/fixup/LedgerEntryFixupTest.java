package org.broadinstitute.gpinformatics.athena.entity.billing.fixup;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingEjb;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportInfo;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryFixupDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPInterfaceException;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

@Test(groups = TestGroups.FIXUP)
public class LedgerEntryFixupTest extends Arquillian {

    private static final Log logger = LogFactory.getLog(LedgerEntryFixupTest.class);

    @Inject
    private UserBean userBean;

    @Inject
    private UserTransaction utx;

    @Inject
    private LedgerEntryFixupDao ledgerEntryFixupDao;

    @Inject
    private PriceListCache priceListCache;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    @Inject
    private ProductOrderEjb productOrderEjb;

    @Inject
    private QuoteServiceImpl quoteService;

    @Inject
    private BillingEjb billingEjb;

    // Use (RC, "rc"), (PROD, "prod") to push the backfill to RC and production respectively.
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }


    /**
     * Per the query below, no billing ledger records were found corresponding to billing sessions with a billed date
     * older than the date a quote was set into a PDO, including PDO quote updates.  Since PDO quote editing is
     * presently locked down, we know that all ledger entries should be backfilled with the quote currently set into the
     * PDO for the ledger entries' PDO samples.
     *
     *
     *  SELECT
     *    bs.BILLED_DATE,
     *    pdo.rev_date,
     *    pdo.JIRA_TICKET_KEY,
     *    pdo.QUOTE_ID
     *  FROM athena.BILLING_SESSION bs, athena.billing_ledger bl, athena.product_order_sample pdo_s,
     *  -- Outer select that enables us to pick out only the rows from the inner query with rank = 1.  This yields the
     *  -- oldest row for each jira_ticket_key + quote_id combination.
     *    (SELECT
     *       *
     *     FROM (
     *       -- Select the fields corresponding to the audit record for a PDO, including a dense_rank() of this record
     *       -- among all records with this combination of jira_ticket_key + quote_id.  The ranking is ordered by
     *       -- rev_date ASC, which means the oldest record for this jira_ticket_key + quote_id combination would
     *       -- have rank 1.
     *       SELECT
     *         product_order_id,
     *         jira_ticket_key,
     *         quote_id,
     *         rev_date,
     *         dense_rank()
     *         OVER (PARTITION BY jira_ticket_key, quote_id
     *           ORDER BY rev_date ASC) rnk
     *       FROM
     *       -- Use the rev date from the Envers rev_info table for this audit record, the PDO modified_dates were not
     *       -- being properly set for a period in Mercury's history.
     *         (SELECT
     *            product_order_id,
     *            jira_ticket_key,
     *            quote_id,
     *            ri.REV_DATE
     *          FROM athena.product_order_aud pdo2, mercury.rev_info ri
     *          WHERE pdo2.rev = ri.rev_info_id
     *                -- Filter drafts PDOs and messaging tests.
     *                AND pdo2.jira_ticket_key IS NOT null AND pdo2.jira_ticket_key NOT LIKE '%MsgTest%')
     *     )
     *     WHERE rnk = 1
     *     ORDER BY jira_ticket_key, quote_id) pdo
     *
     *  -- Look for billing ledger entries belonging to a billing session with a billed date less than any quote
     *  -- assignment date for the billing ledger's PDO sample's PDO.
     *  WHERE pdo_s.product_order = pdo.product_order_id AND
     *        bl.PRODUCT_ORDER_SAMPLE_ID = pdo_s.PRODUCT_ORDER_SAMPLE_ID AND
     *        bl.BILLING_SESSION = bs.BILLING_SESSION_ID AND
     *        bs.BILLED_DATE < pdo.rev_date
     *
     */
    @Test(enabled = false)
    public void backfillLedgerQuotes() {
        int counter = 0;
        for (LedgerEntry ledger : ledgerEntryFixupDao.findSuccessfullyBilledLedgerEntriesWithoutQuoteId()) {
            String quoteId = ledger.getProductOrderSample().getProductOrder().getQuoteId();

            final int BATCH_SIZE = 1000;
            ledger.setQuoteId(quoteId);
            if (++counter % BATCH_SIZE == 0) {
                // Only create a transaction for every BATCH_SIZE ledger entries, otherwise this test runs
                // excruciatingly slowly.
                ledgerEntryFixupDao.persistAll(Collections.emptyList());
                logger.info(MessageFormat.format("Issued persist at record {0}", counter));
            }
        }
        // We need the transaction that #persistAll gives us to get the last set of modulus BATCH_SIZE ledger
        // entries.  It doesn't matter what we pass this method, it will create the transaction around the
        // extended persistence context that holds the entities we modified above.
        ledgerEntryFixupDao.persistAll(Collections.emptyList());
    }

    /**
     * Add all appropriate price item types to any ledger entry items that were billed to the quote server.
     */
    @Test(enabled = false)
    public void populatePriceItemTypes() {
        int counter = 1;
        final int BATCH_SIZE = 2000;
        for (LedgerEntry ledger : ledgerEntryFixupDao.findAllBilledEntries()) {
            if (ledger.getPriceItem().equals(
                    ledger.getProductOrderSample().getProductOrder().getProduct().getPrimaryPriceItem())) {
                // This is a primary price item.
                ledger.setPriceItemType(LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM);
            } else if (isEntryAReplacementItem(ledger)) {
                ledger.setPriceItemType(LedgerEntry.PriceItemType.REPLACEMENT_PRICE_ITEM);
            } else {
                ledger.setPriceItemType(LedgerEntry.PriceItemType.ADD_ON_PRICE_ITEM);
            }

            if (counter % BATCH_SIZE == 0) {
                // Only create a transaction for every BATCH_SIZE ledger entries.
                ledgerEntryFixupDao.persistAll(Collections.emptyList());
                logger.info(MessageFormat.format("Issued persist at record {0}", counter));
            }

            counter++;
        }

        // We need the transaction that #persistAll gives us to get the last set of modulus BATCH_SIZE ledger
        // entries.  It doesn't matter what we pass this method, it will create the transaction that persists
        // the entities modified above.
        ledgerEntryFixupDao.persistAll(Collections.emptyList());
    }

    private boolean isEntryAReplacementItem(LedgerEntry ledger) {

        Collection<QuotePriceItem> quotePriceItems =
            priceListCache.getReplacementPriceItems(ledger.getProductOrderSample().getProductOrder().getProduct());

        for (QuotePriceItem quotePriceItem : quotePriceItems) {
            if (quotePriceItem.getName().equals(ledger.getPriceItem().getName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Largely copy/pasted from #backfillLedgerQuotes above, used to null out ledger quote IDs when we
     * discover we didn't assign them properly before and need to revise the assignments.
     */
    @Test(enabled = false)
    public void nullOutAllQuotes() {

        int counter = 0;

        List<LedgerEntry> ledgerEntries =
                ledgerEntryFixupDao.findAll(LedgerEntry.class, new GenericDao.GenericDaoCallback<LedgerEntry>() {
                    @Override
                    public void callback(CriteriaQuery<LedgerEntry> criteriaQuery, Root<LedgerEntry> root) {
                        // This runs much more slowly without the fetch as these would otherwise be singleton selected.
                        root.fetch(LedgerEntry_.productOrderSample);
                    }
                });

        for (LedgerEntry ledger : ledgerEntries) {

            final int BATCH_SIZE = 2000;
            ledger.setQuoteId(null);

            if (++counter % BATCH_SIZE == 0) {
                // Only create a transaction for every BATCH_SIZE ledger entries, otherwise this test runs
                // excruciatingly slowly.
                ledgerEntryFixupDao.persistAll(Collections.emptyList());
                logger.info(MessageFormat.format("Issued persist at record {0}", counter));
            }
        }

        // We need the transaction that #persistAll gives us to get the last set of modulus BATCH_SIZE ledger
        // entries.  It doesn't matter what we pass this method, it will create the transaction that persists
        // the entities modified above.
        ledgerEntryFixupDao.persistAll(Collections.emptyList());
    }

    /**
     * At one point the name of the session column in ledger was changed and that implicitly dropped the
     * foreign key constraint on session. Somehow there was an orphaned session entry, 1591. This needs to be
     * removed from the ledger so that we can place the constraint back. This required going in and changing the actual
     * LedgerEntry object to make the session a LONG instead of a BillingSession. To get that to run I also needed to
     * comment out a lot of DAO calls on the ledger
     */
    @Test(enabled = false)
    public void removeOrphanedSession() {
        List<Long> ledgerEntryIds = ledgerEntryFixupDao.getEntriesWithOrphanedSession(1591);
        for (Long entryId : ledgerEntryIds) {
            LedgerEntry entry = ledgerEntryFixupDao.findById(LedgerEntry.class, entryId);
            entry.setBillingSession(null);
        }

        // Persist all the clears
        ledgerEntryFixupDao.persistAll(Collections.emptyList());
    }

    @Test(enabled = false)
    public void gplim4143FixEntryBilledAsAddOnInsteadOfPrimary()
            throws IOException, ProductOrderEjb.NoSuchPDOException, SystemException, NotSupportedException,
            HeuristicRollbackException, HeuristicMixedException, RollbackException, SAPInterfaceException {
        userBean.loginOSUser();

        /*
         * Use a user transaction for this test because it calls an EJB method. Without the user transaction, the EJB's
         * transaction would save the entity modifications while the FixupCommentary would be persisted in a separate
         * transaction with a different rev ID.
         */
        utx.begin();

        String pdoKey = "PDO-8360";
        String sample = "SM-4AZSZ";

        List<ProductOrderSample> samples =
                productOrderSampleDao.findByOrderKeyAndSampleNames(pdoKey, Collections.singleton(sample));
        assertThat(samples, hasSize(1));
        assertThat(samples.get(0).getLedgerItems(), hasSize(1));
        LedgerEntry ledgerEntry = samples.get(0).getLedgerItems().iterator().next();

        assertThat(ledgerEntry.getPriceItemType(), equalTo(LedgerEntry.PriceItemType.ADD_ON_PRICE_ITEM));
        ledgerEntry.setPriceItemType(LedgerEntry.PriceItemType.REPLACEMENT_PRICE_ITEM);

        MessageReporter.LogReporter reporter = new MessageReporter.LogReporter(logger);
        productOrderEjb.updateOrderStatus(pdoKey, reporter);

        ledgerEntryFixupDao.persist(new FixupCommentary(
            String.format("GPLIM-4143 Correcting price item type for %s on %s", sample, pdoKey)));

        utx.commit();
    }

    @Test(enabled = false)
    public void gplim4371BackfillLedgerEntryBilledSuccessfullyMessage() {
        userBean.loginOSUser();

        List<LedgerEntry> ledgerEntries = ledgerEntryFixupDao.findBilledWithoutBillingMessage();
        for (LedgerEntry ledgerEntry : ledgerEntries) {
            ProductOrderSample productOrderSample = ledgerEntry.getProductOrderSample();
            System.out.println(String.format("Updating billingMessage on billed LedgerEntry for sample %s in %s", productOrderSample.getSampleKey(), productOrderSample.getProductOrder().getBusinessKey()));
            ledgerEntry.setBillingMessage(BillingSession.SUCCESS);
        }

        ledgerEntryFixupDao.persist(new FixupCommentary(
                String.format("SUPPORT-2120 Backfilled \"%s\" billing message for %d billed ledger entries",
                        BillingSession.SUCCESS, ledgerEntries.size())));
    }

    @Test(enabled = false)
    public void support4164ReverseIncorrectQuantity() {
        userBean.loginOSUser();

        LedgerEntry entryToCorrect= ledgerEntryFixupDao.findSingle(LedgerEntry.class, LedgerEntry_.workItem, "282137");

        Quote quoteByAlphaId = null;
        try {
            quoteByAlphaId = quoteService.getQuoteByAlphaId(entryToCorrect.getQuoteId());
        } catch (QuoteServerException e) {
            Assert.fail();
        } catch (QuoteNotFoundException e) {
            Assert.fail();
        }

        final String correction = quoteService.registerNewSAPWork(quoteByAlphaId,
                QuotePriceItem.convertMercuryPriceItem(entryToCorrect.getPriceItem()), null,
                entryToCorrect.getWorkCompleteDate(), -entryToCorrect.getQuantity(),
                "https://gpinfojira.broadinstitute.org/jira/browse/SUPPORT-4164",
                "correction", "SUPPORT-4164", null);

        entryToCorrect.getProductOrderSample().getLedgerItems().remove(entryToCorrect);
//        ledgerEntryFixupDao.remove(entryToCorrect);

        System.out.println("Corrected value in Quote server for quote " + entryToCorrect.getQuoteId() +
                           " with work item id " + correction);

        ledgerEntryFixupDao.persist(new FixupCommentary("Support-4164 Removing Ledger entry that was created "
                                                        + "in the wrong way.  Quote server correction is found at work item " + correction));
    }

    @Test(enabled = false)
    public void support4714ReverseIncorrectQuantity() {
        userBean.loginOSUser();

        String workItem = "302042";
        LedgerEntry entryToCorrect= ledgerEntryFixupDao.findSingle(LedgerEntry.class, LedgerEntry_.workItem, workItem);
        String quote = "MMML4G";
        Assert.assertEquals(entryToCorrect.getQuoteId(), quote);

        Quote quoteByAlphaId = null;
        try {
            quoteByAlphaId = quoteService.getQuoteByAlphaId(entryToCorrect.getQuoteId());
        } catch (QuoteServerException | QuoteNotFoundException e) {
            Assert.fail();
        }
        String fixupMessage = String.format("SUPPORT-4714 Reverse samples billed in Quotes (workItem %s, Quote %s)", workItem, quote);

        final String correction = quoteService.registerNewSAPWork(quoteByAlphaId,
                QuotePriceItem.convertMercuryPriceItem(entryToCorrect.getPriceItem()), null,
                entryToCorrect.getWorkCompleteDate(), -4,
                "https://gpinfojira.broadinstitute.org/jira/browse/SUPPORT-4714",
                "correction", "SUPPORT-4714", null);

        String messageWithCorrection = String.format("%s %s", fixupMessage, correction);
        System.out.println(messageWithCorrection);
        ledgerEntryFixupDao.persist(new FixupCommentary(messageWithCorrection));
    }

    @Test(enabled=false)
    public void support4208RePostQuoteServerPosts() {
        userBean.loginOSUser();
        QuoteImportInfo collectedEntriesByQuoteId = new QuoteImportInfo();
        final List<String> workItemsToUpdate = Arrays.asList("282484", "282485", "282488", "282509", "282489", "282494",
                "282495", "282496", "282501", "282502");

        Multimap<String, Long> entriesByWorkItem = ArrayListMultimap.create();
        ledgerEntryFixupDao.findListByList(LedgerEntry.class, LedgerEntry_.workItem, workItemsToUpdate)
                .forEach((entry)->{
            collectedEntriesByQuoteId.addQuantity(entry);
            entriesByWorkItem.put(entry.getWorkItem(), entry.getLedgerId());
        }
        );

        List<String> newWorkItems = new ArrayList<>();
        try {
            final List<QuoteImportItem> quoteImportItems = collectedEntriesByQuoteId.getQuoteImportItems(priceListCache);
            for (QuoteImportItem item : quoteImportItems) {
                Quote quote = item.getProductOrder().getQuote(quoteService);

                System.out.println("SUPPORT-4208 For work item " + item.getSingleWorkItem() + " updating "
                                   + item.getLedgerItems().size() + " ledger entries: "
                                   + StringUtils.join(entriesByWorkItem.get(item.getSingleWorkItem()), ","));
                final PriceList priceItemsForDate = quoteService.getPriceItemsForDate(Collections.singletonList(item));
                String newWorkId = quoteService.registerNewWork(quote,
                        QuotePriceItem.convertMercuryPriceItem(item.getPriceItem()),
                        item.getPrimaryForReplacement(priceItemsForDate),
                        item.getWorkCompleteDate(),
                        item.getQuantity(),
                        "https://gpinfojira.broadinstitute.org/jira/browse/SUPPORT-4208",
                        "SupportTicket","SUPPORT-4208",null);

                newWorkItems.add(newWorkId);

                item.updateLedgerEntries(null, BillingSession.SUCCESS, newWorkId, Collections.emptyList(),
                        item.getSapItems());
            }

        } catch (QuoteNotFoundException | QuoteServerException e) {
            Assert.fail();
        }
        ledgerEntryFixupDao.persist(new FixupCommentary("SUPPORT-4208 Replaced work item references " +
                                                        StringUtils.join(workItemsToUpdate,",")
                                                        + " Found in Ledger entries, with " + StringUtils.join(newWorkItems, ",")));
    }

    @Test(enabled = false)
    public void support5409ReverseIncorrectQuantity() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        String workItem = "328674";
        List <LedgerEntry> entryToCorrect= ledgerEntryFixupDao.findList(LedgerEntry.class, LedgerEntry_.workItem, workItem);
        String quote = "MMMOXY";
        entryToCorrect.forEach(ledgerEntry -> Assert.assertEquals(ledgerEntry.getQuoteId(), quote));

        final Set<PriceItem> collectedPriceItem =
                entryToCorrect.stream().map(LedgerEntry::getPriceItem).collect(Collectors.toSet());
        final Set<Date> collectedWorkCompleteDate =
                entryToCorrect.stream().map(LedgerEntry::getWorkCompleteDate).collect(Collectors.toSet());

        Quote quoteByAlphaId = null;
        try {
            quoteByAlphaId = quoteService.getQuoteByAlphaId(quote);
        } catch (QuoteServerException | QuoteNotFoundException e) {
            Assert.fail();
        }
        String fixupMessage = String.format("SUPPORT-5409 Reverse samples billed in Quotes (workItem %s, Quote %s)", workItem, quote);

        final String correction = quoteService.registerNewSAPWork(quoteByAlphaId,
                QuotePriceItem.convertMercuryPriceItem(collectedPriceItem.iterator().next()), null,
                collectedWorkCompleteDate.iterator().next(), -1.75,
                "https://gpinfojira.broadinstitute.org/jira/browse/SUPPORT-5409",
                "correction", "SUPPORT-5409", null);

        final Map<ProductOrderSample, List<LedgerEntry>> ledgersByProductOrderSample =
                entryToCorrect.stream().collect(Collectors.groupingBy(LedgerEntry::getProductOrderSample));

        ledgersByProductOrderSample.forEach((productOrderSample, ledgerEntries) -> productOrderSample.getLedgerItems().removeAll(ledgerEntries));

        String messageWithCorrection = String.format("%s %s", fixupMessage, correction);
        System.out.println(messageWithCorrection);
        ledgerEntryFixupDao.persist(new FixupCommentary(messageWithCorrection));
        utx.commit();
    }

}
