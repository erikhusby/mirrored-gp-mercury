package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.collections15.map.LRUMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel.SampleType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

@Stateful
public class SequencingSampleFactEtl extends GenericEntityEtl<SequencingRun, SequencingRun> {
    private ProductOrderDao pdoDao;
    private Collection<SequencingRunDto> loggingDtos = new ArrayList<>();
    public static final String NONE = "NONE";
    public static final String MULTIPLE = "MULTIPLE";

    public SequencingSampleFactEtl() {
    }

    @Inject
    public SequencingSampleFactEtl(IlluminaSequencingRunDao dao, ProductOrderDao pdoDao) {
        super(SequencingRun.class, "sequencing_sample_fact", dao);
        this.pdoDao = pdoDao;
    }

    @Override
    Long entityId(SequencingRun entity) {
        return entity.getSequencingRunId();
    }

    @Override
    Path rootId(Root<SequencingRun> root) {
        return root.get(SequencingRun_.sequencingRunId);
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
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, SequencingRun entity) {
        Collection<String> records = new ArrayList<String>();
        try {
            Collection<SequencingRunDto> dtos = makeSequencingRunDtos(entity);
            for (SequencingRunDto dto : dtos) {
                if (dto.canEtl()) {
                    // Turns "LANE1" to "LANE8" into "1" to "8".
                    String position = dto.getPosition().replaceAll("LANE", "");

                    records.add(genericRecord(etlDateStr, isDelete,
                            entity.getSequencingRunId(),
                            format(dto.getFlowcellBarcode()),
                            format(position),
                            format(dto.getMolecularIndexingSchemeName()),
                            format(dto.getProductOrderId()),
                            format(dto.getSampleKey()),
                            format(dto.getResearchProjectId()),
                            (dto.getLoadingVessel() != null) ? format(dto.getLoadingVessel().getLabel()) : null,
                            (dto.getLoadingVessel() != null) ? format(ExtractTransform.secTimestampFormat
                                    .format(dto.getLoadingVessel().getCreatedOn())) : null,
                            format(dto.getBatchName())
                    ));
                }
            }
        } catch (Exception e) {
            // Uncaught RuntimeExceptions kill the injected SequencingSampleFactEtl in ExtractTransform.
            logger.error("Error doing ETL of sequencingRun", e);
        }

        return records;
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
            this.molecularIndexingSchemeName = molecularIndexingSchemeName;
            this.productOrderId = productOrderId;
            this.sampleKey = sampleKey;
            this.researchProjectId = researchProjectId;
            this.canEtl = canEtl;
            this.loadingVessel = loadingVessel;
            this.batchName = batchName;
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

    public List<SequencingRunDto> makeSequencingRunDtos(SequencingRun entity) {
        List<SequencingRunDto> dtos = new ArrayList<>();
        if (entity != null) {
                RunCartridge cartridge = entity.getSampleCartridge();
                String flowcellBarcode = cartridge.getCartridgeName();

                Map<VesselPosition, LabVessel> vesselsWithPositions = cartridge.getNearestTubeAncestorsForLanes();
                VesselPosition[] vesselPositions = cartridge.getVesselGeometry().getVesselPositions();
                for (VesselPosition position : vesselPositions) {
                    Collection<SampleInstance> sampleInstances =
                            cartridge.getSamplesAtPosition(position, SampleType.WITH_PDO);
                    for (SampleInstance si : sampleInstances) {
                        String productOrderId = null;
                        String researchProjectId = null;
                        Collection<LabBatch> batches = si.getAllWorkflowLabBatches();
                        String batchName = batches.size() == 0 ? NONE :
                                batches.size() == 1 ? batches.iterator().next().getBatchName() : MULTIPLE;
                        String pdoKey = si.getProductOrderKey();
                        if (pdoKey != null) {
                            // Does cache lookup and fills cache as needed.
                            synchronized (pdoKeyToPdoId) {
                                if (pdoKeyToPdoId.containsKey(pdoKey)) {
                                    productOrderId = pdoKeyToPdoId.get(pdoKey);
                                    researchProjectId = pdoKeyToResearchProjectId.get(pdoKey);
                                } else {
                                    ProductOrder pdo = pdoDao.findByBusinessKey(pdoKey);
                                    if (pdo != null) {
                                        productOrderId = String.valueOf(pdo.getProductOrderId());
                                        researchProjectId = (pdo.getResearchProject() != null) ?
                                                String.valueOf(pdo.getResearchProject().getResearchProjectId()) : null;
                                    }
                                    pdoKeyToPdoId.put(pdoKey, productOrderId);
                                    pdoKeyToResearchProjectId.put(pdoKey, researchProjectId);
                                }
                            }
                        }

                        MercurySample sample = si.getStartingSample();
                        String sampleKey = (sample != null ? sample.getSampleKey() : null);

                        // Finds the molecular barcode, or "NONE" if not found, or a sorted comma-delimited
                        // concatenation if multiple are found.
                        SortedSet<String> names = new TreeSet<>();
                        for (Reagent reagent : si.getReagents()) {
                            if (OrmUtil.proxySafeIsInstance(reagent, MolecularIndexReagent.class)) {
                                names.add(((MolecularIndexReagent) reagent).getMolecularIndexingScheme().getName());
                            }
                        }
                        String molecularIndexingSchemeName =
                                (names.size() == 0 ? "NONE" : StringUtils.join(names, " "));

                        boolean canEtl = !StringUtils.isBlank(flowcellBarcode)
                                         && !StringUtils.isBlank(productOrderId)
                                         && !StringUtils.isBlank(researchProjectId);

                        dtos.add(new SequencingRunDto(entity, flowcellBarcode, position.name(),
                                molecularIndexingSchemeName, productOrderId, sampleKey, researchProjectId, canEtl,
                                vesselsWithPositions.get(position), batchName));
                    }
                    if (sampleInstances.size() == 0) {
                        dtos.add(new SequencingRunDto(entity, flowcellBarcode, null, null, null, null, null, false,
                                vesselsWithPositions.get(position), null));
                    }
                }
        } else {
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
