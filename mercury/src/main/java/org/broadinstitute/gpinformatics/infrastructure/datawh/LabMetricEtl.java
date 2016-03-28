package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabMetricRunDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Stateful
public class LabMetricEtl extends GenericEntityEtl<LabMetric, LabMetric> {
    private LabMetricRunDao labMetricDao;
    private ProductOrderDao pdoDao;
    private final Set<Long> nullVesselLabMetricIds = Collections.synchronizedSet(new HashSet<Long>());
    private final Set<String> loggerMessages = Collections.synchronizedSet(new HashSet<String>());

    public LabMetricEtl() {
    }

    @Inject
    public LabMetricEtl(LabMetricRunDao labMetricDao, ProductOrderDao pdoDao) {
        super(LabMetric.class, "lab_metric", "lab_metric_aud", "lab_metric_id", labMetricDao);
        this.labMetricDao = labMetricDao;
        this.pdoDao = pdoDao;
    }

    @Override
    Long entityId(LabMetric entity) {
        return entity.getLabMetricId();
    }

    @Override
    Path rootId(Root<LabMetric> root) {
        return root.get(LabMetric_.labMetricId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(LabMetric.class, entityId));
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, LabMetric entity) {
        throw new RuntimeException("This method cannot apply to this etl class.");
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, LabMetric entity) {
        Collection<String> records = new ArrayList<>();
        try {
            if (entity != null && entity.getLabVessel() != null) {
                LabVessel labVessel = entity.getLabVessel();
                LabMetricRun run = entity.getLabMetricRun();

                Set<SampleInstanceV2> sampleInstances = labVessel.getSampleInstancesV2();

                if (!sampleInstances.isEmpty()) {
                    for (SampleInstanceV2 si : sampleInstances) {
                        MercurySample sample = si.getRootOrEarliestMercurySample();
                        if (sample != null) {
                            Set<ProductOrderSample> pdoSamples = sample.getProductOrderSamples();
                            ProductOrder pdo = (pdoSamples.size() == 1) ? pdoSamples.iterator().next().getProductOrder() : null;

                            String batchName;
                            LabBatch labBatch = si.getSingleBatch();
                            if( labBatch != null ) {
                                batchName = labBatch.getBatchName();
                            } else {
                                // If a single batch can't be inferred, show them all
                                List<LabBatch> labBatches = si.getAllWorkflowBatches();
                                if( labBatches.size() == 0 ) {
                                    batchName = LabEventEtl.NONE;
                                } else {
                                    batchName = LabEventEtl.MULTIPLE;
                                    // Don't show multiples
                                    //StringBuilder batchNames = new StringBuilder();
                                    //for( LabBatch labBatchIter : labBatches ){
                                    //    batchNames.append(labBatchIter.getBatchName() ).append(", ");
                                    //}
                                    //batchName = batchNames.substring(0, batchNames.length() - 2 );
                                }
                            }

                            records.add(genericRecord(etlDateStr, isDelete,
                                    entity.getLabMetricId(),
                                    format(sample.getSampleKey()),
                                    format(labVessel != null ? labVessel.getLabVesselId() : null),
                                    format(pdo != null ? pdo.getProductOrderId() : null),
                                    format(batchName),
                                    format(entity.getName().name()),
                                    format(entity.getUnits().name()),
                                    format(entity.getValue()),
                                    format(run != null ? run.getRunName() : null),
                                    format(run != null ? run.getRunDate() : entity.getCreatedDate()),
                                    format(entity.getVesselPosition())));
                        }
                    }
                }
            } else {
                nullVesselLabMetricIds.add(entity != null ? entity.getLabMetricId() : 0);
            }
        } catch(Exception e) {
            // Uncaught RuntimeExceptions kill the injected LabMetricEtl in ExtractTransform.
            logger.error("Error doing lab metric etl", e);
        }
        return records;
    }


    @Override
    public void postEtlLogging() {
        super.postEtlLogging();

        synchronized (nullVesselLabMetricIds) {
            if (nullVesselLabMetricIds.size() > 0) {
                logger.debug("Missing vessel for labMetrics: " + StringUtils.join(nullVesselLabMetricIds, ", "));
            }
            nullVesselLabMetricIds.clear();
        }
        synchronized (loggerMessages) {
            for (String msg : loggerMessages) {
                logger.debug(msg);
            }
            loggerMessages.clear();
        }
    }
}
