package org.broadinstitute.gpinformatics.mercury.boundary.bucket;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.boundary.BucketException;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactory;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Stateful
@RequestScoped
public class BucketEjb {
    private LabEventFactory labEventFactory;
    private JiraService jiraService;
    private BucketDao bucketDao;
    private LabVesselDao labVesselDao;
    private BucketEntryDao bucketEntryDao;
    private WorkflowLoader workflowLoader;
    private BSPUserList bspUserList;
    private LabVesselFactory labVesselFactory;
    private MercurySampleDao mercurySampleDao;

    /*
     * Uses BSPSampleDataFetcher (rather than SampleDataFetcher) to create LabVessels for samples that are in BSP but
     * are not yet known to Mercury.
     */
    private BSPSampleDataFetcher bspSampleDataFetcher;
    private ProductOrderDao productOrderDao;

    private static final Log logger = LogFactory.getLog(BucketEjb.class);

    public BucketEjb() {}

    @Inject
    public BucketEjb(LabEventFactory labEventFactory,
                     JiraService jiraService,
                     BucketDao bucketDao,
                     BucketEntryDao bucketEntryDao,
                     LabVesselDao labVesselDao,
                     LabVesselFactory labVesselFactory,
                     BSPSampleDataFetcher bspSampleDataFetcher,
                     BSPUserList bspUserList,
                     WorkflowLoader workflowLoader, ProductOrderDao productOrderDao, MercurySampleDao mercurySampleDao) {
        this.labEventFactory = labEventFactory;
        this.jiraService = jiraService;
        this.bucketDao = bucketDao;
        this.bucketEntryDao = bucketEntryDao;
        this.labVesselDao = labVesselDao;
        this.labVesselFactory = labVesselFactory;
        this.bspSampleDataFetcher = bspSampleDataFetcher;
        this.bspUserList = bspUserList;
        this.workflowLoader = workflowLoader;
        this.productOrderDao = productOrderDao;
        this.mercurySampleDao = mercurySampleDao;
    }

    /**
     * Adds a pre-defined collection of {@link LabVessel}s to the given bucket using the specified pdoBusinessKey.
     *
     * @param entriesToAdd  Collection of LabVessels to be added to a bucket
     * @param entryType     Type of BucketEntry, PDO_ENTRY or REWORK_ENTRY
     * @param programName   Name of the program that initiated this action
     * @param operator      Represents the user that initiated adding the vessels to the bucket
     * @param eventLocation Machine location from which operator initiated this action
     * @param pdo           Product order for all vessels
     */
    public Collection<BucketEntry> add(@Nonnull Map<WorkflowBucketDef, Collection<LabVessel>> entriesToAdd,
                                       @Nonnull BucketEntry.BucketEntryType entryType, @Nonnull String programName,
                                       @Nonnull String operator, @Nonnull String eventLocation,
                                       @Nonnull ProductOrder pdo, @Nonnull Date date) {
        List<BucketEntry> listOfNewEntries = new ArrayList<>(entriesToAdd.size());
        long disambiguatorOffset = 0;
        for (Map.Entry<WorkflowBucketDef, Collection<LabVessel>> bucketVesselsEntry : entriesToAdd.entrySet()) {
            Collection<LabVessel> bucketVessels = bucketVesselsEntry.getValue();
            WorkflowBucketDef bucketDef = bucketVesselsEntry.getKey();
            Bucket bucket = findOrCreateBucket(bucketDef.getName());
            LabEventType bucketEventType = bucketDef.getBucketEventType();

            for (LabVessel currVessel : bucketVessels) {
                if (!currVessel.checkCurrentBucketStatus(pdo, bucketDef.getName(), BucketEntry.Status.Active)) {
                    listOfNewEntries.add(bucket.addEntry(pdo, currVessel, entryType, date));
                }
            }
            long eventCount = CollectionUtils.emptyIfNull(labEventFactory.buildFromBatchRequests(listOfNewEntries,
                    operator, null, eventLocation, programName, bucketEventType, date, disambiguatorOffset)).size();
            disambiguatorOffset += eventCount;
        }

        return listOfNewEntries;
    }

    /**
     * Creates batch entries for the vessels, then moves the entries from the given bucket to the given batch.
     * This is primarily used by messaging.
     *
     * @param vesselsToBatch the vessels to be processed
     * @param bucket         the bucket containing the vessels
     */
    public void createEntriesAndBatchThem(@Nonnull Collection<LabVessel> vesselsToBatch, @Nonnull Bucket bucket) {
        moveFromBucketToCommonBatch(selectEntries(vesselsToBatch, bucket));
    }

