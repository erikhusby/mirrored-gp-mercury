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

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * A Test to backpopulate a column which ought to be not null.
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class LabBatchFixUpTest extends Arquillian {
    @Inject
    private LabBatchDao labBatchDao;
    @Inject
    private UserTransaction utx;

    @BeforeMethod
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.setTransactionTimeout(3000);
        utx.begin();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.commit();
    }

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
    @Test(enabled = true)
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

}
