package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.bsp.exports.BSPExportsService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.exports.IsExported;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;

import javax.annotation.Nonnull;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class determines the system of record (Mercury or Squid) for one or more lab vessels.
 * A vessel "belongs" to Mercury if all vessels upstream of the specified vessel (or set of
 * vessels) have been batched for a Mercury-supported workflow.
 *
 * Message routing has been removed from this class because the only destination is now Mercury.
 */
@Dependent
public class SystemOfRecord implements Serializable {

    private static final long serialVersionUID = 20130507L;

    public enum System {
        MERCURY, SQUID
    }

    private LabVesselDao labVesselDao;
    private WorkflowConfig workflowConfig;
    private BSPExportsService bspExportsService;

    SystemOfRecord() {
    }

    @Inject
    public SystemOfRecord(LabVesselDao labVesselDao, WorkflowConfig workflowConfig, BSPExportsService bspExportsService) {
        this.labVesselDao = labVesselDao;
        this.workflowConfig = workflowConfig;
        this.bspExportsService = bspExportsService;
    }

    /**
     * Determines if the lab vessels were processed by Mercury or by Squid.
     */
    public System getSystemOfRecord(Collection<String> barcodes) {
        return systemOfRecord(labVesselDao.findByBarcodes(new ArrayList<>(barcodes)).values());
    }

    /**
     * Determines if the lab vessels were processed by Mercury or by Squid.
     */
    public System getSystemOfRecordForVessels(Collection<LabVessel> labVessels) {
        return systemOfRecord(labVessels);
    }

    /**
     * Determines if the lab vessel was processed by Mercury or by Squid.
     */
    public System getSystemOfRecordForVessel(LabVessel labVessel) {
        return systemOfRecord(Collections.singleton(labVessel));
    }

    /**
     * Determines if the lab vessel was processed by Mercury or by Squid.
     */
    public System getSystemOfRecord(String barcode) {
        return getSystemOfRecord(Collections.singletonList(barcode));
    }

    /**
     * Determines the system of record from the BSP export status.  If none of the vessels have been exported
     * then the system of record is Mercury.  If all have been exported to Sequencing then the system of record
     * is Squid. For any other condition, the system of record is ambiguous and an exception is thrown.
     */
    private System determineSystemOfRecordPerBspExports(@Nonnull Collection<LabVessel> labVessels) {
        IsExported.ExportResults exportResults = bspExportsService.findExportDestinations(labVessels);
        Multimap<System, String> systemToVessels = HashMultimap.create();
        for (IsExported.ExportResult exportResult : exportResults.getExportResult()) {
            Set<IsExported.ExternalSystem> externalSystems = exportResult.getExportDestinations();
            String vesselBarcode = exportResult.getBarcode();
            if (CollectionUtils.isEmpty(externalSystems)) {
                // If there are no export destinations given in the results, look for lookup misses or errors.
                String error = exportResult.getError();

                if (error == null) {
                    // The vessel is recognized but was never exported, so this is MERCURY.
                    systemToVessels.put(System.MERCURY, vesselBarcode);
                } else {
                    // Error trying to look up vessel.
                    throw new InformaticsServiceException(error);
                }
            } else if (externalSystems.contains(IsExported.ExternalSystem.Sequencing)) {
                systemToVessels.put(System.SQUID, vesselBarcode);
            } else if (externalSystems.contains(IsExported.ExternalSystem.Mercury)) {
                systemToVessels.put(System.MERCURY, vesselBarcode);
            } else {
                // We are not currently expecting to see vessels exported to destinations other than Sequencing
                // or Mercury.
                String message = String.format("Unexpected export destination(s) for vessel '%s': %s",
                        vesselBarcode, StringUtils.join(externalSystems, ", "));
                throw new InformaticsServiceException(message);
            }
        }

        if (systemToVessels.keySet().size() > 1) {
            String message = "Ambiguous systems of record for vessels: " +
                    systemToVessels.asMap().entrySet().stream().
                            map(mapEntry -> String.format("%s: %s",
                                    mapEntry.getKey(),
                                    // Sorts the vessel barcodes.
                                    mapEntry.getValue().stream().sorted().collect(Collectors.joining(", ")))).
                            sorted(). // Sorts the system names.
                            collect(Collectors.joining(" "));
            throw new InformaticsServiceException(message);
        }

        // Return the unique System key.
        return systemToVessels.keySet().iterator().next();
    }

