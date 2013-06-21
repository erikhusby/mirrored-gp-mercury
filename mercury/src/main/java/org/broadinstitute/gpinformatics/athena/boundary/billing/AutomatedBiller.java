package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.work.WorkCompleteMessageDao;
import org.broadinstitute.gpinformatics.athena.entity.work.WorkCompleteMessage;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtility;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a scheduled class that can generate ledger entries for product orders based on messages in the message
 * queue.  Messages are collected and stored as WorkCompleteMessage objects in the database.  Each message is used
 * to generate billing data for a sample in a product order, if possible.
 */
@Singleton
@Startup
public class AutomatedBiller {

    private final WorkCompleteMessageDao workCompleteMessageDao;
    private final ProductOrderEjb productOrderEjb;
    private final SessionContextUtility sessionContextUtility;

    private final Log log = LogFactory.getLog(AutomatedBiller.class);

    @Inject
    AutomatedBiller(WorkCompleteMessageDao workCompleteMessageDao,
                    ProductOrderEjb productOrderEjb,
                    SessionContextUtility sessionContextUtility) {
        this.workCompleteMessageDao = workCompleteMessageDao;
        this.productOrderEjb = productOrderEjb;
        this.sessionContextUtility = sessionContextUtility;
    }

    // EJBs require a no arg constructor.
    @SuppressWarnings("unused")
    public AutomatedBiller() {
        this(null, null, null);
    }

    /**
     * The schedule is every 30 minutes from Midnight through 4:30 (before 5AM). This annotation MUST BE IN SYNC WITH THE
     * TIME RANGE that is in ProductOrderEjb. I chose that ejb because it is used by this class and I don't
     * want to have to construct on automated biller just to use the method AND because making a static
     * method is a no-no for EJBs.
     */
    @Schedule(minute = "*/30", hour = "0,1,2,3,4", persistent = false)
    public void processMessages() {
        // Use SessionContextUtility here because ProductOrderEjb depends on session scoped beans.
        sessionContextUtility.executeInContext(new SessionContextUtility.Function() {
            @Override
            public void apply() {

                // Since we may check on product orders one at a time per sample, this will keep us from
                // doing two queries every time within the following loop.
                Map<String, Boolean> orderLockoutCache = new HashMap<> ();

                for (WorkCompleteMessage message : workCompleteMessageDao.getNewMessages()) {
                    // Default to true. Even if an exception is thrown, the message is considered to be processed
                    // to avoid re-throwing every time.
                    boolean processed = true;
                    try {
                        // For each message, request auto billing of the sample in the order.
                        processed = productOrderEjb.autoBillSample(message.getPdoName(), message.getAliquotId(),
                                message.getCompletedDate(), message.getData(), orderLockoutCache);
                    } catch (Exception e) {
                        log.error(MessageFormat.format(
                                "Error while processing work complete message. PDO: {0}, Sample: {1}",
                                message.getPdoName(), message.getAliquotId()), e);
                    }
                    if (processed) {
                        // Once a message is processed, mark it to avoid processing it again.
                        workCompleteMessageDao.markMessageProcessed(message);
                    }
                }
            }
        });
    }
}
