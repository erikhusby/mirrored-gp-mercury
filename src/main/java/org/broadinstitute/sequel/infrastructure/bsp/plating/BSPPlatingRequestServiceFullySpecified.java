package org.broadinstitute.sequel.infrastructure.bsp.plating;

import java.util.List;


public interface BSPPlatingRequestServiceFullySpecified extends BSPPlatingRequestService {

    /**
     * Create a brand new BSP Plating Request. This is the fully specified
     * version of the method that allows the caller to supply plating request
     * options, useful for test code.
     *
     * @param options            Options to the plating service (user-selectable and/or
     *                           programmatically driven)
     * @param login              Platform PM login (should be the person currently logged into
     *                           Squid)
     * @param platingRequestName unique name to set into BSP plating request .
     * @param aliquots           List of aliquots desired, stored wherever BSP thinks best on
     *                           the output plate
     * @param controlWells       List of controls and the Wells they should inhabit on the
     *                           output plate
     * @return plating result
     */
    BSPPlatingRequestResult createPlatingRequest(
            BSPPlatingRequestOptions options,
            String login, String platingRequestName, String squidWorkRequestId,
            List<SeqWorkRequestAliquot> aliquots,
            List<ControlWell> controlWells, String comments,
            String seqTechnology, String humanReadableBarcode);


    /**
     * Update an existing plating request, typically to clear errors or
     * warnings. This is the fully specified version of the method that allows
     * the caller to supply plating request options, useful for test code.
     * Parameters are the same as #createPlatingRequest() except for the
     * addition of the plating WR barcode for the existing plating request.
     *
     * @param bspPlatingBarcode BSP Plating request barcode
     * @param options           Options to the plating service (user-selectable and/or
     *                          programmatically driven)
     * @param login             Platform PM login (should be the person currently logged into
     *                          Squid)
     * @param aliquots          List of aliquots desired, stored wherever BSP thinks best on
     *                          the output plate
     * @param controlWells      List of controls and the Wells they should inhabit on the
     *                          output plate
     * @return plating result
     */
    BSPPlatingRequestResult updatePlatingRequest(
            String bspPlatingBarcode,
            BSPPlatingRequestOptions options,
            String login,
            List<SeqWorkRequestAliquot> aliquots,
            List<ControlWell> controlWells);


}
