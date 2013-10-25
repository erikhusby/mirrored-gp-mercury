package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;

import javax.annotation.Nonnull;
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

import static org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter.System.BOTH;
import static org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter.System.MERCURY;
import static org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter.System.SQUID;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel.SampleType;
import static org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch.LabBatchType;

/**
 * Utility for routing messages and queries to Mercury or Squid as determined by the supplied sample containers.
 * Defines the notion of a vessel belonging to either Mercury or Squid.
 * <p>
 * The current definition of "belonging" to Mercury is that all vessels upstream of the specified vessel (or set of
 * vessels) have been batched for a Mercury-supported workflow.
 *
 */
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

    private LabVesselDao         labVesselDao;
    private ControlDao           controlDao;
    private WorkflowLoader       workflowLoader;
    private BSPSampleDataFetcher bspSampleDataFetcher;

    SystemRouter() {
    }

    @Inject
    public SystemRouter(LabVesselDao labVesselDao, ControlDao controlDao,
                        WorkflowLoader workflowLoader, BSPSampleDataFetcher bspSampleDataFetcher) {
        this.labVesselDao = labVesselDao;
        this.controlDao = controlDao;
        this.workflowLoader = workflowLoader;
        this.bspSampleDataFetcher = bspSampleDataFetcher;
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
     * Determines if a tube belongs to Mercury or Squid. See {@link org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter.System} for a description of "belongs".
     *
     * @param barcode the barcode of the tube to check
     *
     * @return An instance of a MercuryOrSquid enum that will assist in determining to which system requests should be
     * routed.
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

    private System routeForVessels(Collection<LabVessel> labVessels, Intent intent) {
        Set<System> routingOptions = EnumSet.noneOf(System.class);

        // Determine which samples might be controls
        Set<SampleInstance> possibleControls = new HashSet<>();
        for (LabVessel labVessel : labVessels) {
            if (labVessel != null) {
                Set<SampleInstance> sampleInstances = labVessel.getSampleInstances(SampleType.PREFER_PDO, LabBatchType.WORKFLOW);
                for (SampleInstance sampleInstance : sampleInstances) {
                    String productOrderKey = sampleInstance.getProductOrderKey();
                    if (productOrderKey == null) {
                        possibleControls.add(sampleInstance);
                    }
                }
            }
        }

        List<String> controlCollaboratorSampleIds = new ArrayList<>();
        Collection<String> sampleNames = new ArrayList<>();
        Map<String, BSPSampleDTO> mapSampleNameToDto = null;
        if (!possibleControls.isEmpty()) {
            for (SampleInstance sampleInstance : possibleControls) {
                sampleNames.add(sampleInstance.getStartingSample().getSampleKey());
            }
            mapSampleNameToDto = bspSampleDataFetcher.fetchSamplesFromBSP(sampleNames);

            List<Control> controls = controlDao.findAllActive();
            for (Control control : controls) {
                controlCollaboratorSampleIds.add(control.getCollaboratorSampleId());
            }
        }
        System system = routeForVessels(labVessels, controlCollaboratorSampleIds, mapSampleNameToDto, intent);
        if (system != null) {
            routingOptions.add(system);
        }

        return evaluateRoutingOption(routingOptions, intent);
    }

    // TODO: figure out how to handle libraryNames for fetchLibraryDetailsByLibraryName

    /**
     *
     * This is the main method that will determine, for a given lab vessel, to which system transactions for that
     * vessel should be routed.
     * <p />
     *
     * The logic within this method will utilize the vessel to navigate back to the correct PDO and determine what
     * routing is configured for the workflow associated with the PDO.
     *  * When the intent is system-of-record and a control tube is given, null will be returned. This reflects the fact
     * that, for system-of-record determination, controls defer to their travel partners.
     *
     * @param vessels a collection of LabVessels for which system routing is to be determined
     * @param controlCollaboratorSampleIds list of collaborator IDs for controls
     * @param mapSampleNameToDto map from sample name to BSP sample DTO
     * @param intent whether to return one routing option, or multiple
     * @return An instance of a MercuryOrSquid enum that will assist in determining to which system requests should be
     */
    @DaoFree
    public System routeForVessels(Collection<LabVessel> vessels, List<String> controlCollaboratorSampleIds,
                                         Map<String, BSPSampleDTO> mapSampleNameToDto, Intent intent) {
        Set<System> routingOptions = EnumSet.noneOf(System.class);
        for (LabVessel vessel : vessels) {
            if (vessel == null) {
                routingOptions.add(SQUID);
            } else {
                Set<SampleInstance> sampleInstances = vessel.getSampleInstances(SampleType.PREFER_PDO, LabBatchType.WORKFLOW);
                if (sampleInstances.isEmpty()) {
                    routingOptions.add(SQUID);
                } else {
                    Set<SampleInstance> possibleControls = new HashSet<>();
                    for (SampleInstance sampleInstance : sampleInstances) {
                        if (sampleInstance.getAllWorkflowLabBatches().isEmpty()) {
                            possibleControls.add(sampleInstance);
                        } else {
                            String workflowName = sampleInstance.getWorkflowName();
                            LabBatch batch = sampleInstance.getLabBatch();
                            if (workflowName != null && batch != null) {
                                ProductWorkflowDefVersion productWorkflowDef = getWorkflowVersion(workflowName,
                                        batch.getCreatedOn());
                                if (intent == Intent.SYSTEM_OF_RECORD) {
                                    System system;
                                    if (productWorkflowDef.getInValidation()) {
                                        LabBatch labBatch = sampleInstance.getLabBatch();
                                        if(labBatch == null) {
                                            throw new RuntimeException("No lab batch for sample " +
                                                                     sampleInstance.getStartingSample().getSampleKey());
                                        }
                                        // Per Andrew, we can assume that validation plastic has only one LCSET
                                        if(labBatch.isValidationBatch()) {
                                            system = MERCURY;
                                        } else {
                                            system = productWorkflowDef.getRouting();
                                        }
                                    } else {
                                        system = productWorkflowDef.getRouting();
                                    }
                                    if (system == BOTH) {
                                        badCrspRouting();
                                        system = SQUID;
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
                    if (!possibleControls.isEmpty()) {

                        // TODO: move this logic into ControlEjb?

                        // Don't bother querying BSP if Mercury doesn't have any active controls.
                        if (controlCollaboratorSampleIds.isEmpty()) {
                            badCrspRouting();
                            routingOptions.add(SQUID);
                        } else {
                            for (SampleInstance possibleControl : possibleControls) {
                                String sampleKey = possibleControl.getStartingSample().getSampleKey();
                                BSPSampleDTO sampleDTO = mapSampleNameToDto.get(sampleKey);
                                if (sampleDTO == null) {
                                    // Don't know what this is, but it isn't for Mercury.
                                    routingOptions.add(SQUID);
                                } else {
                                    if (controlCollaboratorSampleIds.contains(sampleDTO.getCollaboratorsSampleName())) {

                                        /*
                                         * It's a control, but only give it a vote if we can pin it to a workflow. It
                                         * might be slightly more correct to return SQUID if we don't have a workflow,
                                         * but that doesn't work in the case of the first message for a batch that has
                                         * had a non-batched control re-arrayed into it.
                                         */
                                        String workflowName = possibleControl.getWorkflowName();
                                        LabBatch effectiveBatch = possibleControl.getLabBatch();
                                        if (workflowName != null && effectiveBatch != null) {
                                            ProductWorkflowDefVersion productWorkflowDef =
                                                    getWorkflowVersion(workflowName, effectiveBatch.getCreatedOn());
                                            routingOptions.add(productWorkflowDef.getRouting());
                                        }
                                    } else {
                                        badCrspRouting();
                                        routingOptions.add(SQUID);
                                    }
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

    private void badCrspRouting() {
        if(Deployment.isCRSP) {
            throw new RouterException("For a CRSP Deployment, Squid should never be a routing option")  ;
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
     * TODO: Consider routing for controls
     * Controls recognized by Mercury currently route to BOTH. When Mercury is the system of record for a workflow,
     * we'll start to see routingOptions of { BOTH, MERCURY }, which this implementation will currently reject.
     *
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
            badCrspRouting();
            result = SQUID;
        } else if (routingOptions.size() == 1) {
            result = routingOptions.iterator().next();
        } else if (routingOptions.equals(EnumSet.of(SQUID, BOTH)) ||
                (intent == Intent.SYSTEM_OF_RECORD && routingOptions.equals(EnumSet.of(SQUID, MERCURY)))) {
            badCrspRouting();
            result = SQUID;
        } else if (routingOptions.equals(EnumSet.of(MERCURY, BOTH))) {
            result = MERCURY;
        } else {
            throw new RouterException("The Routing cannot be determined for options: " + routingOptions);
        }

        return result;
    }

    /** Returns the workflowDef for the given workflowName and date. */
    private ProductWorkflowDefVersion getWorkflowVersion(@Nonnull String workflowName, @Nonnull Date effectiveDate) {

        WorkflowConfig workflowConfig = workflowLoader.load();

        return workflowConfig.getWorkflowVersionByName(workflowName, effectiveDate);
    }
}
