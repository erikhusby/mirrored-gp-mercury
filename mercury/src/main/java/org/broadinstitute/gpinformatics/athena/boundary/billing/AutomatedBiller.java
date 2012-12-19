package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingLedgerDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.work.WorkCompleteMessageDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.work.WorkCompleteMessage;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.List;

/**
 * This is a scheduled class that can generate billing ledgers for product orders based on messages in the message
 * queue.  Messages are collected and stored as WorkCompleteMessage objects in the database.  Each message is used
 * to generate billing data for a sample in a product order, if possible.
 */
@Singleton
@Startup
public class AutomatedBiller {

    private final WorkCompleteMessageDao workCompleteMessageDao;
    private final ProductOrderSampleDao productOrderSampleDao;
    private final ProductOrderDao productOrderDao;
    private final BillingLedgerDao billingLedgerDao;

    private final Log log = LogFactory.getLog(AutomatedBiller.class);

    @Inject
    AutomatedBiller(WorkCompleteMessageDao workCompleteMessageDao,
                    ProductOrderSampleDao productOrderSampleDao,
                    ProductOrderDao productOrderDao,
                    BillingLedgerDao billingLedgerDao) {
        this.workCompleteMessageDao = workCompleteMessageDao;
        this.productOrderSampleDao = productOrderSampleDao;
        this.productOrderDao = productOrderDao;
        this.billingLedgerDao = billingLedgerDao;
    }

    @Schedule(minute = "*/15", hour = "*")
    public void processMessages() {
        for (WorkCompleteMessage message : workCompleteMessageDao.getNewMessages()) {
            try {
                processMessage(message);
            } catch (Exception e) {
                // FIXME: need to mark messages as processed even when an error occurs; enable this once code is debugged.
//                workCompleteMessageDao.markMessageProcessed(message);
                log.error(MessageFormat.format("Error while processing work complete message. PDO: {0}, Sample: {1}, Index: {2}",
                        message.getPdoName(), message.getCollaboratorSampleId(), message.getSampleIndex()), e);
            }
        }
    }

    /**
     * Check and see if a given order is locked out, e.g. currently in a billing session.
     * @param order the order to check
     * @return true if the order is locked out.
     */
    private boolean isLockedOut(ProductOrder order) {
        return !billingLedgerDao.findLockedOutByOrderList(new ProductOrder[]{ order }).isEmpty();
    }

    /**
     * Process each message in the work complete message queue.  For each message, find its product order.  If the
     * order's product supports automated billing, and it's not currently locked out, generate a list of billing ledger
     * items for the sample and add them to the billing ledger.  Once a message is processed, mark it to avoid
     * processing it again.
     * @param message the message to process.
     */
    public void processMessage(WorkCompleteMessage message) {
        ProductOrder order = productOrderDao.findByBusinessKey(message.getPdoName());
        Product product = order.getProduct();

        if (isLockedOut(order)) {
            log.debug(MessageFormat.format("Can''t auto-bill order {0} because it''s currently locked out.",
                    order.getJiraTicketKey()));
            // Return early to avoid marking the message as processed.
            // TODO: This code should be wrapped in a beginLockout()/endLockout() block to avoid collisions with
            // a mercury user starting a billing session during this process.
            return;
        }

        if (product.isUseAutomatedBilling()) {
            if (product.getRequirement().canBill(message.getData())) {
                List<ProductOrderSample> samples =
                        productOrderSampleDao.findByOrderAndName(order, message.getCollaboratorSampleId());
                ProductOrderSample sample = samples.get(message.getSampleIndex().intValue());
                sample.autoBillSample(message.getCompletedDate(), 1);
            }
        } else {
            log.debug(MessageFormat.format("Product {0} doesn''t support automated billing.", product.getProductName()));
        }
        workCompleteMessageDao.markMessageProcessed(message);
    }
}