    /**
     * Applies a sequence of tests to determine the system of record for the vessels.
     */
    private System systemOfRecord(Collection<LabVessel> labVessels) {
        // If all samples have Mercury metadata source then the relevant system is Mercury.
        int nonNullCount = 0;
        int sampleCount = 0;
        int mercurySourceCount = 0;
        Set<LabVessel> nonMercurySampleVesselsSet = new HashSet<>();
        List<LabVessel> nonMercurySampleVesselsList = new ArrayList<>();
        for (LabVessel labVessel : labVessels) {
            if (labVessel != null) {
                nonNullCount++;
                boolean foundBsp = false;
                Set<SampleInstanceV2> sampleInstancesV2 = labVessel.getSampleInstancesV2();
                for (SampleInstanceV2 sampleInstanceV2 : sampleInstancesV2) {
                    Set<MercurySample> rootMercurySamples = sampleInstanceV2.getRootMercurySamples();
                    if (rootMercurySamples.isEmpty()) {
                        foundBsp = true;
                    } else {
                        for (MercurySample mercurySample : rootMercurySamples) {
                            sampleCount++;
                            if (mercurySample.getMetadataSource() == MercurySample.MetadataSource.MERCURY) {
                                mercurySourceCount++;
                            } else if (mercurySample.getMetadataSource() == MercurySample.MetadataSource.BSP) {
                                foundBsp = true;
                            }
                        }
                    }
                }
                if (foundBsp) {
                    if (nonMercurySampleVesselsSet.add(labVessel)) {
                        nonMercurySampleVesselsList.add(labVessel);
                    }
                }
            }
        }
        if (nonNullCount == labVessels.size() && mercurySourceCount > 0 && sampleCount == mercurySourceCount) {
            return System.MERCURY;
        }

        // If all the vessels' events have event types that are handled exclusively by one system,
        // then that system is the system of record.
        Set<LabEventType.SystemOfRecord> systemsOfRecord = EnumSet.noneOf(LabEventType.SystemOfRecord.class);
        for (LabVessel labVessel : labVessels) {
            if (labVessel != null) {
                for (LabEvent labEvent : labVessel.getInPlaceAndTransferToEvents()) {
                    systemsOfRecord.add(labEvent.getLabEventType().getSystemOfRecord());
                }
            }
        }
        if (systemsOfRecord.size() == 1) {
            LabEventType.SystemOfRecord systemOfRecord = systemsOfRecord.iterator().next();
            if (systemOfRecord == LabEventType.SystemOfRecord.SQUID) {
                return System.SQUID;
            }
            if (systemOfRecord == LabEventType.SystemOfRecord.MERCURY) {
                return determineSystemOfRecordPerBspExports(nonMercurySampleVesselsList);
            }
        }

        // Determines the system of record using workflow routing rules for vessels that have been batched.
        Set<System> systems = workflowRoutingRules(labVessels);
        switch (systems.size()) {
        case 0:
            // Keeps Squid as the system of record for unbatched vessels.
            return System.SQUID;
        case 1:
            return systems.iterator().next();
        default:
            // Routing rules indicate both systems.
            return System.SQUID;
        }
    }

    /**
     * Returns all the workflow routing rules for the vessels.
     * Returns empty if all vessels are unbatched or have purely reagents.
     */
    @DaoFree
    public Set<System> workflowRoutingRules(Collection<LabVessel> vessels) {
        Set<System> routingOptions = EnumSet.noneOf(System.class);
        for (LabVessel vessel : vessels) {
            if (vessel == null) {
                routingOptions.add(System.SQUID);
            } else {
                Set<SampleInstanceV2> sampleInstances = vessel.getSampleInstancesV2();
                if (sampleInstances.isEmpty()) {
                    routingOptions.add(System.SQUID);
                } else {
                    for (SampleInstanceV2 sampleInstance : sampleInstances) {
                        if (!sampleInstance.isReagentOnly()) {
                            for (LabBatch batch : sampleInstance.getAllWorkflowBatches()) {
                                String workflowName = batch.getWorkflowName();
                                ProductWorkflowDefVersion productWorkflowDef = getWorkflowVersion(workflowName,
                                        batch.getCreatedOn());
                                if (productWorkflowDef.getInValidation() && batch.isValidationBatch() ||
                                        productWorkflowDef.getRoutingRule() ==
                                                ProductWorkflowDefVersion.RoutingRule.MERCURY) {
                                    routingOptions.add(System.MERCURY);
                                } else {
                                    // Routing to Squid only, or to both.
                                    routingOptions.add(System.SQUID);
                                }
                            }
                        }
                    }
                }
            }
        }
        return routingOptions;
    }

    /** Returns the workflowDef for the given workflowName and date. */
    private ProductWorkflowDefVersion getWorkflowVersion(@Nonnull String workflowName, @Nonnull Date effectiveDate) {
        return workflowConfig.getWorkflowVersionByName(workflowName, effectiveDate);
    }

}
