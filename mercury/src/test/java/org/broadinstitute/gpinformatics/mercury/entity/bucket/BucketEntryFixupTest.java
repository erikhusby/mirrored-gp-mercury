package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import com.google.common.collect.ArrayListMultimap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
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
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void support4176RemoveBadPDOsFromBucket() throws Exception{
        userBean.loginOSUser();
        final String jiraTicket = "SUPPORT-4176";

        removeBucketEntriesFromInactiveProductOrders(jiraTicket);
    }

    private void removeBucketEntriesFromInactiveProductOrders(String jiraTicket) {
        List<Bucket> buckets = bucketDao.findAll(Bucket.class);

        ArrayListMultimap<Bucket, BucketEntry> entryMapping = ArrayListMultimap.create();
        for(Bucket bucket: buckets) {
            final List<BucketEntry> collect = bucket.getBucketEntries().stream().filter(entry ->
                    (entry.getProductOrder().getOrderStatus() == ProductOrder.OrderStatus.Completed ||
                     entry.getProductOrder().getOrderStatus() == ProductOrder.OrderStatus.Abandoned) &&
                    entry.getStatus() == BucketEntry.Status.Active)
                    .collect(Collectors.toList());
            entryMapping.putAll(bucket, collect);
            System.out.println(jiraTicket + " Deleting " + collect.size() + " entries from a total of " + bucket.getBucketEntries().size() + " Bucket entries which are from PDOs which are either Completed, Abandoned in " + bucket.getBucketDefinitionName());
        }

        for(Map.Entry<Bucket, BucketEntry> collectionEntries: entryMapping.entries()) {
            collectionEntries.getKey().removeEntry(collectionEntries.getValue());
        }

        bucketDao.persist(new FixupCommentary(
                jiraTicket + " Removed Completed and Abandoned bucket entries from all Buckets"));
    }

    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void setProductOrderReferences() throws Exception {

        utx.begin();
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
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupGplim5745() throws IOException, SystemException, NotSupportedException,
            HeuristicRollbackException, HeuristicMixedException, RollbackException {
        userBean.loginOSUser();
        utx.begin();
        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("DeleteBucketEntries.txt"));
        for(int i = 1; i < lines.size(); i++) {
            BucketEntry bucketEntry = bucketEntryDao.findById(BucketEntry.class, Long.valueOf(lines.get(i)));
            System.out.println("Deleting bucket entry " + bucketEntry.getBucketEntryId());
            bucketEntryDao.remove(bucketEntry);
        }
        bucketEntryDao.persist(new FixupCommentary(lines.get(0)));
        utx.commit();
    }

    @Test(enabled = false)
    public void gplim5419_RemoveBucketEntry() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        for (Long id : Arrays.asList(487719L, 487801L)) {
            BucketEntry bucketEntry = bucketEntryDao.findById(BucketEntry.class, id);
            Assert.assertNotNull(bucketEntry);
            System.out.println("Removing bucket entry " + bucketEntry.getBucketEntryId());
            bucketEntryDao.remove(bucketEntry);
        }

        bucketEntryDao.persist(new FixupCommentary("GPLIM-5419 Followup fixup to remove bucket entries."));
        bucketEntryDao.flush();
        utx.commit();
    }

    /** Adds back a bucket entry that was deleted by LabBatchFixUpTest.deleteCancelledLcset */
    @Test(enabled = false)
    public void support5662_addBucketEntry() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        //bucket entry id 942665 will be replaced by a new one
        LabVessel tube = labVesselDao.findById(LabVessel.class, 7168834L);
        Assert.assertNotNull(tube);
        Bucket bucket = labVesselDao.findById(Bucket.class, 16052L);
        Assert.assertNotNull(bucket);
        Date date = FastDateFormat.getInstance("yyyy-MM-dd-HH:mm:ss").parse("2019-08-06-12:39:58");
        LabBatch lcset = labVesselDao.findById(LabBatch.class, 401302L);
        Assert.assertNotNull(lcset);
        ProductOrder pdo = productOrderDao.findById(379706L);
        Assert.assertNotNull(pdo);
        Assert.assertTrue(pdo.getBusinessKey().equals("PDO-19098"));

        System.out.println("Adding " + bucket.getBucketDefinitionName() + " bucket entry for tube " + tube.getLabel());
        BucketEntry bucketEntry = bucket.addEntry(pdo, tube, BucketEntry.BucketEntryType.PDO_ENTRY, date);
        bucketEntry.setLabBatch(lcset);
        bucketEntry.setStatus(BucketEntry.Status.Archived);

        bucketEntryDao.persistAll(Arrays.asList(bucket, bucketEntry, new FixupCommentary(
                "SUPPORT-5662 Followup fixup to add bucket entry deleted by deleteCancelledLcset fixup test.")));
        bucketEntryDao.flush();
        utx.commit();
    }

}
