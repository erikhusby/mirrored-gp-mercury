package org.broadinstitute.gpinformatics.mercury.boundary.lims;

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
 * @author breilly
 */
public class MercuryOrSquidRouter {

    public enum MercuryOrSquid { MERCURY, SQUID }

    private TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

    @Inject
    public MercuryOrSquidRouter(TwoDBarcodedTubeDAO twoDBarcodedTubeDAO) {
        this.twoDBarcodedTubeDAO = twoDBarcodedTubeDAO;
    }

    public MercuryOrSquid routeForTubes(List<String> tubeBarcodes) {
        return SQUID;
    }

    public MercuryOrSquid routeForPlate(String plateBarcode) {
        return SQUID;
    }

    // TODO: figure out how to handle libraryNames for fetchLibraryDetailsByLibraryName

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
                if (sampleInstance.getStartingSample().getProductOrderKey() != null) {
                    return MERCURY;
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
                    if (sampleInstance.getStartingSample().getProductOrderKey() != null) {
                        foundAnyProductOrder = true;
                        return TraversalControl.StopTraversing;
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
