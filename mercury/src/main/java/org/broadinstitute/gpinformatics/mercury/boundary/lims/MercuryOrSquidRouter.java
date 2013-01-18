package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.inject.Inject;
import java.util.List;

import static org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter.MercuryOrSquid.MERCURY;
import static org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter.MercuryOrSquid.SQUID;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria.TraversalDirection.Ancestors;

/**
 * Utility for routing messages and queries to Mercury or Squid as determined by the supplied sample containers.
 * Defines the notion of a vessel belonging to either Mercury or Squid.
 * <p>
 * The current definition of "belonging" to Mercury is that any sample in the tube is associated with an
 * Exome Express product order.
 */
public class MercuryOrSquidRouter {

    public enum MercuryOrSquid { MERCURY, SQUID }

    private TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

    private ProductOrderDao productOrderDao;

    @Inject
    public MercuryOrSquidRouter(TwoDBarcodedTubeDAO twoDBarcodedTubeDAO, ProductOrderDao productOrderDao) {
        this.twoDBarcodedTubeDAO = twoDBarcodedTubeDAO;
        this.productOrderDao = productOrderDao;
    }

    public MercuryOrSquid routeForTubes(List<String> tubeBarcodes) {
        return SQUID;
    }

    public MercuryOrSquid routeForPlate(String plateBarcode) {
        return SQUID;
    }

    // TODO: figure out how to handle libraryNames for fetchLibraryDetailsByLibraryName

    /**
     * Determines if a tube belongs to Mercury or Squid. See {@link MercuryOrSquid} for a description of "belongs".
     *
     * @param tubeBarcode    the barcode of the tube to check
     * @return system that should process messages/queries for the tube
     */
    public MercuryOrSquid routeForTube(String tubeBarcode) {
        TwoDBarcodedTube tube = twoDBarcodedTubeDAO.findByBarcode(tubeBarcode);
        if (tube != null) {
/*
            HasProductOrderCriteria criteria = new HasProductOrderCriteria();
            tube.evaluateCriteria(criteria, Ancestors);
            if (criteria.getFoundAnyProductOrder()) {
                return MERCURY;
            } else {
                return SQUID;
            }
*/
            for (SampleInstance sampleInstance : tube.getSampleInstances()) {
                String productOrderKey = sampleInstance.getStartingSample().getProductOrderKey();
                if (productOrderKey != null) {
                    ProductOrder order = productOrderDao.findByBusinessKey(productOrderKey);
                    if (order != null && order.getProduct().getProductName().equals("Exome Express")) {
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
        public TraversalControl evaluateVesselPreOrder(LabVessel labVessel, LabEvent labEvent, int hopCount) {
            if (labVessel != null) {
                for (SampleInstance sampleInstance : labVessel.getSampleInstances()) {
                    String productOrderKey = sampleInstance.getStartingSample().getProductOrderKey();
                    if (productOrderKey != null) {
                        ProductOrder order = productOrderDao.findByBusinessKey(productOrderKey);
                        if (order != null && order.getProduct().getProductName().equals("Exome Express")) {
                            foundAnyProductOrder = true;
                            return TraversalControl.StopTraversing;
                        }
                    }
                }
            }
            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselInOrder(LabVessel labVessel, LabEvent labEvent, int hopCount) {}

        @Override
        public void evaluateVesselPostOrder(LabVessel labVessel, LabEvent labEvent, int hopCount) {}

        public boolean getFoundAnyProductOrder() {
            return foundAnyProductOrder;
        }
    }
}
