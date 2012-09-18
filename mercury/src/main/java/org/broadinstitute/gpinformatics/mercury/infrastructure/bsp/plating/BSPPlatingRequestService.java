package org.broadinstitute.gpinformatics.mercury.infrastructure.bsp.plating;


import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingRequest;

import java.io.Serializable;
import java.util.List;


/**
 * Versions of BSP Plating Service method intended to be called by Work Request
 * code. BSP connection parameters are pulled from Maven profiles, plating
 * request options are defaulted.
 */
public interface BSPPlatingRequestService extends Serializable {

    /**
     * Create a brand new BSP Plating Request and attempt to submit it.
     *
     * @param options              Options to the plating service (user-selectable and/or
     *                             programmatically driven)
     * @param login                login for the Platform PM issuing the plating request
     * @param platingRequestName   unique name to set into BSP plating request .
     * @param aliquots             List of aliquots desired, stored wherever BSP thinks best on
     *                             the output plate
     * @param controlWells         List of controls and the Wells they should inhabit on
     *                             the output plate
     * @param comments
     * @param seqTechnology
     * @param humanReadableBarcode
     * @return plating result
     */
    BSPPlatingRequestResult createPlatingRequest(
            BSPPlatingRequestOptions options,
            String login,
            String platingRequestName,
            List<SeqWorkRequestAliquot> aliquots,
            List<ControlWell> controlWells, String comments, String seqTechnology, String humanReadableBarcode);


    BSPPlatingRequestResult issueBSPPlatingRequest(BSPPlatingRequestOptions options, List<BSPPlatingRequest> requests,
                                                   List<ControlWell> controlWells,
                                                   String login,
                                                   String platingRequestName,
                                                   String comments, String seqTechnology, String humanReadableBarcode)
            throws Exception;

    /**
     * Update an existing plating request, typically to clear errors or warnings
     * by changing the set of samples, desired volume or concentration for the
     * aliquots, or the quotes associated with the plating of the aliquots.
     * Parameters are the same as #createPlatingRequest() except for the
     * addition of the plating WR barcode for the existing plating request.
     * This code will attempt to submit the plating WR if it is error free.
     *
     * @param platingRequestReceipt BSP Plating request receipt
     * @param login                 login for the Platform PM issuing the plating request
     * @param aliquots              List of aliquots desired, stored wherever BSP thinks best on
     *                              the output plate
     * @param controlWells          List of controls and the Wells they should inhabit on the
     *                              output plate
     * @return plating result
     */
    BSPPlatingRequestResult updatePlatingRequest(
            String platingRequestReceipt,
            BSPPlatingRequestOptions options,
            String login,
            List<SeqWorkRequestAliquot> aliquots,
            List<ControlWell> controlWells);

    BSPPlatingRequestOptions getBSPPlatingRequestDefaultOptions();


}
