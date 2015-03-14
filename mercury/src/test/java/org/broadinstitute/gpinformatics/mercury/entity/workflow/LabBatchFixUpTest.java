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
import org.broadinstitute.gpinformatics.infrastructure.common.BaseSplitter;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.ReworkDetail;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * A Test to backpopulate a column which ought to be not null.
 */
@Test(groups = TestGroups.FIXUP)
public class LabBatchFixUpTest extends Arquillian {

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private ProductOrderDao productOrderDao;

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

    private static class TubeBatch {
        private final String labBatch;
        private final String barcode;

        private TubeBatch(String labBatch, String barcode) {
            this.labBatch = labBatch;
            this.barcode = barcode;
        }

        public String getLabBatch() {
            return labBatch;
        }

        public String getBarcode() {
            return barcode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            TubeBatch tubeBatch = (TubeBatch) obj;

            if (!barcode.equals(tubeBatch.barcode)) {
                return false;
            }
            if (!labBatch.equals(tubeBatch.labBatch)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = labBatch.hashCode();
            result = 31 * result + barcode.hashCode();
            return result;
        }
    }

    /**
     * Back populate controls into LCSETs.
     */
    @Test(enabled = true)
    public void fixupGplim3442() {
        Set<TubeBatch> tubeBatches = new HashSet<>();

        List<LabBatch> workflowLabBatches = labBatchDao.findByType(LabBatch.LabBatchType.WORKFLOW);
        List<String> batchNames = new ArrayList<>();
        for (LabBatch workflowLabBatch : workflowLabBatches) {
            batchNames.add(workflowLabBatch.getBatchName());
        }
        labBatchDao.clear();
        List<Collection<String>> listListBatchNames = BaseSplitter.split(batchNames, 20);
        for (Collection<String> listBatchName : listListBatchNames) {
            workflowLabBatches = labBatchDao.findByListIdentifier(new ArrayList<>(listBatchName));
            for (LabBatch workflowLabBatch : workflowLabBatches) {
                boolean found = false;
                for (BucketEntry bucketEntry : workflowLabBatch.getBucketEntries()) {
                    Map<LabEvent, Set<LabVessel>> vesselsForLabEventTypes =
                            bucketEntry.getLabVessel().findVesselsForLabEventType(LabEventType.SHEARING_TRANSFER, false);
                    for (Set<LabVessel> labVessels : vesselsForLabEventTypes.values()) {
                        for (LabVessel labVessel : labVessels) {
                            if (OrmUtil.proxySafeIsInstance(labVessel, BarcodedTube.class)) {
                                found = processTube(tubeBatches, workflowLabBatch, found, (BarcodedTube) labVessel);
                            } else if (OrmUtil.proxySafeIsInstance(labVessel, TubeFormation.class)) {
                                for (LabVessel containedVessel : labVessel.getContainerRole().getContainedVessels()) {
                                    found = processTube(tubeBatches, workflowLabBatch, found, (BarcodedTube) containedVessel);
                                }
                            }
                        }
                    }
                    if (!vesselsForLabEventTypes.isEmpty()) {
                        break;
                    }
                }
                if (!found) {
                    System.out.println("Failed to find any controls for " + workflowLabBatch.getBatchName());
                }
            }
            System.out.println("Flush and clear");
            labBatchDao.flush();
            labBatchDao.clear();
        }
    }

    private boolean processTube(Set<TubeBatch> tubeBatches, LabBatch workflowLabBatch, boolean found, BarcodedTube labVessel) {
        BarcodedTube barcodedTube = labVessel;
        Set<SampleInstanceV2> sampleInstances = barcodedTube.getSampleInstancesV2();
        if (sampleInstances.size() == 1) {
            SampleInstanceV2 sampleInstance = sampleInstances.iterator().next();
            if (sampleInstance.getAllBucketEntries().isEmpty()) {
                if (tubeBatches.add(new TubeBatch(
                        workflowLabBatch.getBatchName(), barcodedTube.getLabel()))) {
                    found = true;
                    System.out.println("Adding " + barcodedTube.getLabel() + " to " +
                            workflowLabBatch.getBatchName() + " " +
                            sampleInstance.getEarliestMercurySampleName());
//                            workflowLabBatch.addLabVessel(barcodedTube);
                }
            }
        }
        return found;
    }
}
