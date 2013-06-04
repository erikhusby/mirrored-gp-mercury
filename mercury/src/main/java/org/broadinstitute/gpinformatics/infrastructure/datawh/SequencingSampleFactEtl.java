package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.collections15.map.LRUMap;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel.SampleType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch.LabBatchType;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.*;

@Stateful
public class SequencingSampleFactEtl extends GenericEntityEtl<SequencingRun, SequencingRun> {
    private ProductOrderDao pdoDao;

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

        if (entity != null) {
            Collection<SequencingRunDto> dtos = makeSequencingRunDtos(entity);
            for (SequencingRunDto dto : dtos) {
                records.add(genericRecord(etlDateStr, isDelete,
                        entity.getSequencingRunId(),
                        format(dto.flowcellBarcode),
                        format(dto.laneName),
                        format(dto.molecularIndexingSchemeName),
                        format(dto.productOrderId),
                        format(dto.sampleKey),
                        format(dto.researchProjectId)
                ));
            }
        }

        return records;
    }

    private static class SequencingRunDto {
        String flowcellBarcode;
        String laneName;
        String molecularIndexingSchemeName;
        long sequencingRunId;
        String runName;
        Date runDate;
        String productOrderId;
        String sampleKey;
        String researchProjectId;

        SequencingRunDto(
                String flowcellBarcode,
                String laneName,
                String molecularIndexingSchemeName,
                Date runDate,
                String runName,
                long sequencingRunId,
                String productOrderId,
                String sampleKey,
                String researchProjectId
        ) {
            this.sampleKey = sampleKey;
            this.runDate = runDate;
            this.runName = runName;
            this.sequencingRunId = sequencingRunId;
            this.laneName = laneName;
            this.flowcellBarcode = flowcellBarcode;
            this.molecularIndexingSchemeName = molecularIndexingSchemeName;
            this.productOrderId = productOrderId;
            this.researchProjectId = researchProjectId;
        }
    }


    // Cache lookups for efficiency.
    private static final int CACHE_SIZE = 16;
    private static Map<String, String> pdoKeyToPdoId = new LRUMap<>(CACHE_SIZE);
    private static Map<String, String> pdoKeyToResearchProjectId = new LRUMap<>(CACHE_SIZE);

    private Collection<SequencingRunDto> makeSequencingRunDtos(SequencingRun entity) {
        Collection<SequencingRunDto> dtos = new ArrayList<>();

        RunCartridge cartridge = entity.getSampleCartridge();
        String laneName = cartridge.getCartridgeName();
        String flowcellBarcode = cartridge.getCartridgeBarcode();

        if (!StringUtils.isBlank(laneName) && !StringUtils.isBlank(flowcellBarcode)) {
            int uniquifierForNone = 0;
            int uniquifierForMultiple = 0;
            int missingStartingSampleCount = 0;
            int missingPdoKeyCount = 0;
            int missingPdoCount = 0;
            Set<String> molecularIndexingSchemeNames = new HashSet<String>();
            Collection<SampleInstance> sampleInstances =
                    cartridge.getSampleInstances(SampleType.WITH_PDO, LabBatchType.WORKFLOW);

            for (SampleInstance si : sampleInstances) {
                String molecularIndexingSchemeName = null;
                String sampleKey = null;
                String productOrderId = null;
                String researchProjectId = null;

                String pdoKey = si.getProductOrderKey();
                if (pdoKey != null) {

                    if (pdoKeyToPdoId.containsKey(pdoKey)) {
                        productOrderId = pdoKeyToPdoId.get(pdoKey);
                        researchProjectId = pdoKeyToResearchProjectId.get(pdoKey);
                    } else {
                        // Fills cache with either value or null, to avoid gratuitous lookups.
                        ProductOrder pdo = pdoDao.findByBusinessKey(pdoKey);

                        productOrderId = (pdo != null) ? String.valueOf(pdo.getProductOrderId()) : null;
                        pdoKeyToPdoId.put(pdoKey, productOrderId);

                        researchProjectId = (pdo != null && pdo.getResearchProject() != null) ?
                                String.valueOf(pdo.getResearchProject().getResearchProjectId()) : null;
                        pdoKeyToResearchProjectId.put(pdoKey, researchProjectId);
                    }

                    if (productOrderId != null) {
                        MercurySample sample = si.getStartingSample();
                        if (sample != null) {
                            sampleKey = sample.getSampleKey();
                        }
                        if (sampleKey != null) {
                            for (Reagent reagent : si.getReagents()) {
                                if (OrmUtil.proxySafeIsInstance(reagent, MolecularIndexReagent.class)) {
                                    if (molecularIndexingSchemeName == null) {
                                        MolecularIndexingScheme molecularIdxScheme =
                                                ((MolecularIndexReagent) reagent).getMolecularIndexingScheme();
                                        molecularIndexingSchemeName = molecularIdxScheme.getName();
                                    } else {
                                        molecularIndexingSchemeName = "MULTIPLE_" + uniquifierForMultiple++;
                                    }
                                }
                            }
                            if (molecularIndexingSchemeName == null) {
                                molecularIndexingSchemeName = "NONE_" + uniquifierForNone++;
                            }
                            // Removes expected duplicates.
                            if (!molecularIndexingSchemeNames.contains(molecularIndexingSchemeName)) {
                                molecularIndexingSchemeNames.add(molecularIndexingSchemeName);
                                dtos.add(new SequencingRunDto(
                                        flowcellBarcode,
                                        laneName,
                                        molecularIndexingSchemeName,
                                        entity.getRunDate(),
                                        entity.getRunName(),
                                        entity.getSequencingRunId(),
                                        productOrderId,
                                        sampleKey,
                                        researchProjectId));
                            }
                        } else {
                            ++missingStartingSampleCount;
                        }
                    } else {
                        ++missingPdoCount;
                    }
                } else {
                    ++missingPdoKeyCount;
                }
            }
            if (missingStartingSampleCount > 0) {
                logger.debug(missingStartingSampleCount + " missing startingSamples in sequencingRun " +
                        entity.getSequencingRunId() + " lane " + laneName);
            }
            if (missingPdoKeyCount > 0) {
                logger.debug(missingPdoKeyCount + " missing productOrderKeys in sequencingRun " +
                        entity.getSequencingRunId() + " lane " + laneName);
            }
            if (missingPdoCount > 0) {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry entry : pdoKeyToPdoId.entrySet()) {
                    if (entry.getValue() == null) {
                        if (sb.length() > 0) {
                            sb.append(", ");
                        }
                        sb.append(entry.getKey());
                    }
                }
                logger.debug("SequencingRun cannot find ProductOrder entity for key " + sb.toString());
            }
            if (sampleInstances.size() == 0) {
                logger.debug("No sample instances found for sequencingRun " +
                        entity.getSequencingRunId() + " lane " + laneName);
            }

        } else {
            logger.debug("SequencingRun " + entity.getSequencingRunId() +
                    " is missing either cartridge (lane) name or flowcell barcode.");
        }


        return dtos;
    }

}
