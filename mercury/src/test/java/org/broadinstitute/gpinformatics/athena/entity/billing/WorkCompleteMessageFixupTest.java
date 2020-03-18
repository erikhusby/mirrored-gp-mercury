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

package org.broadinstitute.gpinformatics.athena.entity.billing;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryFixupDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.work.WorkCompleteMessageDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.work.WorkCompleteMessage;
import org.broadinstitute.gpinformatics.athena.entity.work.WorkCompleteMessage_;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.FIXUP)
public class WorkCompleteMessageFixupTest extends Arquillian {
    @Inject
    private WorkCompleteMessageDao workCompleteMessageDao;
    @Inject
    private ProductOrderDao productOrderDao;
    @Inject
    private ProductOrderSampleDao productOrderSampleDao;
    @Inject
    private UserBean userBean;
    @Inject
    private UserTransaction utx;
    @Inject
    private LedgerEntryFixupDao ledgerEntryFixupDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = true)
    public void addWorkCompleteMessage() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        String pdoKey = "PDO-21657";
        ProductOrder productOrder = productOrderDao.findByBusinessKey(pdoKey);

        Map<String, Object> dataMap = new HashMap<>();

        List<WorkCompleteMessage> workCompleteMessages = new ArrayList<>();
        productOrder.getSamples().forEach(productOrderSample -> {
            Set<LedgerEntry> ledgerItems = productOrderSample.getLedgerItems();
            productOrderSample.getLedgerItems().removeAll(ledgerItems);
            String sampleKey = productOrderSample.getSampleKey();
            if (StringUtils.isBlank(productOrderSample.getAliquotId())) {
                productOrderSample.setAliquotId(sampleKey);
            }
            productOrderSampleDao.persist(productOrderSample);

            List<WorkCompleteMessage> messages =
                workCompleteMessageDao.findByPDOAndAliquot(pdoKey, sampleKey);
            boolean alreadyInQueue =
                messages.stream().map(WorkCompleteMessage::getProcessDate).anyMatch(Objects::isNull);
            if (!alreadyInQueue) {
                Long userId = userBean.getBspUser().getUserId();
                workCompleteMessages.add(new WorkCompleteMessage(pdoKey, sampleKey, "P-WG-0073", userId, new Date(), dataMap));
                workCompleteMessages
                    .add(new WorkCompleteMessage(pdoKey, sampleKey, "P-EX-0052", userId, new Date(), dataMap));
            }
        });

        workCompleteMessageDao.persistAll(workCompleteMessages);
        utx.commit();
    }

    public void testCleanupMessages() throws Exception {
        List<WorkCompleteMessage> allMessages = workCompleteMessageDao.findList(WorkCompleteMessage.class,
            WorkCompleteMessage_.processDate, null);
        allMessages.forEach(workCompleteMessage -> workCompleteMessage.setProcessDate(new Date()));
        workCompleteMessageDao.persistAll(allMessages);

    }

    public void testUnsetProcessDateOnProduct() throws Exception {
        List<WorkCompleteMessage> allMessages = workCompleteMessageDao.findList(WorkCompleteMessage.class,
            WorkCompleteMessage_.pdoName, "PDO-21212");
        allMessages.forEach(workCompleteMessage -> workCompleteMessage.setProcessDate(null));
        workCompleteMessageDao.persistAll(allMessages);

    }
}
