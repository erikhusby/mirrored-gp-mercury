package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowException;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import static org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter.MercuryOrSquid.BOTH;
import static org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter.MercuryOrSquid.MERCURY;
import static org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter.MercuryOrSquid.SQUID;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel.*;
import static org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch.*;

/**
 * Utility for routing messages and queries to Mercury or Squid as determined by the supplied sample containers.
 * Defines the notion of a vessel belonging to either Mercury or Squid.
 * <p>
 * The current definition of "belonging" to Mercury is that all vessels upstream of the specified vessel (or set of
 * vessels) have been batched for Exome Express.
 *
 * TODO SGM  This needs a better name since the options are more than just Mercury or Squid!!!!
 */
public class MercuryOrSquidRouter implements Serializable {

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
    public enum MercuryOrSquid {
        BOTH, MERCURY, SQUID
    }

    private LabVesselDao         labVesselDao;
    private ControlDao           controlDao;
    private AthenaClientService  athenaClientService;
    private WorkflowLoader       workflowLoader;
    private BSPSampleDataFetcher bspSampleDataFetcher;

    MercuryOrSquidRouter() {
    }

    @Inject
    public MercuryOrSquidRouter(LabVesselDao labVesselDao, ControlDao controlDao,
                                AthenaClientService athenaClientService,
                                WorkflowLoader workflowLoader, BSPSampleDataFetcher bspSampleDataFetcher) {
        this.labVesselDao = labVesselDao;
        this.controlDao = controlDao;
        this.athenaClientService = athenaClientService;
        this.workflowLoader = workflowLoader;
        this.bspSampleDataFetcher = bspSampleDataFetcher;
    }

