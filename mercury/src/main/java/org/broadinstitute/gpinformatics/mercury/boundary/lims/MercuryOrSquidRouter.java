package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
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

    private AthenaClientService athenaClientService;

    MercuryOrSquidRouter() {
    }

    @Inject
    public MercuryOrSquidRouter(AthenaClientService athenaClientService) {
        this.athenaClientService = athenaClientService;
    }

    public MercuryOrSquid routeForVessels(List<LabVessel> vessels) {
        for (LabVessel vessel : vessels) {
            if (routeForVessel(vessel) == MERCURY) {
                return MERCURY;
            }
        }
        return SQUID;
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