    /**
     * Puts the bucket entries into the given batch and removes them from the bucket.
     *
     * @param bucketEntries the bucket entries to be batched.
     * @param labBatch      the lab batch receiving the entries.
     */
    public void moveFromBucketToBatch(@Nonnull Collection<BucketEntry> bucketEntries, LabBatch labBatch) {
        removeEntries(bucketEntries);

        for (BucketEntry bucketEntry : bucketEntries) {
            labBatch.addBucketEntry(bucketEntry);
        }
    }

    /**
     * Returns the bucket entries that correspond to the specified vessels in the bucket.
     */
    public Set<BucketEntry> selectEntries(Collection<LabVessel> vessels, Bucket bucket) {
        Set<BucketEntry> entries = new HashSet<>();

        for (LabVessel vessel : vessels) {

            BucketEntry entry = bucket.findEntry(vessel);
            if (entry != null) {
                logger.debug("Adding entry " + entry.getBucketEntryId() + " for vessel " +
                             entry.getLabVessel().getLabCentricName() + " and PDO " + entry.getProductOrder()
                                                                                           .getBusinessKey() +
                             " to be popped from bucket.");
                entries.add(entry);
            } else {
                logger.debug("Attempting to pull a vessel, " + vessel.getLabel() + ", from a bucket, " +
                             bucket.getBucketDefinitionName() + ", when it does not exist in that bucket");
            }
        }
        return entries;
    }

    /**
     * Selects the appropriate batch for the given bucket entries and moves the entries from the bucket to the batch.
     *
     * @param bucketEntries the bucket entries to be moved.
     *
     * @return the lab batch that the entries were put into.
     */
    @DaoFree
    public LabBatch moveFromBucketToCommonBatch(@Nonnull Collection<BucketEntry> bucketEntries) {

        // Gets the vessels.
        Set<LabVessel> batchVessels = new HashSet<>();
        for (BucketEntry entry : bucketEntries) {
            batchVessels.add(entry.getLabVessel());
        }

        // Determines the appropriate batch to use, and updates the bucket entries' batch.
        LabBatch bucketBatch = null;
        if (!batchVessels.isEmpty()) {
            for (LabBatch batch : batchVessels.iterator().next().getNearestWorkflowLabBatches()) {
                if (LabBatch.isCommonBatch(batch, batchVessels)) {
                    bucketBatch = batch;
                }
            }
        }
        for (BucketEntry currEntry : bucketEntries) {
            currEntry.setLabBatch(bucketBatch);
        }

        // Removes bucket entries from the bucket.
        removeEntries(bucketEntries);

        logger.info("Size of entries to remove is " + bucketEntries.size());
        return bucketBatch;
    }

    /**
     * Removes entries from a bucket and marks them as having been extracted.
     */
    //TODO Move to a bucket factory class
    private void removeEntries(@Nonnull Collection<BucketEntry> bucketEntries) {
        removeEntries(bucketEntries, "Extracted for Batch");
    }

    /**
     * Removes entry from a bucket and gives the reason why.
     */
    public void removeEntry(@Nonnull BucketEntry bucketEntry, String reason) {
        removeEntries(Collections.singletonList(bucketEntry), reason);
    }

    public void removeEntriesByIds(@Nonnull Collection<Long> bucketEntryIds, String reason) {
        List<BucketEntry> bucketEntries = bucketEntryDao.findByIds(new ArrayList<>(bucketEntryIds));
        removeEntries(bucketEntries, reason);
    }

    /**
     * Removes entries from a bucket and gives the reason why.
     */
    private void removeEntries(@Nonnull Collection<BucketEntry> bucketEntries, String reason) {
        for (BucketEntry currEntry : bucketEntries) {
            logger.debug("Removing entry " + currEntry.getBucketEntryId() +
                         " for vessel " + currEntry.getLabVessel().getLabCentricName() +
                         " and PDO " + currEntry.getProductOrder().getBusinessKey() +
                         " from bucket " + currEntry.getBucket().getBucketDefinitionName());
            currEntry.getBucket().removeEntry(currEntry);
        }
        jiraRemovalUpdate(bucketEntries, reason);
    }

