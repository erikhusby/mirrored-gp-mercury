package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    // These constants define the automated billing period, which is from midnight through 5AM. This needs to be in sync
    // with the schedule, which is happening from midnight to 4:45.
    public static final int PROCESSING_START_HOUR = 0;
    public static final int PROCESSING_END_HOUR = 5;

    private final WorkCompleteMessageDao workCompleteMessageDao;
    private final BillingEjb billingEjb;
    private final SessionContextUtility sessionContextUtility;

    private final Log log = LogFactory.getLog(AutomatedBiller.class);

    @Inject
    AutomatedBiller(WorkCompleteMessageDao workCompleteMessageDao,
                    BillingEjb billingEjb,
                    SessionContextUtility sessionContextUtility) {
        this.workCompleteMessageDao = workCompleteMessageDao;
        this.billingEjb = billingEjb;
        this.sessionContextUtility = sessionContextUtility;
    }

    // EJBs require a no arg constructor.
    @SuppressWarnings("unused")
    public AutomatedBiller() {
        this(null, null, null);
    }

    /**
     * The schedule is every 30 minutes from 2 minutes after Midnight through 4:30 (before 5AM). It is offset
     * by 2 minutes because the Quote server reboots at midnight (prod) and 3AM (dev).
     */
    @Schedule(minute = "2/30", hour = "0,1,2,3,4", persistent = false)
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
                        processed = billingEjb.autoBillSample(message.getPdoName(), message.getAliquotId(),
                                message.getCompletedDate(), message.getData(), orderLockoutCache);
                    } catch (Exception e) {
                        log.error(MessageFormat.format(
                                "Error while processing work complete message. PDO: {0}, Sample: {1} {2}",
                                message.getPdoName(), message.getAliquotId() + e.getLocalizedMessage()
                                ));
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
