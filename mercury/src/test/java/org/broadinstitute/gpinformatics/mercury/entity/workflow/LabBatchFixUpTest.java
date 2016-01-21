/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.common.BaseSplitter;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.ReworkDetail;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

//import com.jprofiler.api.agent.Controller;

@Test(groups = TestGroups.FIXUP)
public class LabBatchFixUpTest extends Arquillian {

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private LabBatchEjb labBatchEjb;

    @Inject
    private BucketDao bucketDao;

    @Inject
    private UserTransaction userTransaction;

    @Inject
    private ControlDao controlDao;

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    @Inject
    private UserBean userBean;

    @Inject
    private MercurySampleDao mercurySampleDao;

    // Use (RC, "rc"), (PROD, "prod") to push the backfill to RC and production respectively.
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false)
    public void updateNullLabBatchType() {
        final List<LabBatch> nullTypes = labBatchDao.findList(LabBatch.class, LabBatch_.labBatchType, null);
        List<LabBatch> fixedBatches = new ArrayList<>(nullTypes.size());
        for (LabBatch nullType : nullTypes) {
            if (nullType.getLabBatchType() != null) {
                throw new IllegalStateException(
                        "wait, why did the dao return me? " + nullType.getLabBatchId() + " " + nullType
                                .getBatchName() + " " + nullType.getLabBatchType());

            }
            if (nullType.getBatchName().startsWith("BP-")) {
                nullType.setLabBatchType(LabBatch.LabBatchType.BSP);
            } else if (nullType.getBatchName().startsWith("LCSET-")) {
                nullType.setLabBatchType(LabBatch.LabBatchType.WORKFLOW);
            } else if (nullType.getBatchName().startsWith("EX-")) {
                nullType.setLabBatchType(LabBatch.LabBatchType.SAMPLES_IMPORT);
            } else if (nullType.getBatchName().startsWith("SK-")) {
                nullType.setLabBatchType(LabBatch.LabBatchType.SAMPLES_RECEIPT);
            } else {
                throw new IllegalStateException(
                        "I don't know what type of lab batch this is! " + nullType.getLabBatchId() + " " + nullType
                                .getBatchName());
            }
        }
        if (!fixedBatches.isEmpty()) {
            labBatchDao.persistAll(fixedBatches);
        }

    }

    @Test(enabled = false)
    public void updateStartingLabBatches() {
        List<LabBatch> allBatches = labBatchDao.findAll(LabBatch.class);
        List<LabBatch> fixedBatches = new ArrayList<>(allBatches.size());
        for (LabBatch batch : allBatches) {
            Set<LabVessel> oldVessels = batch.getStartingLabVessels();
            for (LabVessel vessel : oldVessels) {
                batch.addLabVessel(vessel);
            }
            fixedBatches.add(batch);
        }
        if (!fixedBatches.isEmpty()) {
            labBatchDao.persistAll(fixedBatches);
        }
    }

    /**
     * LCSET-3792 is supposed to be routed to Mercury, but it was created on July 24th, so change to
     * July 25th.
     */
    @Test(enabled = false)
    public void updateLcset3792ToMercury() {
        LabBatch labBatch = labBatchDao.findByName("LCSET-3792");
        GregorianCalendar gregorianCalendar = new GregorianCalendar(2013, 6, 25, 1, 1, 1);
        labBatch.setCreatedOn(gregorianCalendar.getTime());
        labBatchDao.flush();
    }

    /** Rename Exome Express to Agilent Exome Express. */
    @Test(enabled = false)
    public void updateWorkflowName() {
        List<LabBatch> updates = new ArrayList<>();
        for (LabBatch batch : labBatchDao.findAll(LabBatch.class)) {
            if ("Exome Express".equals(batch.getWorkflowName())) {
                batch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
                updates.add(batch);
            }
        }
        if (!updates.isEmpty()) {
            labBatchDao.persistAll(updates);
        }
    }

    @Test(enabled = false)
    public void fixupGplim2355() {
        List<String> batchNames = new ArrayList<>();
        batchNames.add("EX-41050");
        batchNames.add("EX-41052");
        batchNames.add("EX-41054");
        batchNames.add("EX-41056");
        batchNames.add("EX-41058");
        batchNames.add("EX-41060");
        batchNames.add("EX-41062");
        batchNames.add("EX-41066");
        batchNames.add("EX-41068");
        for (String batchName : batchNames) {
            LabBatch labBatch = labBatchDao.findByName(batchName);
            labBatch.setBatchName(batchName + "x");
        }
        labBatchDao.flush();
    }

    /**
     * Auto-bucketing failed, so samples were added as reworks, but getSampleInstances doesn't handle this.
     */
    @Test(enabled = false)
    public void fixupIpi61025() {
        LabBatch labBatch = labBatchDao.findByName("LCSET-4828");
        for (BucketEntry bucketEntry : labBatch.getBucketEntries()) {
            ReworkDetail reworkDetail = bucketEntry.getReworkDetail();
            if (reworkDetail != null) {
                if (!reworkDetail.getComment().equals("rework from shearing")) {
                    bucketEntry.setReworkDetail(null);
                }
            }
        }
        labBatchDao.flush();
    }

    @Test(enabled = false)
    public void removeSamplesFromLcset() {
        LabBatch lcset = labBatchDao.findByName("LCSET-5332");
        Set<LabBatchStartingVessel> vesselsToRemoveFromBatch = new HashSet<>();
        Set<String> samplesToRemove = new HashSet<>();
        samplesToRemove.add("SM-5R5KW");
        samplesToRemove.add("SM-5U3X7");
        samplesToRemove.add("SM-5U3X9");

        for (LabBatchStartingVessel startingVessel : lcset.getLabBatchStartingVessels()) {
            Set<SampleInstance> sampleInstances = startingVessel.getLabVessel().getSampleInstances();
            for (SampleInstance sampleInstance : sampleInstances) {
                String sample = sampleInstance.getStartingSample().getSampleKey();
                if (samplesToRemove.contains(sample)) {
                    vesselsToRemoveFromBatch.add(startingVessel);
                }
            }
        }
        int numStartingVessels = lcset.getLabBatchStartingVessels().size();
        for (LabBatchStartingVessel vesselToRemove : vesselsToRemoveFromBatch) {
            lcset.getLabBatchStartingVessels().remove(vesselToRemove);
            vesselToRemove.getLabVessel().getLabBatchStartingVessels().remove(vesselToRemove);
        }
        int numVesselsAtEnd = lcset.getLabBatchStartingVessels().size();
        System.out.println("Removed " + (numStartingVessels - numVesselsAtEnd) + " samples from " + lcset.getBatchName());
        labBatchDao.flush();
    }

    @Test(enabled = false)
    public void fixupGplim2921() {
        LabBatch labBatch = labBatchDao.findByName("LCSET-5704");
        LabVessel labVessel = labVesselDao.findByIdentifier("0156349196");
        ProductOrder productOrder = productOrderDao.findByBusinessKey("PDO-3974");
        BucketEntry bucketEntry = new BucketEntry(labVessel, productOrder, BucketEntry.BucketEntryType.PDO_ENTRY);
        bucketEntry.setStatus(BucketEntry.Status.Archived);
        labBatch.addBucketEntry(bucketEntry);
        labBatchDao.flush();
    }

    @Test(enabled = false)
    public void fixupGplim3547() {
        userBean.loginOSUser();
        try {
            userTransaction.begin();
            String batchName = "LCSET-7158";
            LabBatch labBatch = labBatchDao.findByName(batchName);

            Map<String, String> mapOldSampleIdToNew = new HashMap<>();
            mapOldSampleIdToNew.put("SM-9LIRY", "SM-9MKCY");
            mapOldSampleIdToNew.put("SM-9LIRZ", "SM-9MKCZ");
            mapOldSampleIdToNew.put("SM-9LIS1", "SM-9MKD1");
            mapOldSampleIdToNew.put("SM-9LIS2", "SM-9MKD2");
            mapOldSampleIdToNew.put("SM-9LIS3", "SM-9MKD3");
            Map<String, MercurySample> mapIdToMercurySample = mercurySampleDao.findMapIdToMercurySample(
                    mapOldSampleIdToNew.values());

            int foundCount = 0;
            Set<LabVessel> oldLabVessels = new HashSet<>();
            Set<LabVessel> newLabVessels = new HashSet<>();

            // Change bucket entries
            for (BucketEntry bucketEntry : labBatch.getBucketEntries()) {
                Set<SampleInstanceV2> sampleInstancesV2 = bucketEntry.getLabVessel().getSampleInstancesV2();
                Assert.assertEquals(sampleInstancesV2.size(), 1);
                SampleInstanceV2 sampleInstanceV2 = sampleInstancesV2.iterator().next();
                String newMercurySampleId = mapOldSampleIdToNew.get(sampleInstanceV2.getNearestMercurySampleName());
                if (newMercurySampleId != null) {
                    MercurySample newMercurySample = mapIdToMercurySample.get(newMercurySampleId);
                    oldLabVessels.add(bucketEntry.getLabVessel());
                    Assert.assertEquals(newMercurySample.getLabVessel().size(), 1);
                    LabVessel newLabVessel = newMercurySample.getLabVessel().iterator().next();
                    bucketEntry.setLabVessel(newLabVessel);
                    newLabVessels.add(newLabVessel);
                    foundCount++;
                }
            }
            Assert.assertEquals(foundCount, 5);

            // Change reworks
            for (LabVessel oldLabVessel : oldLabVessels) {
                Assert.assertTrue(labBatch.getReworks().remove(oldLabVessel));
            }
            labBatch.getReworks().addAll(newLabVessels);

            labBatchDao.persist(new FixupCommentary("GPLIM-3547 change bucket entries for " + batchName));
            labBatchDao.flush();
            userTransaction.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException |
                HeuristicRollbackException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generate a file to back populate controls into LCSETs.
     */
    @Test(enabled = false)
    public void fixupGplim3442GenFile() {
//        Controller.startCPURecording(true);
//        Controller.startProbeRecording(Controller.PROBE_NAME_JDBC, true);

        Map<String, List<LabEventType>> mapBucketToEvents = new HashMap<>();
        mapBucketToEvents.put("Pico/Plating Bucket", Collections.singletonList(LabEventType.SHEARING_TRANSFER));
        mapBucketToEvents.put("Shearing Bucket", Collections.singletonList(LabEventType.SHEARING_TRANSFER));
        mapBucketToEvents.put("ICE Bucket", Collections.singletonList(LabEventType.ICE_POOLING_TRANSFER));
        mapBucketToEvents.put("Hybridization Bucket", Collections.singletonList(LabEventType.HYBRIDIZATION));
        mapBucketToEvents.put("Pooling Bucket", Collections.singletonList(LabEventType.POOLING_TRANSFER));

        List<Control> controls = controlDao.findAllActive();
        List<String> controlAliases = new ArrayList<>();
        for (Control control : controls) {
            controlAliases.add(control.getCollaboratorParticipantId());
        }

        List<LabBatch> workflowLabBatches = labBatchDao.findByType(LabBatch.LabBatchType.WORKFLOW);
        List<String> batchNames = new ArrayList<>();
        for (LabBatch workflowLabBatch : workflowLabBatches) {
            batchNames.add(workflowLabBatch.getBatchName());
        }
        labBatchDao.clear();
        Collections.sort(batchNames);
        List<Collection<String>> listListBatchNames = BaseSplitter.split(batchNames, 20);

        for (Collection<String> listBatchName : listListBatchNames) {
            workflowLabBatches = labBatchDao.findByListIdentifier(new ArrayList<>(listBatchName));
            for (LabBatch workflowLabBatch : workflowLabBatches) {
                int bucketEntryCount = workflowLabBatch.getBucketEntries().size();
                Set<LabVessel> labVessels = new HashSet<>();
                for (BucketEntry bucketEntry : workflowLabBatch.getBucketEntries()) {
                    LabVessel bucketEntryLabVessel = bucketEntry.getLabVessel();
                    if (bucketEntry.getBucket() == null) {
                        continue;
                    }
                    String bucketDefinitionName = bucketEntry.getBucket().getBucketDefinitionName();
                    List<LabEventType> labEventTypes = mapBucketToEvents.get(bucketDefinitionName);
                    if (labEventTypes == null) {
                        throw new RuntimeException("No event types for bucket " + bucketDefinitionName);
                    }
                    labVessels.addAll(getEventLabVessels(labEventTypes, bucketEntryLabVessel));
                }
                boolean found = false;
                if (!labVessels.isEmpty()) {
                    List<String> sampleNames = new ArrayList<>();
                    for (LabVessel labVessel : labVessels) {
                        for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                            sampleNames.add(sampleInstanceV2.getEarliestMercurySampleName());
                        }
                    }
                    Map<String, SampleData> mapSampleNameToData = sampleDataFetcher.fetchSampleData(sampleNames);
                    for (LabVessel labVessel : labVessels) {
                        int foundCount;
                        if (OrmUtil.proxySafeIsInstance(labVessel, BarcodedTube.class)) {
                            foundCount = labVessels.size();
                        } else if (OrmUtil.proxySafeIsInstance(labVessel, TubeFormation.class)) {
                            foundCount = labVessel.getContainerRole().getContainedVessels().size();
                        } else {
                            throw new RuntimeException("Unexpected class " + labVessel.getClass());
                        }
                        System.out.println("For " + workflowLabBatch.getBatchName() +
                                ", found lab vessels count = " + foundCount +
                                ", bucket entries size = " + bucketEntryCount);
                        int difference = foundCount - bucketEntryCount;
                        if (difference < 0 || difference > 4) {
                            System.out.println("Difference out of range: " + difference);
                        }
                        if (OrmUtil.proxySafeIsInstance(labVessel, BarcodedTube.class)) {
                            found = processTube(workflowLabBatch, found, (BarcodedTube) labVessel,
                                    mapSampleNameToData, controlAliases);
                        } else if (OrmUtil.proxySafeIsInstance(labVessel, TubeFormation.class)) {
                            for (LabVessel containedVessel : labVessel.getContainerRole().getContainedVessels()) {
                                found = processTube(workflowLabBatch, found, (BarcodedTube) containedVessel,
                                        mapSampleNameToData, controlAliases);
                            }
                        }
                    }
                }
                if (!found) {
                    System.out.println("Failed to find any controls for " + workflowLabBatch.getBatchName());
                }
            }
            System.out.println("Clear session");
            labBatchDao.clear();
        }
//        Controller.stopProbeRecording(Controller.PROBE_NAME_JDBC);
//        Controller.stopCPURecording();
    }

    private Set<LabVessel> getEventLabVessels(List<LabEventType> labEventTypes, LabVessel bucketEntryLabVessel) {
        Set<LabVessel> labVessels = new HashSet<>();
        for (LabEvent labEvent : bucketEntryLabVessel.getTransfersFrom()) {
            if (labEventTypes.contains(labEvent.getLabEventType())) {
                labVessels.addAll(labEvent.getSourceLabVessels());
            }
        }
        if (labVessels.isEmpty()) {
            Map<LabEvent, Set<LabVessel>> vesselsForLabEventTypes =
                    bucketEntryLabVessel.findVesselsForLabEventTypes(labEventTypes,
                            Arrays.asList(TransferTraverserCriteria.TraversalDirection.Descendants), false);
            for (Set<LabVessel> vessels : vesselsForLabEventTypes.values()) {
                labVessels.addAll(vessels);
            }
        }
        return labVessels;
    }

    private boolean processTube(LabBatch workflowLabBatch, boolean found,
            BarcodedTube labVessel, Map<String, SampleData> mapSampleNameToData, List<String> controlAliases) {
        Set<SampleInstanceV2> sampleInstances = labVessel.getSampleInstancesV2();
        if (sampleInstances.size() == 1) {
            SampleInstanceV2 sampleInstance = sampleInstances.iterator().next();
            SampleData sampleData = mapSampleNameToData.get(sampleInstance.getEarliestMercurySampleName());
            if (controlAliases.contains(sampleData.getCollaboratorParticipantId())) {
                found = true;
                System.out.println("Add " + labVessel.getLabel() + " to " +
                        workflowLabBatch.getBatchName() + " " +
                        sampleInstance.getEarliestMercurySampleName());
            }
        }
        return found;
    }

    /**
     * Read a file to back populate controls into LCSETs.
     */
    @Test(enabled = false)
    public void fixupGplim3442() {
        userBean.loginOSUser();
        InputStream testSpreadSheetInputStream = VarioskanParserTest.getTestResource("BackfillInput.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(testSpreadSheetInputStream));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] words = line.split("\\s+");
                if (words[0].startsWith("#")) {
                    continue;
                }
                LabVessel labVessel = labVesselDao.findByIdentifier(words[0]);
                LabBatch labBatch = labBatchDao.findByName(words[1]);
                labBatch.addLabVessel(labVessel);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        labBatchDao.persist(new FixupCommentary("GPLIM-3442 back fill controls into LCSETs"));
        labBatchDao.flush();
    }

    /**
     * Tubes were put in both LCSET-7015 and 7016. 7016 was then cancelled. Need to unlink the tubes from 7016.
     *
     * This test should result in audit changes to Bucket and LabBatch (both no-op changes),
     * deletes in BucketEntry and LabBatchStartingVessel, change to BarcodedTube, and add to FixupCommentary.
     */
    @Test(enabled = false)
    public void gplim3493unlinkLcset7016() throws Exception {
        userBean.loginOSUser();

        final LabBatch undesiredLcset = labBatchDao.findByName("LCSET-7016");
        Assert.assertNotNull(undesiredLcset);

        for (Iterator<LabBatchStartingVessel> lbsvIter = undesiredLcset.getLabBatchStartingVessels().iterator();
             lbsvIter.hasNext(); ) {
            LabBatchStartingVessel labBatchStartingVessel = lbsvIter.next();

            // Removing the undesired bucket entry should leave starting vessel with another bucket entry.
            BucketEntry undesiredBucketEntry = null;
            boolean foundOtherBucketEntry = false;
            for (BucketEntry bucketEntry : labBatchStartingVessel.getLabVessel().getBucketEntries()) {
                if (undesiredLcset.getBatchName().equals(bucketEntry.getLabBatch().getBatchName())) {
                    undesiredBucketEntry = bucketEntry;
                } else {
                    foundOtherBucketEntry = true;
                }
            }
            if (undesiredBucketEntry != null) {
                if (foundOtherBucketEntry) {
                    System.out.println("Removing bucketEntry " + undesiredBucketEntry.getBucketEntryId() +
                                       " and batch starting vessel " + labBatchStartingVessel.getBatchStartingVesselId());

                    // Unlinks the bucket entry from the vessel.
                    labBatchStartingVessel.getLabVessel().getBucketEntries().remove(undesiredBucketEntry);
                    // Unlinks the bucket entry from the batch.
                    undesiredLcset.getBucketEntries().remove(undesiredBucketEntry);
                    // Unlinks the starting vessel from the batch.
                    lbsvIter.remove();
                } else {
                    System.out.println("Keeping vessel's only bucketEntry " + undesiredBucketEntry.getBucketEntryId());
                }
            }
        }

        labBatchDao.persist(new FixupCommentary("GPLIM-3493 unlink all tubes from " + undesiredLcset.getBatchName()));
        labBatchDao.flush();
    }


    @Test(enabled = false)
    public void fixupQual680(){
        createFctsWithoutLcset(Collections.singletonList("AB56073486"), IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell,
                8, new BigDecimal("15"), "QUAL-680 create FCT tickets without LCSET");
    }

    /**
     * If a denature tube contains only positive controls, it doesn't have an LCSET (until control inference is
     * removed), so the Create FCT Ticket page can't be used.  This method is adapted from that ActionBean.
     */
    private void createFctsWithoutLcset(List<String> selectedVesselLabels,
            IlluminaFlowcell.FlowcellType selectedType, int numberOfLanes, BigDecimal loadingConc, String reason) {
        try {
            userBean.loginOSUser();
            userTransaction.begin();
            List<LabBatch> createdBatches = new ArrayList<>();
            for (String denatureTubeBarcode : selectedVesselLabels) {
                LabBatch.LabBatchType batchType = selectedType.getBatchType();
                CreateFields.IssueType issueType = selectedType.getIssueType();

                // Puts all vessels on all flowcell lanes.
                List<VesselPosition> lanes = new ArrayList<>();
                for (VesselPosition vesselPosition : selectedType.getVesselGeometry().getVesselPositions()) {
                    lanes.add(vesselPosition);
                }
                List<String> uniqueLabels = new ArrayList<String>(new HashSet<String>(selectedVesselLabels));
                List<LabBatch.VesselToLanesInfo> vesselToLanesInfos = new ArrayList<>();
                for (LabVessel vessel : labVesselDao.findByListIdentifiers(uniqueLabels)) {
                    vesselToLanesInfos.add(new LabBatch.VesselToLanesInfo(lanes, loadingConc, vessel));
                }

                LabBatch batch = new LabBatch(denatureTubeBarcode + " FCT ticket", vesselToLanesInfos, batchType,
                        selectedType);
                batch.setBatchDescription(batch.getBatchName());
                labBatchEjb.createLabBatch(batch, userBean.getLoginUserName(), issueType);
                createdBatches.add(batch);
            }
            labBatchDao.persist(new FixupCommentary(reason));
            labBatchDao.flush();
            userTransaction.commit();
            for (LabBatch createdBatch : createdBatches) {
                System.out.println("Created " + createdBatch.getBatchName());
            }
        } catch (NotSupportedException | SystemException | HeuristicMixedException | HeuristicRollbackException |
                RollbackException e) {
            throw new RuntimeException(e);
        }
    }

    /** Removes BSP Lab batches BP-71077 and BP-71083 along with their MercurySamples and BarcodedTubes. */
    @Test(enabled = false)
    public void fixupGplim3764() throws Exception {
        userBean.loginOSUser();
        userTransaction.begin();

        List<LabBatch> batches = labBatchDao.findByListIdentifier(Arrays.asList(new String[]{"BP-71077", "BP-71083"}));
        Assert.assertEquals(batches.size(), 2);
        final int vesselCount = 192;

        List<LabBatchStartingVessel> labBatchStartingVessels = labBatchDao.findListByList(LabBatchStartingVessel.class,
                LabBatchStartingVessel_.labBatch, batches);
        Assert.assertEquals(labBatchStartingVessels.size(), vesselCount);

        Set<LabVessel> labVessels = new HashSet<>();
        Set<MercurySample> mercurySamples = new HashSet<>();
        for (LabBatchStartingVessel labBatchStartingVessel : labBatchStartingVessels) {
            labVessels.add(labBatchStartingVessel.getLabVessel());
            mercurySamples.addAll(labBatchStartingVessel.getLabVessel().getMercurySamples());
        }
        Assert.assertEquals(labVessels.size(), vesselCount);
        Assert.assertEquals(mercurySamples.size(), vesselCount);

        for (MercurySample mercurySample : mercurySamples) {
            labBatchDao.remove(mercurySample);
        }
        for (LabVessel vessel : labVessels) {
            // This also removes the labBatchStartingVessel.
            labBatchDao.remove(vessel);
        }
        for (LabBatch labBatch : batches) {
            System.out.println("Deleted batch " + labBatch.getBatchName());
            labBatchDao.remove(labBatch);
        }
        labBatchDao.persist(new FixupCommentary("GPLIM-3764 delete batches, vessels, and samples created by BSP test"));
        labBatchDao.flush();
        userTransaction.commit();
    }


}