    /**
     * Building on {@link #routeForVessel(String)}, this method takes a list of barcodes for which a user wishes to
     * determine the system of record.  The work is delegated to {@link #routeForVessel(String)}
     *
     *
     * @param barcodes a Collection of barcodes that correspond to lab vessels that are to be processed by the system
     *
     * @return An instance of a MercuryOrSquid enum that will assist in determining to which system requests should be
     * routed.
     */
    public MercuryOrSquid routeForVessels(Collection<String> barcodes) {

        NavigableSet<MercuryOrSquid> routingOptions = new TreeSet<MercuryOrSquid>();
        for (String vesselBarcode : barcodes) {
            MercuryOrSquid determinedRoute = routeForVessel(vesselBarcode);

            routingOptions.add(determinedRoute);
        }

        return evaluateRoutingOption(routingOptions);
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
     * @return An instance of a MercuryOrSquid enum that will assist in determining to which system requests should be
          * routed.
     */
    private MercuryOrSquid evaluateRoutingOption(NavigableSet<MercuryOrSquid> routingOptions) {

        MercuryOrSquid result = SQUID;

        if (!routingOptions.isEmpty()) {
            result = routingOptions.last();

            if (routingOptions.size() > 1) {
                if ((routingOptions.contains(SQUID) && routingOptions.contains(MERCURY)) ||
                            (routingOptions.contains(BOTH) && routingOptions.contains(MERCURY))) {
                    throw new RouterException("The Routing cannot be determined");
                }
            }
        }
        return result;
    }

    /**
     * Determines if a tube belongs to Mercury or Squid. See {@link MercuryOrSquid} for a description of "belongs".
     *
     * @param barcode the barcode of the tube to check
     *
     * @return An instance of a MercuryOrSquid enum that will assist in determining to which system requests should be
     * routed.
     */
    public MercuryOrSquid routeForVessel(String barcode) {
        LabVessel vessel = labVesselDao.findByIdentifier(barcode);
        return routeForVessel(vessel);
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
     *
     * @param vessel an instance of a LabVessel for which system routing is to be determined
     * @return An instance of a MercuryOrSquid enum that will assist in determining to which system requests should be
     * routed.
     */
    public MercuryOrSquid routeForVessel(LabVessel vessel) {
        NavigableSet<MercuryOrSquid> routingOptions = new TreeSet<MercuryOrSquid>();
        if (vessel != null) {

            Set<SampleInstance> sampleInstances = vessel.getSampleInstances(SampleType.ANY, LabBatchType.WORKFLOW);
            if (sampleInstances.isEmpty()) {
                routingOptions.add(SQUID);
            } else {
                Set<SampleInstance> possibleControls = new HashSet<SampleInstance>();
                for (SampleInstance sampleInstance : sampleInstances) {
                    if (sampleInstance.getProductOrderKey() == null) {
                        possibleControls.add(sampleInstance);
                    } else {
                        ProductOrder order = athenaClientService.retrieveProductOrderDetails(sampleInstance.getProductOrderKey());
                        routingOptions.add(getWorkflow(order.getProduct().getWorkflowName()).getRouting());
                    }
                }
                if (!possibleControls.isEmpty()) {

                    /*
                     * TODO: When querying a group of tubes, if everything might be a control (i.e., nothing has a PDO),
                     * don't bother checking controls; just route to Squid. This will avoid unnecessary BSP queries.
                     */

                    // TODO: change this logic if SampleInstance.controlRole is ever populated

                    // TODO: move this logic into ControlEjb?
                    List<Control> controls = controlDao.findAllActive();
                    List<String> controlSampleIds = new ArrayList<String>();
                    for (Control control : controls) {
                        controlSampleIds.add(control.getCollaboratorSampleId());
                    }

                    // Don't bother querying BSP if Mercury doesn't have any active controls.
                    if (controlSampleIds.isEmpty()) {
                        routingOptions.add(SQUID);
                    } else {
                        Collection<String> sampleNames = new ArrayList<String>();
                        for (SampleInstance sampleInstance : possibleControls) {
                            sampleNames.add(sampleInstance.getStartingSample().getSampleKey());
                        }
                        Map<String, BSPSampleDTO> sampleDTOs = bspSampleDataFetcher.fetchSamplesFromBSP(sampleNames);

                        for (SampleInstance possibleControl : possibleControls) {
                            String sampleKey = possibleControl.getStartingSample().getSampleKey();
                            BSPSampleDTO sampleDTO = sampleDTOs.get(sampleKey);
                            if (sampleDTO == null) {
                                // Don't know what this is, but it isn't for Mercury.
                                routingOptions.add(SQUID);
                            } else {
                                if (controlSampleIds.contains(sampleDTO.getCollaboratorsSampleName())) {
                                    routingOptions.add(BOTH);
                                } else {
                                    routingOptions.add(SQUID);
                                }
                            }
                        }
                    }
                }
            }
        }

        return evaluateRoutingOption(routingOptions);
    }

    private MercuryOrSquid getWorkflowRoutingForVessel(Collection<String> nearestProductOrders) {
        MercuryOrSquid routing = null;
        if (nearestProductOrders.isEmpty()) {
            routing = SQUID;
        } else {
            for (String productOrderKey : nearestProductOrders) {
                if (productOrderKey != null) {
                    ProductOrder order = athenaClientService.retrieveProductOrderDetails(productOrderKey);
                    if (order != null && StringUtils.isNotBlank(order.getProduct().getWorkflowName())) {
                        try {
                            routing = getWorkflow(order.getProduct().getWorkflowName()).getRouting();
                        } catch (WorkflowException e) {
                            routing = SQUID;
                        }
                    }
                }
            }
        }
        return routing;
    }

    /**
     * getWorkflowVersion will, based on the BusinessKey of a product order, find the defined Workflow Version.  It
     * does this by querying to the "Athena" side of Mercury for the ProductOrder Definition and looks up the
     * workflow definition based on the workflow name defined on the ProductOrder
     *
     * @param workflowName
     *
     * @return Workflow Definition for the defined workflow for the product order represented by productOrderKey
     */
    private ProductWorkflowDef getWorkflow(String workflowName) {

        WorkflowConfig workflowConfig = workflowLoader.load();

        return workflowConfig.getWorkflowByName(workflowName);
    }
}