    /**
     * Updates the product order Jira ticket for bucket entry removals.
     */
    private void jiraRemovalUpdate(@Nonnull Collection<BucketEntry> entries, String reason) {

        boolean isSingleEntry = (entries.size() == 1);

        Map<String, Collection<BucketEntry>> pdoToEntries = new HashMap<>();
        for (BucketEntry entry : entries) {
            Collection<BucketEntry> pdoEntries = pdoToEntries.get(entry.getProductOrder().getBusinessKey());
            if (pdoEntries == null) {
                pdoEntries = new ArrayList<>();
                pdoToEntries.put(entry.getProductOrder().getBusinessKey(), pdoEntries);
            }
            pdoEntries.add(entry);
        }
        for (Map.Entry<String, Collection<BucketEntry>> mapEntry : pdoToEntries.entrySet()) {
            BucketEntry firstBucketEntry = mapEntry.getValue().iterator().next();
            String comment;
            if (isSingleEntry) {
                comment = mapEntry.getKey() + ":" +
                          firstBucketEntry.getLabVessel().getLabCentricName() +
                          " removed from bucket " +
                          firstBucketEntry.getBucket().getBucketDefinitionName() + ":: " +
                          reason;
            } else {
                comment = mapEntry.getKey() + ":" +
                          mapEntry.getValue().size() +
                          " samples removed from bucket " +
                          firstBucketEntry.getBucket().getBucketDefinitionName() + ":: " +
                          reason;
            }
            try {
                jiraService.addComment(mapEntry.getKey(), comment);
            } catch (IOException ioe) {
                logger.error("Error attempting to create jira removal comment for " +
                             mapEntry.getKey() + " " +
                             mapEntry.getValue().size() + " samples.", ioe);
            }
        }
    }


    public Bucket findOrCreateBucket(String bucketName) {
        Bucket bucket = bucketDao.findByName(bucketName);
        if (bucket == null) {
            bucket = new Bucket(bucketName);
            bucketDao.persist(bucket);
            bucketDao.flush();
            logger.debug("Created new bucket " + bucketName);
        }
        return bucket;
    }

    /**
     * Update the PDO for the provided {@link BucketEntry}(s)
     *
     * @param bucketEntries Collection of bucket entries to update
     * @param newPdoValue   new value for PDO
     */
    public void updateEntryPdo(@Nonnull Collection<BucketEntry> bucketEntries, @Nonnull String newPdoValue) {
        List<String> updatingList = new ArrayList<>(bucketEntries.size());
        for (BucketEntry bucketEntry : bucketEntries) {
            bucketEntry.setProductOrder(productOrderDao.findByBusinessKey(newPdoValue));
            updatingList.add(bucketEntry.getLabVessel().getLabel());
        }
        logger.info(String.format("Changing PDO to %s for %d bucket entries (%s)", newPdoValue, updatingList.size(),
                                  StringUtils.join(updatingList, ", ")));

        bucketDao.persistAll(bucketEntries);
    }

    /**
     * Puts the product order samples from productOrder into the appropriate bucket
     *
     * @return the samples that were actually added to the bucket.
     */
    // todo jmt called from tests only
    public Map<String, Collection<ProductOrderSample>> addSamplesToBucket(ProductOrder productOrder) {
        return addSamplesToBucket(productOrder, productOrder.getSamples(),
                ProductWorkflowDefVersion.BucketingSource.PDO_SUBMISSION);
    }

