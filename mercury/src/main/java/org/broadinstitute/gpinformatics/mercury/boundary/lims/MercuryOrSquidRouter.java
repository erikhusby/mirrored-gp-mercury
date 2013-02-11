package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;

import javax.enterprise.inject.Default;
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
            Arrays.asList(WorkflowConfig.WorkflowName.EXOME_EXPRESS.getWorkflowName());

    public enum MercuryOrSquid {MERCURY, SQUID}

    private LabVesselDao labVesselDao;
    private AthenaClientService athenaClientService;

    MercuryOrSquidRouter() {
    }

    @Inject
    public MercuryOrSquidRouter(LabVesselDao labVesselDao, AthenaClientService athenaClientService) {
        this.labVesselDao = labVesselDao;
        this.athenaClientService = athenaClientService;
    }

    public MercuryOrSquid routeForVessels(List<String> barcodes) {
        for(String vesselBarcode: barcodes) {
            if(routeForVessel(vesselBarcode) == MERCURY) {
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
            /*
                        HasProductOrderCriteria criteria = new HasProductOrderCriteria();
                        tube.evaluateCriteria(criteria, Ancestors);
                        if (criteria.getFoundAnyProductOrder()) {
                            return MERCURY;
                        } else {
                            return SQUID;
                        }
            */
            for (SampleInstance sampleInstance : vessel.getSampleInstances()) {
                String productOrderKey = sampleInstance.getStartingSample().getProductOrderKey();
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

    public class HasProductOrderCriteria implements TransferTraverserCriteria {

        private boolean foundAnyProductOrder = false;

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            if (context.getLabVessel() != null) {
                for (SampleInstance sampleInstance : context.getLabVessel().getSampleInstances()) {
                    String productOrderKey = sampleInstance.getStartingSample().getProductOrderKey();
                    if (productOrderKey != null) {
                        ProductOrder order = athenaClientService.retrieveProductOrderDetails(productOrderKey);
                        if (order != null && MERCURY_PRODUCTS.contains(order.getProduct().getWorkflowName())) {
                            foundAnyProductOrder = true;
                            return TraversalControl.StopTraversing;
                        }
                    }
                }
            }
            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselInOrder(Context context) {}

        @Override
        public void evaluateVesselPostOrder(Context context) {}

        public boolean getFoundAnyProductOrder() {
            return foundAnyProductOrder;
        }
    }
}
