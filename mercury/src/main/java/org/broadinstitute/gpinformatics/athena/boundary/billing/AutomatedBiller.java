package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.work.WorkCompleteMessageDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.work.WorkCompleteMessage;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.text.MessageFormat;

/**
 * This is a scheduled class that can generate ledger entries for product orders based on messages in the message
 * queue.  Messages are collected and stored as WorkCompleteMessage objects in the database.  Each message is used
 * to generate billing data for a sample in a product order, if possible.
 */
@Singleton
@Startup
public class AutomatedBiller {

    private final WorkCompleteMessageDao workCompleteMessageDao;
    private final ProductOrderDao productOrderDao;
    private final ProductOrderEjb productOrderEjb;

    private final Log log = LogFactory.getLog(AutomatedBiller.class);

    @Inject
    AutomatedBiller(WorkCompleteMessageDao workCompleteMessageDao,
                    ProductOrderDao productOrderDao,
                    ProductOrderEjb productOrderEjb) {
        this.workCompleteMessageDao = workCompleteMessageDao;
        this.productOrderDao = productOrderDao;
        this.productOrderEjb = productOrderEjb;
    }

    // EJBs require a no arg constructor.
    @SuppressWarnings("unused")
    public AutomatedBiller() {
        this(null, null, null);
    }

    // The schedule "minute = */15, hour = *" means every 15 minutes on the hour.
    @Schedule(minute = "*/15", hour = "*")
    public void processMessages() {
        for (WorkCompleteMessage message : workCompleteMessageDao.getNewMessages()) {
            try {
                // For each message, find its product order and request auto billing of the provided sample.
                ProductOrder order = productOrderDao.findByBusinessKey(message.getPdoName());
                if (order != null) {
                    productOrderEjb.autoBillSample(order, message.getAliquotId(),  message.getCompletedDate(),
                            message.getData());
                } else {
                    log.error(MessageFormat.format("Invalid PDO key ''{0}'', no billing will occur.",
                            message.getPdoName()));
                }
                workCompleteMessageDao.markMessageProcessed(message);
            } catch (Exception e) {
                log.error(MessageFormat.format("Error while processing work complete message. PDO: {0}, Sample: {1}",
                        message.getPdoName(), message.getAliquotId()), e);
            }
            // Once a message is processed, mark it to avoid processing it again.
            workCompleteMessageDao.markMessageProcessed(message);
        }
    }
}
