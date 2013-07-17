package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabMetricRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Stateful
public class LabMetricEtl extends GenericEntityEtl<LabMetric, LabMetric> {
    private LabMetricRunDao labMetricDao;
    private final Set<Long> nullVesselLabMetricIds = Collections.synchronizedSet(new HashSet<Long>());
    private final Set<String> loggerMessages = Collections.synchronizedSet(new HashSet<String>());
    public final static Map<LabMetric.MetricType, LabEventType> mapMetricTypeToEventType = new HashMap<>();

    public LabMetricEtl() {
    }

    @Inject
    public LabMetricEtl(LabMetricRunDao labMetricDao) {
        super(LabMetric.class, "lab_metric", labMetricDao);
        this.labMetricDao = labMetricDao;
    }

    static {
        // Creates a map of LabMetric.metricType to LabEvent.eventType.
        for (LabMetric.MetricType metricType : LabMetric.MetricType.values()) {
            for (LabEventType labEventType : LabEventType.values()) {
                if (labEventType.name().equals(metricType.name())) {
                    mapMetricTypeToEventType.put(metricType,labEventType);
                    break;
                }
            }
        }
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
            if (entity != null) {
                LabVessel labVessel = entity.getLabVessel();
                if (labVessel != null) {
                    LabMetricRun run = entity.getLabMetricRun();
                    LabEventType targetEventType = mapMetricTypeToEventType.get(entity.getName());
                    if (targetEventType == null) {
                        loggerMessages.add("Cannot find LabEventType that matches LabMetricType " +
                                           entity.getName().name());
                    } else {
                        // Finds the pico event to link to.  That event will be on a direct descendant (plate) of
                        // the LabMetric's vessel (tube).  If multiple events exist, links to the latest event.
                        LabEvent picoEvent = null;
                        for (LabEvent transferFromEvent : labVessel.getTransfersFrom()) {
                            for (LabVessel targetVessel : transferFromEvent.getTargetLabVessels()) {
                                for (LabEvent targetVesselEvent : targetVessel.getEvents()) {
                                    if (targetVesselEvent.getLabEventType().equals(targetEventType)) {
                                        if ((picoEvent == null
                                            || picoEvent.getEventDate().before(targetVesselEvent.getEventDate()))) {
                                        picoEvent = targetVesselEvent;
                                        }
                                    }
                                }
                            }
                        }


                        records.add(genericRecord(etlDateStr, isDelete,
                                entity.getLabMetricId(),
                                format(labVessel != null ? labVessel.getLabVesselId() : null),
                                format(picoEvent != null ? picoEvent.getLabEventId() : null),
                                format(entity.getName().name()),
                                format(entity.getUnits().name()),
                                format(entity.getValue()),
                                format(run != null ? run.getRunName() : null),
                                format(run != null ? run.getRunDate() : null)));
                    }
                } else {
                    nullVesselLabMetricIds.add(entity.getLabMetricId());
                }
            }
        } catch (Exception e) {
            // Uncaught RuntimeExceptions kill the injected LabMetricEtl in ExtractTransform.
            logger.error("Error doing lab metric etl", e);
        }
        return records;
    }


    @Override
    public void postEtlLogging() {
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
