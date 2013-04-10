package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter.MercuryOrSquid.MERCURY;
import static org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter.MercuryOrSquid.SQUID;

/**
 * Utility for routing messages and queries to Mercury or Squid as determined by the supplied sample containers.
 * Defines the notion of a vessel belonging to either Mercury or Squid.
 * <p>
 * The current definition of "belonging" to Mercury is that any sample in the tube is associated with an
 * Exome Express product order.
 */
public class MercuryOrSquidRouter implements Serializable {

    /**
     * Names of products that Mercury should handle queries and messaging for.
     */
    public static final Collection<String> MERCURY_PRODUCTS =
            Arrays.asList(WorkflowName.EXOME_EXPRESS.getWorkflowName());

    public enum MercuryOrSquid {MERCURY, SQUID}

    private LabVesselDao        labVesselDao;
    private AthenaClientService athenaClientService;

    MercuryOrSquidRouter() {
    }

    @Inject
    public MercuryOrSquidRouter(LabVesselDao labVesselDao, AthenaClientService athenaClientService) {
        this.labVesselDao = labVesselDao;
        this.athenaClientService = athenaClientService;
    }

    /**
     * Building on {@link #routeForVessel(String)}, this method takes a list of barcodes for which a user wishes to
     * determine the system of record.  The work is delegated to {@link #routeForVessel(String)}
     *
     * @param barcodes
     *
     * @return
     */
    public MercuryOrSquid routeForVessels(List<String> barcodes) {
        for (String vesselBarcode : barcodes) {
            if (routeForVessel(vesselBarcode) == MERCURY) {
                return MERCURY;
            }
        }
        return SQUID;
    }

    /**
     * Determines if a tube belongs to Mercury or Squid. See {@link MercuryOrSquid} for a description of "belongs".
     *
     * @param barcode the barcode of the tube to check
     *
     * @return system that should process messages/queries for the tube
     */
    public MercuryOrSquid routeForVessel(String barcode) {
        LabVessel vessel = labVesselDao.findByIdentifier(barcode);
        return routeForVessel(vessel);
    }

    // TODO: figure out how to handle libraryNames for fetchLibraryDetailsByLibraryName

    public MercuryOrSquid routeForVessel(LabVessel vessel) {
        if (vessel != null) {
            for (String productOrderKey : vessel.getNearestProductOrders()) {
                if (productOrderKey != null) {
                    ProductOrder order = athenaClientService.retrieveProductOrderDetails(productOrderKey);
                    if (order != null && MERCURY_PRODUCTS.contains(order.getProduct().getWorkflowName())) {
                        return MERCURY;
                    }
                }
            }
        }
        return SQUID;
    }
}