    /**
     * Puts product order samples into the appropriate bucket.  Does nothing if the product is not supported in Mercury.
     *
     * @return map of bucket name and the samples that were added to that bucket
     */
    public Map<String, Collection<ProductOrderSample>> addSamplesToBucket(ProductOrder order,
            Collection<ProductOrderSample> samples, ProductWorkflowDefVersion.BucketingSource bucketingSource) {
        boolean hasWorkflow = false;
        for (String workflow : order.getProductWorkflows()) {
            ProductWorkflowDef productWorkflowDef = workflowLoader.load().getWorkflowByName(workflow);
            ProductWorkflowDefVersion workflowDefVersion = productWorkflowDef.getEffectiveVersion();
            hasWorkflow = !workflowDefVersion.getBuckets().isEmpty();
            if (hasWorkflow){
                break;
            }
        }
        if (!hasWorkflow) {
            return Collections.emptyMap();
        }

        String username = null;
        Long bspUserId = order.getCreatedBy();
        if (bspUserId != null) {
            BspUser bspUser = bspUserList.getById(bspUserId);
            if (bspUser != null) {
                username = bspUser.getUsername();
            }
        }

        // Finds existing vessels for the pdo samples, or if none, then either the sample has not been received
        // or the sample is a derived stock that has not been seen by Mercury.  Creates both standalone vessel and
        // MercurySample for the latter.
        Multimap<String, ProductOrderSample> nameToPdoSample = ArrayListMultimap.create();
        Set<LabVessel> vessels = new HashSet<>();
        Multimap<String, ProductOrderSample> vesselToPdoSample = HashMultimap.create();
        Map<String, ProductOrderSample> nameToUnknownPdoSample = new HashMap<>();
        Multimap<String, ProductOrderSample> nameToUnlinkedPdoSample = HashMultimap.create();
        for (ProductOrderSample pdoSample : samples) {
            // A pdo can have multiple samples all with same sample name but with different sample position.
            // Each one will get a MercurySample and LabVessel put into the bucket.
            nameToPdoSample.put(pdoSample.getName(), pdoSample);

            Collection<LabVessel> labVessels = labVesselDao.findBySampleKey(pdoSample.getName());
            if (CollectionUtils.isNotEmpty(labVessels)) {
                vessels.addAll(labVessels);
                for (LabVessel labVessel : labVessels) {
                    vesselToPdoSample.put(labVessel.getLabel(), pdoSample);
                }
                if (pdoSample.getMercurySample() == null) {
                    nameToUnlinkedPdoSample.put(pdoSample.getName(), pdoSample);
                }
            } else {
                // The PDO sample name may a tube barcode, and may be different than
                // the name of sample(s) in the tube.
                LabVessel labVessel = labVesselDao.findByIdentifier(pdoSample.getName());
                if (labVessel != null) {
                    vessels.add(labVessel);
                    vesselToPdoSample.put(labVessel.getLabel(), pdoSample);
                    if (pdoSample.getMercurySample() == null) {
                        // Adds the mercury sample link(s) to pdo.
                        for (MercurySample mercurySample : labVessel.getMercurySamples()) {
                            mercurySample.addProductOrderSample(pdoSample);
                        }
                    }
                } else {
                    // Collects the PDO sample names that need vessel/sample creation.
                    nameToUnknownPdoSample.put(pdoSample.getName(), pdoSample);
                    if (pdoSample.getMercurySample() == null) {
                        nameToUnlinkedPdoSample.put(pdoSample.getName(), pdoSample);
                    }
                }
            }
        }

        // Creates vessels for PDO sample names that are in BSP but not Mercury.
        // Throws an unchecked exception if passed a barcode or a sample id not in BSP.
        if (!nameToUnknownPdoSample.isEmpty()) {
            Collection<LabVessel> newVessels = createInitialVessels(nameToUnknownPdoSample.keySet(), username);
            vessels.addAll(newVessels);
            for (LabVessel labVessel : newVessels) {
                for (MercurySample mercurySample : labVessel.getMercurySamples()) {
                    ProductOrderSample pdoSample = nameToUnknownPdoSample.get(mercurySample.getSampleKey());
                    vesselToPdoSample.put(labVessel.getLabel(), pdoSample);
                }
            }
        }

        // For Pdo samples whose name is a Mercury sample id, adds a MercurySample link.
        for (Map.Entry<String, MercurySample> entry :
                mercurySampleDao.findMapIdToMercurySample(nameToUnlinkedPdoSample.keySet()).entrySet()) {
            for (ProductOrderSample productOrderSample : nameToUnlinkedPdoSample.get(entry.getKey())) {
                entry.getValue().addProductOrderSample(productOrderSample);
            }
        }

        // Adds the lab vessels to appropriate buckets.
        Pair<ProductWorkflowDefVersion, Collection<BucketEntry>> workflowBucketEntriesPair = applyBucketCriteria(
                new ArrayList<>(vessels), order, username, bucketingSource, new Date());
        Collection<BucketEntry> newBucketEntries = workflowBucketEntriesPair.getRight();

        // Returns the Pdo samples of the newly added bucketed vessels.
        Map<String, Collection<ProductOrderSample>> bucketToPdoSamples = new HashMap<>();
        for (BucketEntry bucketEntry : newBucketEntries) {
            String bucketName = bucketEntry.getBucket().getBucketDefinitionName();
            Collection<ProductOrderSample> pdoSamples = bucketToPdoSamples.get(bucketName);
            if (pdoSamples == null) {
                pdoSamples = new HashSet<>();
                bucketToPdoSamples.put(bucketName, pdoSamples);
            }
            String bucketVesselBarcode = bucketEntry.getLabVessel().getLabel();
            pdoSamples.addAll(vesselToPdoSample.get(bucketVesselBarcode));
        }
        return bucketToPdoSamples;
    }


