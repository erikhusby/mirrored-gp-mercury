package org.broadinstitute.sequel.infrastructure.bsp.plating;


import org.broadinstitute.sequel.entity.project.JiraTicket;

import java.util.List;


/**
 * Versions of BSP Plating Service method intended to be called by Work Request
 * code. BSP connection parameters are pulled from Maven profiles, plating
 * request options are defaulted.
 *
 * @author mcovarr
 */
public interface BSPPlatingRequestService {

    /**
     * Create a brand new BSP Plating Request and attempt to submit it.
     *
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
            String login,
            String platingRequestName,
            String squidWorkRequestName,
            List<SeqWorkRequestAliquot> aliquots,
            List<ControlWell> controlWells, String comments, String seqTechnology, String humanReadableBarcode);

    /**
     * Create a brand new BSP Plating Request and attempt to submit it.
     *
     * @param options            Options to the plating service (user-selectable and/or
     *                           programmatically driven)
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
            String squidWorkRequestName,
            List<SeqWorkRequestAliquot> aliquots,
            List<ControlWell> controlWells, String comments, String seqTechnology, String humanReadableBarcode);

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
            String login,
            List<SeqWorkRequestAliquot> aliquots,
            List<ControlWell> controlWells);


    /**
     * Set the Squid WR id into an existing BSP Plating Work Request
     *
     * @param platingRequestReceipt BSP Plating request receipt
     * @param squidWorkRequestId
     */
    void setSquidWorkRequestId(String platingRequestReceipt, long squidWorkRequestId);


    /**
     * Set the human readable barcode for the specified plate in the specified BSP plating request.
     * For current Tripod work there will be only one plate per plating work request, so this method
     * should only be invoked once per plating work request with the plateIndex set to 1.
     *
     * @param platingRequestReceipt The receipt by which to lookup the BSP plating request
     * @param plateIndex            1-based index of the plate whose human-readable barcode we are setting.
     * @param humanReadableText     The text that should appear on the barcode for this plate
     */
    void setHumanReadableBarcode(String platingRequestReceipt, int plateIndex, String humanReadableText);


    /**
     * Try to resubmit this plating WR again in the hope that previously encountered errors have now
     * been cleared "out of band".  Examples of such errors would be missing collaborator definitions
     * in Squid or internal PI definitions in BSP.  In these cases there's nothing actually wrong
     * with the plating request, but there are things wrong with the outside world that need to be
     * corrected for the plating request to be successfully submitted.
     *
     * @param platingRequestReceipt
     * @return
     */
    BSPPlatingRequestResult tryAgain(String platingRequestReceipt);

    /**
     * Generate the URL to link to the appropriate page in BSP for the specified
     * plating WR barcode.
     *
     * @param platingRequestReceipt BSP Plating request receipt
     * @return
     */
    String generateLinkToBSPPlatingRequestPage(String platingRequestReceipt);


    /**
     * Return the LcSet JIRA key used to format the human readable barcode
     *
     * @param jiraTicket
     * @return
     */
    String getLcSetJiraKey(JiraTicket jiraTicket);

    BSPPlatingRequestOptions getBSPPlatingRequestDefaultOptions();

}
