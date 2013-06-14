package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.collections15.map.LRUMap;
import org.apache.commons.lang3.StringUtils;
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
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel.SampleType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.*;

@Stateful
public class SequencingSampleFactEtl extends GenericEntityEtl<SequencingRun, SequencingRun> {
    private ProductOrderDao pdoDao;
    private Collection<SequencingRunDto> loggingDtos = new ArrayList<>();

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

        Collection<SequencingRunDto> dtos = makeSequencingRunDtos(entity);
        for (SequencingRunDto dto : dtos) {
            if (dto.isComplete()) {
                // Turns "LANE1" to "LANE8" into "1" to "8".
                String position = dto.getPosition().replaceAll("LANE", "");

                records.add(genericRecord(etlDateStr, isDelete,
                        entity.getSequencingRunId(),
                        format(dto.getFlowcellBarcode()),
                        format(position),
                        format(dto.getMolecularIndexingSchemeName()),
                        format(dto.getProductOrderId()),
                        format(dto.getSampleKey()),
                        format(dto.getResearchProjectId())
                ));
            }
        }

        return records;
    }

    public static class SequencingRunDto {
        SequencingRun sequencingRun;
        String flowcellBarcode;
        String position;
        String molecularIndexingSchemeName;
        String productOrderId;
        String sampleKey;
        String researchProjectId;
        boolean isComplete;

        SequencingRunDto(
                SequencingRun sequencingRun,
                String flowcellBarcode,
                String position,
                String molecularIndexingSchemeName,
                String productOrderId,
                String sampleKey,
                String researchProjectId,
                boolean isComplete
        ) {
            this.sequencingRun = sequencingRun;
            this.flowcellBarcode = flowcellBarcode;
            this.position = position;
            this.molecularIndexingSchemeName = molecularIndexingSchemeName;
            this.productOrderId = productOrderId;
            this.sampleKey = sampleKey;
            this.researchProjectId = researchProjectId;
            this.isComplete = isComplete;
        }

        private final static String NULLS_LAST = "zzzzzzzzzz";

        public static Comparator sampleKeyComparator() {
            return new Comparator<SequencingRunDto>() {
                @Override
                public int compare(SequencingRunDto o1, SequencingRunDto o2) {
                    String s1 = o1.getSampleKey() == null ? NULLS_LAST : o1.getSampleKey();
                    String s2 = o2.getSampleKey() == null ? NULLS_LAST : o2.getSampleKey();
                    return s1.compareTo(s2);
                }
            };
        }

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

        public boolean isComplete() {
            return isComplete;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SequencingRunDto that = (SequencingRunDto) o;

            if (isComplete != that.isComplete) return false;
            if (flowcellBarcode != null ? !flowcellBarcode.equals(that.flowcellBarcode) : that.flowcellBarcode != null)
                return false;
            if (molecularIndexingSchemeName != null ? !molecularIndexingSchemeName.equals(that.molecularIndexingSchemeName) : that.molecularIndexingSchemeName != null)
                return false;
            if (position != null ? !position.equals(that.position) : that.position != null) return false;
            if (productOrderId != null ? !productOrderId.equals(that.productOrderId) : that.productOrderId != null)
                return false;
            if (researchProjectId != null ? !researchProjectId.equals(that.researchProjectId) : that.researchProjectId != null)
                return false;
            if (sampleKey != null ? !sampleKey.equals(that.sampleKey) : that.sampleKey != null) return false;
            if (sequencingRun != null ? !sequencingRun.equals(that.sequencingRun) : that.sequencingRun != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            long result = (isComplete ? 1 : 0);
            result = 31 * result + (productOrderId != null ? productOrderId.hashCode() : 0);
            result = 31 * result + (sampleKey != null ? sampleKey.hashCode() : 0);
            result = 31 * result + (researchProjectId != null ? researchProjectId.hashCode() : 0);
            result = 31 * result + (flowcellBarcode != null ? flowcellBarcode.hashCode() : 0);
            result = 31 * result + (position != null ? position.hashCode() : 0);
            result = 31 * result + (molecularIndexingSchemeName != null ? molecularIndexingSchemeName.hashCode() : 0);
            result = 31 * result + (sequencingRun != null ? sequencingRun.getSequencingRunId() : 0);
            return (int)result;
        }
    }


    // Cache lookups for efficiency.
    private static final int CACHE_SIZE = 16;
    private static Map<String, String> pdoKeyToPdoId = new LRUMap<>(CACHE_SIZE);
    private static Map<String, String> pdoKeyToResearchProjectId = new LRUMap<>(CACHE_SIZE);

    public List<SequencingRunDto> makeSequencingRunDtos(long sequencingRunId) {
        SequencingRun sequencingRun = dao.findById(SequencingRun.class, sequencingRunId);
        return makeSequencingRunDtos(sequencingRun);
    }

    public List<SequencingRunDto> makeSequencingRunDtos(SequencingRun entity) {
        List<SequencingRunDto> dtos = new ArrayList<>();
        if (entity != null) {

            RunCartridge cartridge = entity.getSampleCartridge();
            String flowcellBarcode = cartridge.getCartridgeName();
            VesselPosition[] vesselPositions = cartridge.getVesselGeometry().getVesselPositions();
            for (VesselPosition position : vesselPositions) {
                Collection<SampleInstance> sampleInstances =
                        cartridge.getSamplesAtPosition(position, SampleType.WITH_PDO);
                for (SampleInstance si : sampleInstances) {
                    String productOrderId = null;
                    String researchProjectId = null;

                    String pdoKey = si.getProductOrderKey();
                    if (pdoKey != null) {
                        // Does cache lookup and fills cache as needed.
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

                    MercurySample sample = si.getStartingSample();
                    String sampleKey = (sample != null ? sample.getSampleKey() : null);

                    // Finds the molecular barcode, or "NONE" if not found, or a sorted comma-delimited concatenation
                    // if multiple are found.
                    SortedSet<String> names = new TreeSet<>();
                    for (Reagent reagent : si.getReagents()) {
                        if (OrmUtil.proxySafeIsInstance(reagent, MolecularIndexReagent.class)) {
                            names.add(((MolecularIndexReagent) reagent).getMolecularIndexingScheme().getName());
                        }
                    }
                    String molecularIndexingSchemeName = (names.size() == 0 ? "NONE" : StringUtils.join(names, " "));

                    boolean isComplete = !StringUtils.isBlank(flowcellBarcode)
                            && !StringUtils.isBlank(productOrderId)
                            && !StringUtils.isBlank(researchProjectId);

                    dtos.add(new SequencingRunDto(entity, flowcellBarcode, position.name(),
                            molecularIndexingSchemeName, productOrderId, sampleKey, researchProjectId, isComplete));
                }
                if (sampleInstances.size() == 0) {
                    dtos.add(new SequencingRunDto(entity, flowcellBarcode, null, null, null, null, null, false));
                }
            }
        } else {
            dtos.add(new SequencingRunDto(null, null, null, null, null, null, null, false));
        }
        Collections.sort(dtos, SequencingRunDto.sampleKeyComparator());

        synchronized (loggingDtos) {
            loggingDtos.clear();
            loggingDtos.addAll(dtos);
        }
        return dtos;
    }

    @Override
    public void postEtlLogging() {
        synchronized(loggingDtos) {
            // Aggregates errors by the appropriate record identifier, depending on what the missing value is.
            Set<Long> errorIds = new HashSet<>();

            // Keeps track of reported errors so we log an entity once, showing the most basic flaw.
            Set<SequencingRunDto> reportedErrors = new HashSet<>();

            // No event.
            int count = 0;
            for (SequencingRunDto dto : loggingDtos) {
                if (!dto.isComplete() && !reportedErrors.contains(dto) &&
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
                if (!dto.isComplete() && !reportedErrors.contains(dto) &&
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
                if (!dto.isComplete() && !reportedErrors.contains(dto) &&
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
                if (!dto.isComplete() && !reportedErrors.contains(dto) &&
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
                if (!dto.isComplete() && !reportedErrors.contains(dto) &&
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
                if (!dto.isComplete() && !reportedErrors.contains(dto) &&
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
        }
    }
}
