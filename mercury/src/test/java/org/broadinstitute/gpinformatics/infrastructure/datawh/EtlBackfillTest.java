package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Contains tests that identify starting points for ETL backfills.
 */
public class EtlBackfillTest extends Arquillian {

    @Inject
    private ProductOrderDao productOrderDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    /**
     * Finds each shearing rack in a given PDO, and generates a command to backfill it.
     */
    @Test(enabled = false)
    public void testFindVesselsForPdo() {
        String productOrderKey = "PDO-17756"; // PDO-17459, PDO-17756, PDO-17852
        ProductOrder productOrder = productOrderDao.findByBusinessKey(productOrderKey);
        TransferTraverserCriteria.VesselForEventTypeCriteria transferTraverserCriteria =
                new TransferTraverserCriteria.VesselForEventTypeCriteria(
                        Collections.singletonList(LabEventType.SHEARING_TRANSFER), false, true);
        for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
            for (LabVessel labVessel : productOrderSample.getMercurySample().getLabVessel()) {
                labVessel.evaluateCriteria(transferTraverserCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
            }
        }
        List<Pair<String, String>> labBatchRackPairs = new ArrayList<>();
        for (Map.Entry<LabEvent, Set<LabVessel>> labEventSetEntry : transferTraverserCriteria.getVesselsForLabEventType().entrySet()) {
            LabEvent labEvent = labEventSetEntry.getKey();
            Set<LabBatch> computedLcSets = labEvent.getComputedLcSets();
            if (computedLcSets.size() == 1) {
                LabBatch labBatch = computedLcSets.iterator().next();
                for (BucketEntry bucketEntry : labBatch.getBucketEntries()) {
                    if (bucketEntry.getProductOrder().getBusinessKey().equals(productOrderKey)) {
                        labBatchRackPairs.add(new ImmutablePair<>(labBatch.getBusinessKey(),
                                labEvent.getSourceLabVessels().iterator().next().getLabel()));
                        break;
                    }
                }
            } else {
                System.out.println("Failed to find single LCSET for " + labEvent.getLabEventId());
            }
        }
        labBatchRackPairs.sort(Comparator.comparing(Pair::getKey));
        labBatchRackPairs.forEach(stringStringPair -> System.out.println(
                "curl -X PUT https://mercury.broadinstitute.org:8443/Mercury/rest/etl/backfillByVessel/" +
                        stringStringPair.getValue() + " # " + stringStringPair.getKey()));
    }
}
