package org.broadinstitute.gpinformatics.mercury.boundary.lims;

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
import java.util.Set;

/**
 * Utility for routing messages and queries to Mercury or Squid as determined by the supplied sample containers.
 * Defines the notion of a vessel belonging to either Mercury or Squid.
 * <p>
 * The current definition of "belonging" to Mercury is that all vessels upstream of the specified vessel (or set of
 * vessels) have been batched for a Mercury-supported workflow.
 *
 */
@Dependent
public class SystemRouter implements Serializable {

    private static final long serialVersionUID = 20130507L;

    /**
     * This enum defines the possible choices for where work is to be routed during normal Mercury processing.
     * The order determines the hierarchy of routing considerations:
     *
     * <ol>
     * <li>BOTH -- The safest bet is to send to both systems.  If there is any doubt that all work for all vessels
     * should go to either Mercury or Squid, send to both</li>
     * <li>MERCURY -- If we are sure that all elements being evaluated are only for Mercury, keep it in the
     * mercury system</li>
     * <li>SQUID -- If we are sure that all elements being evaluated are only for squid, or have no record of
     * the element in Mercury, send it to Squid</li>
     * </ol>
     */
    public enum System {
        BOTH, MERCURY, SQUID
    }

    public enum Intent {
        /** route messages (possibly to multiple systems) */
        ROUTE,
        /** determine which system will handle LIMS queries (only one system) */
        SYSTEM_OF_RECORD
    }

    private LabVesselDao labVesselDao;
    private WorkflowConfig workflowConfig;
    private BSPExportsService bspExportsService;

    SystemRouter() {
    }

    @Inject
    public SystemRouter(LabVesselDao labVesselDao, WorkflowConfig workflowConfig, BSPExportsService bspExportsService) {
        this.labVesselDao = labVesselDao;
        this.workflowConfig = workflowConfig;
        this.bspExportsService = bspExportsService;
    }

    /**
     * Takes a collection of barcodes for which a user wishes to determine the system of record.
     *
     * @param barcodes a Collection of barcodes that correspond to lab vessels that are to be processed by the system
     *
     * @return An instance of a MercuryOrSquid enum that will assist in determining to which system requests should be
     * routed.
     */
    public System routeForVesselBarcodes(Collection<String> barcodes) {
        return routeForVesselBarcodes(barcodes, Intent.ROUTE);
    }

    public System getSystemOfRecordForVesselBarcodes(Collection<String> barcodes) {
        return routeForVesselBarcodes(barcodes, Intent.SYSTEM_OF_RECORD);
    }

    private System routeForVesselBarcodes(Collection<String> barcodes, Intent intent) {
        Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(new ArrayList<>(barcodes));
        // can't use mapBarcodeToVessel.values(), because it doesn't include nulls
        List<LabVessel> labVessels = new ArrayList<>();
        for (Map.Entry<String, LabVessel> stringLabVesselEntry : mapBarcodeToVessel.entrySet()) {
            labVessels.add(stringLabVesselEntry.getValue());
        }
        return routeForVessels(labVessels, intent);
    }

    /**
     * Determines if a tube belongs to Mercury or Squid. See {@link System} for a description of "belongs".
     *
     * @param barcode the barcode of the tube to check
     *
     * @return An instance of a System enum that determines to which system requests should be routed.
     */
    public System routeForVessel(String barcode) {
        return routeForVesselBarcodes(Collections.singletonList(barcode), Intent.ROUTE);
    }

    public System getSystemOfRecordForVessel(String barcode) {
        return routeForVesselBarcodes(Collections.singletonList(barcode), Intent.SYSTEM_OF_RECORD);
    }

    /**
     * Takes a collection of lab vessels for which a user wishes to determine the system of record.
     *
     * @param labVessels entities
     * @return An instance of a MercuryOrSquid enum that will assist in determining to which system requests should be
     * routed.
     */
    public System routeForVessels(Collection<LabVessel> labVessels) {
        return routeForVessels(labVessels, Intent.ROUTE);
    }

    /**
     * Query BSP for the export status of all labVessels given as parameters.  If none of these has been exported,
     * route to Mercury.  If all have been exported to Sequencing (possibly including parallel validation exports to
     * Mercury), route to Squid.  If there is any other condition, throw an InformaticsServiceException as the
     * situation is ambiguous.
     */
    private System determineSystemOfRecordPerBspExports(@Nonnull Collection<LabVessel> labVessels) {
        IsExported.ExportResults exportResults = bspExportsService.findExportDestinations(labVessels);

        MultiMap<System, String> systemToVessels = new MultiValueMap<>();
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
            } else {
                // Parallel validation sets will be exported to both Squid and Mercury, but Squid gets priority as the
                // system of record until the process of transitioning Squid to Mercury is complete.
                if (externalSystems.contains(IsExported.ExternalSystem.Sequencing)) {
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
        }

        if (systemToVessels.size() > 1) {
            StringBuilder builder = new StringBuilder("Ambiguous systems of record for vessels: ");
            for (Map.Entry<System, Object> entry : systemToVessels.entrySet()) {
                String vesselBarcodes = StringUtils.join((Collection<?>) entry.getValue(), ", ");
                builder.append(entry.getKey()).append(": ").append(vesselBarcodes);
            }

            throw new InformaticsServiceException(builder.toString());
        }

        // Return the unique System key.
        return systemToVessels.keySet().iterator().next();
    }

