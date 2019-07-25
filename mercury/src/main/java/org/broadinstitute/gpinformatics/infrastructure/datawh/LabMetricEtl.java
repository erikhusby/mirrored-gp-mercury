package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabMetricRunDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.*;

/**
 * Etls CRUD edits to lab_metrics table and also produces associated lab_metric_metadata entries <br/>
 * Because metadata is always attached via the lab_metrics enbtity, will capture all metadata changes
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class LabMetricEtl extends GenericEntityEtl<LabMetric, LabMetric> {
    private final Map<String,List<Long>> loggerMessages = new HashMap<>();
    private final String nullVesselKey = "No lab vessel for metric";
    private final String vesselNotTubeKey = "Metric vessel is not a tube";
    public final String metadataFileName = "lab_metric_metadata";

    private BSPUserList userList;

    public LabMetricEtl() {
        initLogging();
    }

    @Inject
    public LabMetricEtl(LabMetricRunDao labMetricDao, BSPUserList userList) {
        super(LabMetric.class, "lab_metric", "lab_metric_aud", "lab_metric_id", labMetricDao);
        this.userList = userList;
        initLogging();
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

    /**
     * Overridden to gather LabEvent objects for entity ids and pass to writer so the file writes are forked
     * into event or ancestry file <br/>
     * Scope relaxed from protected to public to allow a backfill service hook
     */
    @Override
    public int writeRecords(Collection<Long> deletedEntityIds,
                            Collection<Long> modifiedEntityIds,
                            Collection<Long> addedEntityIds,
                            Collection<RevInfoPair<LabMetric>> revInfoPairs,
                            String etlDateStr) {

        Collection<Long> nonDeletedIds = new ArrayList<>();
        nonDeletedIds.addAll(modifiedEntityIds);
        nonDeletedIds.addAll(addedEntityIds);

        Collection<LabMetric> metricList = new ArrayList<>();
        LabMetric metric;
        for (Long entityId : nonDeletedIds) {
            metric = dao.findById( LabMetric.class, entityId );
            if( metric != null ) {
                metricList.add(metric);
            }
        }
        return writeRecords( metricList, deletedEntityIds, etlDateStr );
    }

    /**
     * Writes the lab_metric sqlLoader data file records per standard processing, then writes separate lab_metric_metadata file <br/>
     * Scope relaxed from protected to public to allow a backfill service hook
     */
    @DaoFree
    @Override
    public int writeRecords(Collection<LabMetric> entities,
                            Collection<Long>deletedEntityIds,
                            String etlDateStr) {
        int count = 0;
        DataFile metaDataFile = new DataFile(dataFilename(etlDateStr, metadataFileName));
        try {
            count += super.writeRecords(entities, deletedEntityIds, etlDateStr);

            // Writes the metadata records.
            for (LabMetric entity : entities) {
                if (!deletedEntityIds.contains(dataSourceEntityId(entity))) {
                    try {
                        for(Metadata meta : entity.getMetadataSet() ) {
                            String record = genericRecord(etlDateStr, false,
                                    entity.getLabMetricId(),
                                    meta.getId(),
                                    format( meta.getKey().name() ),
                                    format( meta.getStringValue() ),
                                    format( meta.getDateValue() ),
                                    format( meta.getNumberValue() )
                            );
                            metaDataFile.write(record);
                        }
                    } catch (Exception e) {
                        // Continues ETL and logs data-specific Mercury exceptions.  Re-throws systemic exceptions
                        // such as when BSP is down in order to stop this run of ETL.
                        if (!isSystemException(e)) {
                            if (errorException == null) {
                                errorException = e;
                            }
                            errorIds.add(dataSourceEntityId(entity));
                        } else {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error while writing " + metaDataFile.getFilename(), e);

        } finally {
            metaDataFile.close();
        }
        return count + metaDataFile.getRecordCount();
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, LabMetric entity) {
        Collection<String> records = new ArrayList<>();

        try {

            // No vessel test
            if (entity == null || entity.getLabVessel() == null ) {
                loggerMessages.get(nullVesselKey).add(entity != null ? entity.getLabMetricId() : 0L);
                return records;
            }

            // Tube test
            if (entity.getLabVessel().getType() != LabVessel.ContainerType.TUBE ) {
                loggerMessages.get(vesselNotTubeKey).add(entity.getLabMetricId());
                return records;
            }

            LabVessel labVessel = entity.getLabVessel();
            LabMetricRun run = entity.getLabMetricRun();

            String decider = null;
            if( entity.getLabMetricDecision() != null ) {
                Long userId = entity.getLabMetricDecision().getDeciderUserId();
                decider = this.userList.getUserFullName(userId);
            }

            records.add(
                    genericRecord(etlDateStr, isDelete,
                            entity.getLabMetricId(),
                            format(entity.getName().name()),
                            format(entity.getUnits().name()),
                            format(entity.getValue()),
                            format(run != null ? run.getRunName() : null),
                            format(run != null ? run.getRunDate() : entity.getCreatedDate()),
                            format(labVessel != null ? labVessel.getLabVesselId() : null),
                            format(labVessel != null ? labVessel.getLabel() : null),
                            format(entity.getVesselPosition()),
                            format(entity.getLabMetricDecision() != null ? entity.getLabMetricDecision().getDecision().toString() : null),
                            format(entity.getLabMetricDecision() != null ? entity.getLabMetricDecision().getDecidedDate() : null),
                            format(decider),
                            format(entity.getLabMetricDecision() != null ? entity.getLabMetricDecision().getOverrideReason() : null)
                    )
            );

        } catch(Exception e) {
            // Uncaught RuntimeExceptions kill the injected LabMetricEtl in ExtractTransform.
            logger.error("Error doing lab metric etl", e);
        }
        return records;
    }

    @Override
    public void postEtlLogging() {
        super.postEtlLogging();
        for (Map.Entry<String,List<Long>> logEntries : loggerMessages.entrySet()) {
            if( logEntries.getValue().size() > 0 ) {
                // TODO JMS List of ids might get very long during backfills, possibly break up?
                logger.debug(logEntries.getKey() + ": " + StringUtils.join(logEntries.getValue(), ','));
            }
        }
        loggerMessages.clear();
    }

    private void initLogging() {
        loggerMessages.put(nullVesselKey, new ArrayList<Long>());
        loggerMessages.put(vesselNotTubeKey, new ArrayList<Long>());
    }
}