    public Pair<ProductWorkflowDefVersion, Collection<BucketEntry>> applyBucketCriteria(
            List<LabVessel> vessels, ProductOrder productOrder, String username,
            ProductWorkflowDefVersion.BucketingSource bucketingSource, Date date) {
        Collection<BucketEntry> bucketEntries = new ArrayList<>(vessels.size());
        List<Product> possibleProducts = new ArrayList<>();
        for (ProductOrderAddOn productOrderAddOn : productOrder.getAddOns()) {
            if (productOrderAddOn.getAddOn().getWorkflowName() != null) {
                possibleProducts.add(productOrderAddOn.getAddOn());
            }
        }
        possibleProducts.add(productOrder.getProduct());
        ProductWorkflowDefVersion workflowDefVersion = null;
        int offset = 0;
        for (Product product : possibleProducts) {
            Date localDate = date;
            if (offset > 0) {
                // Avoid unique constraint on bucket lab events
                localDate = new Date(date.getTime() + offset);
            }
            if (product.getWorkflowName() != null) {
                ProductWorkflowDef productWorkflowDef = workflowLoader.load().getWorkflowByName(product.getWorkflowName());
                workflowDefVersion = productWorkflowDef.getEffectiveVersion();
                Map<WorkflowBucketDef, Collection<LabVessel>> initialBucket =
                        workflowDefVersion.getInitialBucket(productOrder, vessels, bucketingSource);

                if (!initialBucket.isEmpty()) {
                    Collection<BucketEntry> entries = add(initialBucket, BucketEntry.BucketEntryType.PDO_ENTRY,
                            LabEvent.UI_PROGRAM_NAME, username, LabEvent.UI_EVENT_LOCATION, productOrder, localDate);
                    bucketEntries.addAll(entries);
                }
            }
            offset++;
        }
        return new ImmutablePair<>(workflowDefVersion, bucketEntries);
    }

    /**
     * Creates LabVessels with BSP receptacle barcodes for the given BSP sample IDs.
     * Throws exception if BSP does not know about all of the sample IDs.
     *
     * @param samplesWithoutVessel BSP sample IDs that need LabVessels
     * @param username             the user performing the operation leading to the LabVessels being created
     *
     * @return the created LabVessels
     */
    public Collection<LabVessel> createInitialVessels(Collection<String> samplesWithoutVessel, String username) {
        Map<String, BspSampleData> bspSampleDataMap = bspSampleDataFetcher.fetchSampleData(samplesWithoutVessel);
        // todo jmt why not fetch manufacturer barcode?
        Collection<LabVessel> vessels = new ArrayList<>();
        List<String> cannotAddToBucket = new ArrayList<>();
        List<String> missingSampleData = new ArrayList<>();
        for (String sampleName : samplesWithoutVessel) {
            BspSampleData bspSampleData = bspSampleDataMap.get(sampleName);
            if (bspSampleData == null || bspSampleData.getMaterialType() == null) {
                missingSampleData.add(sampleName);
            } else if (bspSampleData != null && StringUtils.isNotBlank(bspSampleData.getBarcodeForLabVessel())) {
                if (bspSampleData.isSampleReceived()) {
                    // Process is only interested in the primary vessels
                    List<LabVessel> sampleVessels = labVesselFactory.buildInitialLabVessels(sampleName, bspSampleData.getBarcodeForLabVessel(),
                            username, new Date(), MercurySample.MetadataSource.BSP).getLeft();
                    vessels.addAll(sampleVessels );
                }
            } else {
                cannotAddToBucket.add(sampleName);
            }
        }
        if (!missingSampleData.isEmpty()) {
            throw new BucketException("Samples were not added to the bucket. Could not find BSP or Mercury " +
                    "sample data for: " + StringUtils.join(missingSampleData, ", "));
        }
        if (!cannotAddToBucket.isEmpty()) {
            throw new BucketException("Samples were not added to the bucket. Neither BSP nor Mercury have a " +
                    "labeled vessel for: " + StringUtils.join(cannotAddToBucket, ", "));
        }
        if (!vessels.isEmpty()) {
            labVesselDao.persistAll(vessels);
            labVesselDao.flush();
        }
        return vessels;
    }
}