    /**
     * Applies a sequence of short cuts toward the goal of determining the system to use for the
     * specified intent (e.g. which system will process a vessel, or which has the sample data records).
     */
    private System routeForVessels(Collection<LabVessel> labVessels, Intent intent) {
        // If all samples have Mercury for their metadata source, then regardless of intent, the
        // relevant system is Mercury, because neither BSP nor Squid know about the samples.
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
        if (intent == Intent.SYSTEM_OF_RECORD) {
            Set<LabEventType.SystemOfRecord> systemsOfRecord = EnumSet.noneOf(LabEventType.SystemOfRecord.class);
            for (LabVessel labVessel : labVessels) {
                if (labVessel != null) {
                    for (LabEvent labEvent : labVessel.getInPlaceAndTransferToEvents()) {
                        systemsOfRecord.add(labEvent.getLabEventType().getSystemOfRecord());
                    }
                }
            }

            // If the System of record is unambiguous in these events and is either SQUID or MERCURY, short circuit
            // any further evaluation.
            if (systemsOfRecord.size() == 1) {
                LabEventType.SystemOfRecord systemOfRecord = systemsOfRecord.iterator().next();
                switch (systemOfRecord) {
                case SQUID:
                    return System.SQUID;
                case MERCURY:
                    // If everything here has been exported to sequencing.
                    return determineSystemOfRecordPerBspExports(nonMercurySampleVesselsList);
                case WORKFLOW_DEPENDENT:
                    // Fall through.
                }
            }
        }

        // None of the above short cuts succeeded, so do the full vessel analysis.
        Set<System> routingOptions = EnumSet.noneOf(System.class);
        System system = routeForVesselsDaoFree(labVessels, intent);
        if (system != null) {
            routingOptions.add(system);
        }

        return evaluateRoutingOption(routingOptions, intent);
    }

    /**
     * Determines the system to use for the specified intent (e.g. which system will process a vessel,
     * or which has the sample data records), using what is configured for the workflow associated with
     * the vessels' workflow lab batch.
     *
     * @param vessels a collection of LabVessel.
     * @param intent whether to return one routing option, or multiple
     * @return determines which system or systems serve the vessels.
     */
    @DaoFree
    public System routeForVesselsDaoFree(Collection<LabVessel> vessels, Intent intent) {
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
                        if (sampleInstance.isReagentOnly()) {
                            continue;
                        }
                        if (sampleInstance.getAllWorkflowBatches().isEmpty()) { // todo jmt what about bucket entry with no batch?
                            routingOptions.add(System.SQUID);
                        } else {
                            for (LabBatch batch : sampleInstance.getAllWorkflowBatches()) {
                                String workflowName = batch.getWorkflowName();
                                if (workflowName != null) {
                                    ProductWorkflowDefVersion productWorkflowDef = getWorkflowVersion(workflowName,
                                            batch.getCreatedOn());
                                    if (intent == Intent.SYSTEM_OF_RECORD) {
                                        System system;
                                        if (productWorkflowDef.getInValidation() && batch.isValidationBatch()) {
                                            system = System.MERCURY;
                                        } else {
                                            system = productWorkflowDef.getRouting();
                                        }
                                        if (system == System.BOTH) {
                                            system = System.SQUID;
                                        }
                                        routingOptions.add(system);
                                    } else {
                                        routingOptions.add(productWorkflowDef.getRouting());
                                    }
                                } else {
                                    // TODO: what about this case?
                                }
                            }
                        }
                    }
                }
            }
        }

        if (routingOptions.isEmpty() && intent == Intent.SYSTEM_OF_RECORD) {
            return null;
        } else {
            return evaluateRoutingOption(routingOptions, intent);
        }
    }

    /**
     * Helper method to assist in determining the cumulative routing suggestion for a collection of vessels.
     * <p />
     *
     * This method will either give the user a determined routing suggestion, or throw a {@link RouterException} if
     * a safe routing option cannot be determined based on the routing options given.
     * <p />
     *
     * The logic is as follows:
     * <ul>
     *     <li>If there is but one routing option passed, that is the winner</li>
     *     <li>If there is a combination of SQUID and BOTH as routing options, route to Squid.  Since it has
     *     historically been the main LIMS system of record, in this scenario it is the safest bet</li>
     *     <li>If there are any other combinations, throw a {@link RouterException}</li>
     * </ul>
     *
     * @param routingOptions A navigable collection of determined routing options for a collection of lab vessels.
     *
     * @param intent routing or queries
     * @return An instance of a MercuryOrSquid enum that will assist in determining to which system requests should be
     * routed.
     */
    private System evaluateRoutingOption(Set<System> routingOptions, Intent intent) {

        System result;

        if (routingOptions.isEmpty()) {
            result = System.SQUID;
        } else if (routingOptions.size() == 1) {
            result = routingOptions.iterator().next();
        } else if (routingOptions.equals(EnumSet.of(System.SQUID, System.BOTH)) ||
                (intent == Intent.SYSTEM_OF_RECORD && routingOptions.equals(EnumSet.of(System.SQUID, System.MERCURY)))) {
            result = System.SQUID;
        } else if (routingOptions.equals(EnumSet.of(System.MERCURY, System.BOTH))) {
            result = System.MERCURY;
        } else {
            throw new RouterException("The Routing cannot be determined for options: " + routingOptions);
        }

        return result;
    }

    /** Returns the workflowDef for the given workflowName and date. */
    private ProductWorkflowDefVersion getWorkflowVersion(@Nonnull String workflowName, @Nonnull Date effectiveDate) {
        return workflowConfig.getWorkflowVersionByName(workflowName, effectiveDate);
    }

}
