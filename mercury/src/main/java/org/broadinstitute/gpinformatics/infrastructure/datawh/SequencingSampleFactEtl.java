package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class SequencingSampleFactEtl extends GenericEntityEtl<SequencingRun, SequencingRun> {
    private Collection<SequencingRunDto> loggingDtos = new ArrayList<>();
    public static final String NONE = "NONE";
    public static final String MULTIPLE = "MULTIPLE";

    public SequencingSampleFactEtl() {
    }

    @Inject
    public SequencingSampleFactEtl(IlluminaSequencingRunDao dao) {
        super(SequencingRun.class, "sequencing_sample_fact", "sequencing_run_aud", "sequencing_run_id", dao);
    }

    @Override
    Long entityId(SequencingRun entity) {
        return entity.getSequencingRunId();
    }

    @Override
    Path rootId(Root<SequencingRun> root) {
        return root.get(SequencingRun_.sequencingRunId);
    }

    /**
     * Scope relaxed from protected to public to allow a backfill service hook
     */
    @Override
    public int writeRecords(Collection<SequencingRun> entities,
                            Collection<Long>deletedEntityIds,
                            String etlDateStr) throws Exception {
        return super.writeRecords(entities, deletedEntityIds, etlDateStr);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(SequencingRun.class, entityId));
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, SequencingRun entity) {
        throw new RuntimeException("This method cannot apply to this etl class.");
    }

    @Override
    public Collection<String> dataRecords(String etlDateStr, boolean isDelete, SequencingRun entity) {
        try {
            return dataRecords(etlDateStr, isDelete, entity.getSequencingRunId(), makeSequencingRunDtos(entity));
        } catch (Exception e) {
            // Uncaught RuntimeExceptions kill the injected SequencingSampleFactEtl in ExtractTransform.
            logger.error("Error doing ETL of sequencingRun", e);
            return Collections.emptyList();
        }
    }

    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long sequencingRunId,
                                   Collection<SequencingRunDto> dtos) {
        Collection<String> records = new ArrayList<>();

        // De-duplicate based on flowcell barcode, lane, and molecular index (GPLIM-1618).
        Map<String, SequencingRunDto> uniqueFlowcellLaneIndex = new HashMap<>();
        for (SequencingRunDto dto : dtos) {
            uniqueFlowcellLaneIndex.put(dto.getFlowcellLaneIndexKey(), dto);
        }
        for (SequencingRunDto dto : uniqueFlowcellLaneIndex.values()) {
            if (dto.canEtl()) {
                // Turns "LANE1" to "LANE8" into "1" to "8".
                String position = dto.getPosition().replaceAll("LANE", "");

                LabVessel loadingVessel = dto.getLoadingVessel();
                records.add(genericRecord(etlDateStr, isDelete,
                        sequencingRunId,
                        format(dto.getFlowcellBarcode()),
                        format(position),
                        format(dto.getMolecularIndexingSchemeName()),
                        format(dto.getProductOrderId()),
                        format(dto.getSampleKey()),
                        format(dto.getResearchProjectId()),
                        (loadingVessel != null) ? format(loadingVessel.getLabel()) : null,
                        (loadingVessel != null) ?
                                format(ExtractTransform.formatTimestamp(loadingVessel.getCreatedOn())) : null,
                        format(dto.getBatchName())
                ));
            }
        }
        return records;
    }

    public void writeEtlDataFileWrapper(List<Long> deleteIds, Set<Long> modifyIds, List<Long> addIds,
            List<RevInfoPair<SequencingRun>> revInfoPairs, String etlDateStr) throws Exception {
        writeEtlDataFile(deleteIds, modifyIds, addIds, revInfoPairs, etlDateStr);
    }

    public static class SequencingRunDto {
        private SequencingRun sequencingRun;
        private String flowcellBarcode;
        private String position;
        private String molecularIndexingSchemeName;
        private String productOrderId;
        private String sampleKey;
        private String researchProjectId;
        private boolean canEtl;
        private String batchName;

        private LabVessel loadingVessel;

        SequencingRunDto(
                SequencingRun sequencingRun,
                String flowcellBarcode,
                String position,
                String molecularIndexingSchemeName,
                String productOrderId,
                String sampleKey,
                String researchProjectId,
                boolean canEtl,
                LabVessel loadingVessel,
                String batchName) {
            this.sequencingRun = sequencingRun;
            this.flowcellBarcode = flowcellBarcode;
            this.position = position;
            this.molecularIndexingSchemeName
                    = StringUtils.isBlank(molecularIndexingSchemeName)?NONE:molecularIndexingSchemeName;
            this.productOrderId = productOrderId;
            this.sampleKey = sampleKey;
            this.researchProjectId = researchProjectId;
            this.canEtl = canEtl;
            this.loadingVessel = loadingVessel;
            this.batchName = StringUtils.isBlank(batchName)?NONE:batchName;
        }

        public static final Comparator<SequencingRunDto> BY_SAMPLE_KEY = new Comparator<SequencingRunDto>() {
            // Sorting a null sample key will put it after non-null sample keys.
            private final static String NULLS_LAST = "zzzzzzzzzz";

            @Override
            public int compare(SequencingRunDto lhs, SequencingRunDto rhs) {
                String s1 = lhs.getSampleKey() == null ? NULLS_LAST : lhs.getSampleKey();
                String s2 = rhs.getSampleKey() == null ? NULLS_LAST : rhs.getSampleKey();
                return s1.compareTo(s2);
            }
        };

        public String getFlowcellLaneIndexKey() {
            final String delimiter = "_____";
            return StringUtils.join(new String[]{molecularIndexingSchemeName, position, flowcellBarcode}, delimiter);
        };

        public String getFlowcellBarcode() {
            return flowcellBarcode;
        }

        public String getPosition() {
            return position;
        }

        public String getMolecularIndexingSchemeName() {
            return molecularIndexingSchemeName;
        }

        public String getProductOrderId() {
            return productOrderId;
        }

        public String getSampleKey() {
            return sampleKey;
        }

        public String getResearchProjectId() {
            return researchProjectId;
        }

        public SequencingRun getSequencingRun() {
            return sequencingRun;
        }

        public boolean canEtl() {
            return canEtl;
        }

        public LabVessel getLoadingVessel() {
            return loadingVessel;
        }

        public String getBatchName() {
            return batchName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || !OrmUtil.proxySafeIsInstance(o, SequencingRunDto.class)) {
                return false;
            }

            SequencingRunDto that = OrmUtil.proxySafeCast(o, SequencingRunDto.class);

            return new EqualsBuilder().append(canEtl(), that.canEtl())
                    .append(getFlowcellBarcode(), that.getFlowcellBarcode())
                    .append(getMolecularIndexingSchemeName(), that.getMolecularIndexingSchemeName())
                    .append(getPosition(), that.getPosition())
                    .append(getProductOrderId(), that.getProductOrderId())
                    .append(getResearchProjectId(), that.getResearchProjectId())
                    .append(getSampleKey(), that.getSampleKey())
                    .append(getSequencingRun(), that.getSequencingRun())
                    .append(getLoadingVessel(), that.getLoadingVessel())
                    .append(getBatchName(), that.getBatchName())
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(canEtl())
                    .append(getProductOrderId())
                    .append(getSampleKey())
                    .append(getResearchProjectId())
                    .append(getFlowcellBarcode())
                    .append(getPosition())
                    .append(getMolecularIndexingSchemeName())
                    .append(getSequencingRun())
                    .append(getLoadingVessel())
                    .append(getBatchName())
                    .toHashCode();
        }
    }


    // Cache lookups for efficiency.
    private static final int CACHE_SIZE = 16;
    private static final Map<String, String> pdoKeyToPdoId = new LRUMap<>(CACHE_SIZE);
    private static final Map<String, String> pdoKeyToResearchProjectId = new LRUMap<>(CACHE_SIZE);

    public List<SequencingRunDto> makeSequencingRunDtos(long sequencingRunId) {
        try {
            SequencingRun sequencingRun = dao.findById(SequencingRun.class, sequencingRunId);
            return makeSequencingRunDtos(sequencingRun);
        } catch (Exception e) {
            // Uncaught RuntimeExceptions kill the injected SequencingSampleFactEtl in ExtractTransform.
            logger.error("Error doing ETL sequencingRunId " + sequencingRunId, e);
            return Collections.<SequencingRunDto>emptyList();
        }
    }

    /** This is used by diagnostics (ExtractTransformResource class) so do not suppress error data in this method. */
    public List<SequencingRunDto> makeSequencingRunDtos(SequencingRun entity) {
        List<SequencingRunDto> dtos = new ArrayList<>();
        if (entity != null) {
            RunCartridge cartridge = entity.getSampleCartridge();
            String flowcellBarcode = cartridge.getCartridgeName();
            Map<VesselPosition, LabVessel> lanesAndLoadingTubes = cartridge.getNearestTubeAncestorsForLanes();
            for (Map.Entry<VesselPosition, LabVessel> entry : lanesAndLoadingTubes.entrySet()) {
                VesselPosition lane = entry.getKey();
                LabVessel loadingTube = entry.getValue();
                if (loadingTube != null) {
                    Collection<SampleInstanceV2> sampleInstances = loadingTube.getSampleInstancesV2();
                    for (SampleInstanceV2 si : sampleInstances) {
                        LabBatchStartingVessel labBatchStartingVessel =
                                si.getSingleBatchVessel(LabBatch.LabBatchType.FCT);
                        LabVessel fctVessel = labBatchStartingVessel != null ?
                                labBatchStartingVessel.getLabVessel() : loadingTube;

                        // Extract ETL data from sample instance
                        SampleInstanceEtlData sampleInstanceEtlData = SampleInstanceEtlData.buildFromSampleInstance
                                ( si, entity.getRunDate(), false );

                        String pdoSampleKey = sampleInstanceEtlData.getPdoSampleId();
                        ProductOrder pdo = sampleInstanceEtlData.getPdo();
                        LabBatch labBatch = sampleInstanceEtlData.getLabBatch();
                        String molecularIndexingSchemeName = sampleInstanceEtlData.getMolecularIndexingSchemeName();

                        String productOrderId = pdo == null ? null : String.valueOf(pdo.getProductOrderId());
                        String batchName = labBatch != null ? labBatch.getBatchName() : null;


                        String researchProjectId = (pdo != null && pdo.getResearchProject() != null) ?
                                String.valueOf(pdo.getResearchProject().getResearchProjectId()) : null;

                        boolean canEtl = !StringUtils.isBlank(flowcellBarcode) && !StringUtils.isBlank(productOrderId)
                                         && !StringUtils.isBlank(researchProjectId);

                        dtos.add(new SequencingRunDto(entity, flowcellBarcode, lane.name(),
                                molecularIndexingSchemeName, productOrderId, pdoSampleKey, researchProjectId,
                                canEtl, fctVessel, batchName));
                    }
                    if (sampleInstances.size() == 0) {
                        // Use of the full constructor which in this case has multiple nulls is intentional
                        // since exactly which fields are null is used as indicator in postEtlLogging, and this
                        // pattern is used in other fact table etl that are exposed in ExtractTransformResource.
                        dtos.add(new SequencingRunDto(entity, flowcellBarcode, null, null, null, null, null, false,
                                loadingTube, null));
                    }
                }
            }
        } else {
            // Use of the full constructor which in this case has multiple nulls is intentional
            // since exactly which fields are null is used as indicator in postEtlLogging, and this
            // pattern is used in other fact table etl that are exposed in ExtractTransformResource.
            dtos.add(new SequencingRunDto(null, null, null, null, null, null, null, false, null, null));
        }
        Collections.sort(dtos, SequencingRunDto.BY_SAMPLE_KEY);

        synchronized (loggingDtos) {
            loggingDtos.clear();
            loggingDtos.addAll(dtos);
        }
        return dtos;
    }

    @Override
    public void postEtlLogging() {
        super.postEtlLogging();

        synchronized (loggingDtos) {
            // Aggregates errors by the appropriate record identifier, depending on what the missing value is.
            Set<Long> errorIds = new HashSet<>();

            // Keeps track of reported errors so we log an entity once, showing the most basic flaw.
            Set<SequencingRunDto> reportedErrors = new HashSet<>();

            // No event.
            int count = 0;
            for (SequencingRunDto dto : loggingDtos) {
                if (!dto.canEtl() && !reportedErrors.contains(dto) &&
                    dto.getSequencingRun() == null) {
                    reportedErrors.add(dto);
                    ++count;
                }
            }
            if (count > 0) {
                logger.debug("Cannot ETL, missing SequencingRun entity in " + count + " records.");
            }
            errorIds.clear();

            // No sampleInstance on vessel.
            for (SequencingRunDto dto : loggingDtos) {
                if (!dto.canEtl() && !reportedErrors.contains(dto) &&
                    StringUtils.isBlank(dto.getFlowcellBarcode())) {
                    reportedErrors.add(dto);
                    errorIds.add(dto.getSequencingRun().getSequencingRunId());
                }
            }
            if (errorIds.size() > 0) {
                logger.debug("Missing flowcell barcode in SequencingRun entities "
                             + StringUtils.join(errorIds, ", "));
            }
            errorIds.clear();

            // No position.
            for (SequencingRunDto dto : loggingDtos) {
                if (!dto.canEtl() && !reportedErrors.contains(dto) &&
                    StringUtils.isBlank(dto.getPosition())) {
                    reportedErrors.add(dto);
                    errorIds.add(dto.getSequencingRun().getSequencingRunId());
                }
            }
            if (errorIds.size() > 0) {
                logger.debug("No position for sample in SequencingRun entities " + StringUtils.join(errorIds, ", "));
            }
            errorIds.clear();

            // No sample instances or molecular barcodes.
            for (SequencingRunDto dto : loggingDtos) {
                if (!dto.canEtl() && !reportedErrors.contains(dto) &&
                    StringUtils.isBlank(dto.getMolecularIndexingSchemeName())) {
                    reportedErrors.add(dto);
                    errorIds.add(dto.getSequencingRun().getSequencingRunId());
                }
            }
            if (errorIds.size() > 0) {
                logger.debug("Missing sample instance or molecular barcode in SequencingRun entities "
                             + StringUtils.join(errorIds, ", "));
            }
            errorIds.clear();

            // No pdo on sampleInstance, or no pdo entity for pdoKey.
            for (SequencingRunDto dto : loggingDtos) {
                if (!dto.canEtl() && !reportedErrors.contains(dto) &&
                    StringUtils.isBlank(dto.getProductOrderId())) {
                    reportedErrors.add(dto);
                    errorIds.add(dto.getSequencingRun().getSequencingRunId());
                }
            }
            if (errorIds.size() > 0) {
                logger.debug("Missing product order in SequencingRun entities "
                             + StringUtils.join(errorIds, ", "));
            }
            errorIds.clear();

            // No starting sample.
            for (SequencingRunDto dto : loggingDtos) {
                if (!dto.canEtl() && !reportedErrors.contains(dto) &&
                    StringUtils.isBlank(dto.getSampleKey())) {
                    reportedErrors.add(dto);
                    errorIds.add(dto.getSequencingRun().getSequencingRunId());
                }
            }
            if (errorIds.size() > 0) {
                logger.debug("Missing starting sample in SequencingRun entities "
                             + StringUtils.join(errorIds, ", "));
            }
            errorIds.clear();

            for (SequencingRunDto dto : loggingDtos) {
                if (dto.getLoadingVessel() == null) {
                    errorIds.add(dto.getSequencingRun().getSequencingRunId());
                }
            }

            if (errorIds.size() > 0) {
                logger.debug("missing loading library in SequencingRun Entities"
                             + StringUtils.join(errorIds, ", "));
            }

            errorIds.clear();
        }
    }
}
