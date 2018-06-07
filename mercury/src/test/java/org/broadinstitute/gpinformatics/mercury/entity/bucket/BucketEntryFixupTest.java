package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.ReworkReasonDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Test(groups = TestGroups.FIXUP)
@Dependent
public class BucketEntryFixupTest extends Arquillian {

    @Inject
    BucketDao bucketDao;

    @Inject
    BucketEntryDao bucketEntryDao;

    @Inject
    ReworkReasonDao reworkReasonDao;

    @Inject
    LabVesselDao labVesselDao;

    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    UserTransaction utx;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private UserBean userBean;
    

    /**
     * Use test deployment here to talk to the actual jira
     *
     * @return
     */
    @Deployment
    public static WebArchive buildMercuryWar() {

        /*
         * If the need comes to utilize this fixup in production, change the buildMercuryWar parameters accordingly
         */
        return DeploymentBuilder.buildMercuryWar(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV, "dev");
    }

    @BeforeMethod(groups = TestGroups.FIXUP)
    public void setUp() throws Exception {
        if (utx == null) {
            System.out.println("User transaction not injected.  Leaving");
            return;
        } else {
            System.out.println("User transaction is injected");
        }
//        utx.begin();
    }

    @AfterMethod(groups = TestGroups.FIXUP)
    public void tearDown() throws Exception {
        // Skip if no injections, since we're not running in container.
        if (utx == null) {
            System.out.println("User transaction not injected.  Leaving");
            return;
        } else {
            System.out.println("User transaction is injected");
        }

//        utx.commit();
    }


    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void archiveReworkEntries() throws Exception {

        Bucket fixupBucket = bucketDao.findByName("Pico/Plating Bucket");


        for (BucketEntry reworkEntry : fixupBucket.getReworkEntries()) {
            reworkEntry.setStatus(BucketEntry.Status.Archived);
        }
    }

    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void remove0150385070FromShearingBucketForGPLIM1932() {
        Bucket bucket = bucketDao.findByName("Shearing Bucket");
        LabVessel vessel = labVesselDao.findByIdentifier("0150385070");

        BucketEntry bucketEntry = bucket.findEntry(vessel);
        bucket.removeEntry(bucketEntry);
    }

    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void remove0155694973FromPoolingBucketGPLIM2503() {
        Bucket bucket = bucketDao.findByName("Pooling Bucket");
        LabVessel vessel = labVesselDao.findByIdentifier("0155694973");

        BucketEntry bucketEntry = bucket.findEntry(vessel);
        bucket.removeEntry(bucketEntry);
    }

    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void remove0155694973FromPicoPlatingBucketGPLIM2503() {
        Bucket bucket = bucketDao.findByName("Pico/Plating Bucket");
        LabVessel vessel = labVesselDao.findByIdentifier("0155694973");

        BucketEntry bucketEntry = bucket.findEntry(vessel);
        bucket.removeEntry(bucketEntry);
    }

    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void remove0159876899FromPicoPlatingBucketGPLIM2503() {
        Bucket bucket = bucketDao.findByName("Pico/Plating Bucket");
        LabVessel vessel = labVesselDao.findByIdentifier("0159876899");

        BucketEntry bucketEntry = bucket.findEntry(vessel);
        bucket.removeEntry(bucketEntry);
    }

    @Test(groups = TestGroups.FIXUP, enabled = true)
    public void gplim5593RemoveBadPDOsFromBucket() {
        Bucket bucket = bucketDao.findByName("Pico/Plating Bucket");

        final BspUser ccusick = bspUserList.getByUsername("ccusick");
        final BspUser jBoch= bspUserList.getByUsername("jboch");

        System.out.println("Getting bucket entries from " + bucket.getBucketDefinitionName());

        final List<BucketEntry> bucketEntriesToDelete = bucket.getBucketEntries().stream().filter(entry -> {
            final Long pdoCreator = entry.getProductOrder().getCreatedBy();

            return pdoCreator.equals(ccusick.getUserId()) || pdoCreator.equals(jBoch.getUserId()) ||
                   entry.getProductOrder().getOrderStatus() == ProductOrder.OrderStatus.Completed ||
                   entry.getProductOrder().getOrderStatus() == ProductOrder.OrderStatus.Abandoned;
        }).collect(Collectors.toList());

        System.out.println("The number of entries found are " + bucketEntriesToDelete.size() + " from a total of " + bucket.getBucketEntries().size());

        System.out.println("Check out the buckets");
    }

    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void setProductOrderReferxences() throws Exception {

        List<BucketEntry> bucketEntriesToFix =
                bucketEntryDao.findList(BucketEntry.class, BucketEntry_.productOrder, null);
        int counter = 0;

        Set<String> badKeySet = new HashSet<>();

        Map<String, ProductOrder> pdoCache = new HashMap<>();
        while (!bucketEntriesToFix.isEmpty()) {

            for (Iterator<BucketEntry> entryIterator = bucketEntriesToFix.iterator(); entryIterator.hasNext(); ) {
                BucketEntry entry = entryIterator.next();
                if (StringUtils.isNotBlank(entry.getPoBusinessKey()) && !badKeySet.contains(entry.getPoBusinessKey())) {
                    ProductOrder orderToUpdate = pdoCache.get(entry.getPoBusinessKey());
                    if (orderToUpdate == null) {
                        orderToUpdate = bucketEntryDao.findSingle(ProductOrder.class, ProductOrder_.jiraTicketKey,
                                                                  entry.getPoBusinessKey());
                    }
                    if (orderToUpdate != null) {
                        pdoCache.put(entry.getPoBusinessKey(), orderToUpdate);
                        entry.setProductOrder(orderToUpdate);
                        counter++;
                    } else {
                        badKeySet.add(entry.getPoBusinessKey());
                        entryIterator.remove();
                    }
                } else {
                    entryIterator.remove();
                }
                if (counter == 100) {
                    counter = 0;
                    utx.commit();
                    utx.begin();
                    bucketEntriesToFix = bucketEntryDao.findList(BucketEntry.class, BucketEntry_.productOrder, null);
                    break;
                }
            }
        }
    }
}
