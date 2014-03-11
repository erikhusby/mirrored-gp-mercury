package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.bsp.client.response.SampleKitListResponse;
import org.broadinstitute.bsp.client.response.SampleKitReceiptResponse;
import org.broadinstitute.bsp.client.sample.Sample;
import org.broadinstitute.bsp.client.sample.SampleKit;
import org.broadinstitute.bsp.client.sample.SampleManager;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.samples.SampleReceiptValidation;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleReceiptService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ParentVesselBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.SampleReceiptBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.SampleReceiptResource;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.xml.bind.JAXBException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * EJB for receiving samples within BSP.
 *
 * The container test for this, ReceiveSamplesEjbTest, has been Disabled.  If this is re-introduced, enable it and add
 * more to it.
 */
@Stateful
@RequestScoped
public class ReceiveSamplesEjb {

    private BSPSampleReceiptService receiptService;
    private BSPManagerFactory managerFactory;
    private ProductOrderSampleDao productOrderSampleDao;
    private BSPUserList bspUserList;
    private SampleReceiptResource sampleReceiptResource;

    public ReceiveSamplesEjb() {
    }

    @Inject
    public ReceiveSamplesEjb(BSPSampleReceiptService receiptService,
                             BSPManagerFactory managerFactory,
                             ProductOrderSampleDao productOrderSampleDao,
                             BSPUserList bspUserList,
                             SampleReceiptResource sampleReceiptResource) {
        this.receiptService = receiptService;
        this.managerFactory = managerFactory;
        this.productOrderSampleDao = productOrderSampleDao;
        this.bspUserList = bspUserList;
        this.sampleReceiptResource = sampleReceiptResource;
    }

    /**
     * Handles the receipt validation, and the receiving of the samples passed in.  Utilizes the current user's username
     * to make sure they have the correct BSP privileges to do the receipt.
     *
     *
     * @param sampleIds         SampleIds of the samples to receive.
     * @param bspUser           The currently logged in user.
     * @param messageCollection Messages to send back to the user.
     *
     * @return SampleKitReceiptResponse received from BSP.
     */
    public SampleKitReceiptResponse receiveSamples(List<String> sampleIds, BspUser bspUser,
                                                   MessageCollection messageCollection) throws JAXBException {

        SampleKitReceiptResponse receiptResponse = null;

        // Validate first, if there are no errors receive first in BSP, then do the mercury receipt work.
        validateForReceipt(sampleIds, messageCollection, bspUser.getUsername());

        if (!messageCollection.hasErrors()) {

            try {
                receiptResponse = receiptService.receiveSamples(sampleIds, bspUser.getUsername());
            } catch (UnsupportedEncodingException e) {
                throw new InformaticsServiceException(e);
            }

            if (receiptResponse.isSuccess()) {

                for (Map.Entry<String, Set<SampleKitReceiptResponse.Barcodes>> entry :
                        receiptResponse.getReceivedSamplesPerKit().entrySet()) {

                    List<ParentVesselBean> parentVesselBeans = new ArrayList<>();
                    for (SampleKitReceiptResponse.Barcodes barcodes : entry.getValue()) {
                        parentVesselBeans.add(new ParentVesselBean(barcodes.getExternalBarcode(),
                                barcodes.getSampleBarcode(),
                                receiptResponse.getTubeTypePerSample().get(barcodes.getSampleBarcode()),
                                null));
                    }
                    SampleReceiptBean sampleReceiptBean = new SampleReceiptBean(new Date(), entry.getKey(),
                            parentVesselBeans, bspUser.getUsername());
                    sampleReceiptResource.notifyOfReceipt(sampleReceiptBean);
                }
            }
        }

        return receiptResponse;
    }

    /**
     * Run at the time of receipt, this method validates that samples received meet a certain criteria.  If any of the
     * criteria fails, the user will receive either a warning or an error.  This criteria includes:
     * <ul>
     * <li>Of the sample kits that encompass the collection of samples given, not all samples came back
     * together -- Warning.</li>
     * <li>Some of the samples returned are not found in BSP -- Error.</li>
     * <li>The collection of samples received represents more than one sample kit -- Error.</li>
     * </ul>
     *
     * @param sampleIds         IDs of the samples to be validated
     * @param messageCollection collection of errors and/or warnings to be returned to the user
     * @param operator          username of the person scanning in the received samples
     */
    public void validateForReceipt(Collection<String> sampleIds, MessageCollection messageCollection,
                                   String operator) {

        SampleManager bspSampleManager = managerFactory.createSampleManager();
        SampleKitListResponse sampleKitsBySampleIds = bspSampleManager.getSampleKitsBySampleIds(sampleIds);

        Map<SampleKit, Set<String>> sampleIDsBySampleKit = new HashMap<>();
        Set<String> allFoundSampleIds = new HashSet<>();

        for (SampleKit currentKit : sampleKitsBySampleIds.getResult()) {
            if (sampleIDsBySampleKit.get(currentKit) == null) {
                sampleIDsBySampleKit.put(currentKit, new HashSet<String>());
            }

            for (Sample currentKitSample : currentKit.getSamples()) {
                sampleIDsBySampleKit.get(currentKit).add(currentKitSample.getSampleId());
                allFoundSampleIds.add(currentKitSample.getSampleId());
            }
        }

        Map<String, Set<ProductOrderSample>> associatedProductOrderSamples =
                productOrderSampleDao.findMapBySamples(sampleIds);

        // Check to see if received samples span more than one sample kit

        if (sampleKitsBySampleIds.getResult().size() > 1) {
            // Samples span multiple sample kits

            // Get all associated Product order samples
            // add blocker errors
            addMultipleKitValidationViolations(messageCollection, operator, associatedProductOrderSamples);
        }

        // Check to see if any samples received are not currently recognized by BSP
        Set<String> requestedIdsLessFoundIds = new HashSet<>(sampleIds);
        requestedIdsLessFoundIds.removeAll(allFoundSampleIds);

        // add blocker errors for unrecognized samples
        addSampleNotFoundValidationViolations(messageCollection, operator, associatedProductOrderSamples,
                requestedIdsLessFoundIds);

        // List warnings for all samples received which do not complete
        for (Map.Entry<SampleKit, Set<String>> currentSampleKitEntry : sampleIDsBySampleKit.entrySet()) {

            List<String> sampleKitIdsMinusRequestedIds = new ArrayList<>(currentSampleKitEntry.getValue());
            List<String> unionSampleKitAndRequestedSampleIDs = new ArrayList<>(currentSampleKitEntry.getValue());

            // Remove all sample IDs from the sample ID's of the found sample kit
            sampleKitIdsMinusRequestedIds.removeAll(sampleIds);

            // If there are any samples left over, they were in the kit but not in the receipt scan
            if (!sampleKitIdsMinusRequestedIds.isEmpty()) {
                unionSampleKitAndRequestedSampleIDs.removeAll(sampleKitIdsMinusRequestedIds);
                // Get all associated Product order samples for missing samples

                // add warning
                addMissingSampleFromKitValidationViolations(messageCollection, operator, associatedProductOrderSamples,
                        currentSampleKitEntry, unionSampleKitAndRequestedSampleIDs);
            }
        }
    }

    /**
     * For a given collection of sample ids, this method will apply the validation violation that not all samples for
     * an associated sample kit were received.  Not only will the error message be returned for processing, but the
     * receipt validation will be applied to the appropriate product order sample(s)
     *
     * @param messageCollection             collection of errors and/or warnings to be returned to the user
     * @param operator                      username of the person scanning in the received samples
     * @param associatedProductOrderSamples map of ProductOrderSamples to be processed indexed by their respective
     *                                      sample ID
     * @param currentSampleKitEntry         Map entry of found Sample kits referencing the IDs of the samples that are
     *                                      found within the sample kits
     * @param sampleIDsInSampleKit          Collection of Sample IDs for which validation errors are to be applied
     */
    private void addMissingSampleFromKitValidationViolations(MessageCollection messageCollection, String operator,
                                                             Map<String, Set<ProductOrderSample>> associatedProductOrderSamples,
                                                             Map.Entry<SampleKit, Set<String>> currentSampleKitEntry,
                                                             List<String> sampleIDsInSampleKit) {
        for (String currentSampleId : sampleIDsInSampleKit) {

            addValidation(messageCollection, operator, associatedProductOrderSamples.get(currentSampleId),
                    SampleReceiptValidation.SampleValidationReason.MISSING_SAMPLE_FROM_SAMPLE_KIT,
                    SampleReceiptValidation.SampleValidationType.WARNING,
                    currentSampleId + ": kit " + currentSampleKitEntry.getKey().getSampleKitId() + " "
                    + SampleReceiptValidation.SampleValidationReason
                            .MISSING_SAMPLE_FROM_SAMPLE_KIT
                            .getReasonMessage());

        }
    }

    /**
     * For a given collection of sample ids, this method will apply the validation violation that the sample received
     * is not recognized in BSP.  Not only will the error message be returned for processing, but the
     * receipt validation will be applied to the appropriate product order sample(s)
     *
     * @param messageCollection             collection of errors and/or warnings to be returned to the user
     * @param operator                      username of the person scanning in the received samples
     * @param associatedProductOrderSamples map of ProductOrderSamples to be processed indexed by their respective
     *                                      sample ID
     * @param requestedIdsLessFoundIds      Collection of the sample IDs that are not associated with any sample kit
     */
    private void addSampleNotFoundValidationViolations(MessageCollection messageCollection, String operator,
                                                       Map<String, Set<ProductOrderSample>> associatedProductOrderSamples,
                                                       Set<String> requestedIdsLessFoundIds) {
        for (String currentSampleIdNotFound : requestedIdsLessFoundIds) {
            addValidation(messageCollection, operator, associatedProductOrderSamples.get(currentSampleIdNotFound),
                    SampleReceiptValidation.SampleValidationReason.SAMPLE_NOT_IN_BSP,
                    SampleReceiptValidation.SampleValidationType.BLOCKING,
                    currentSampleIdNotFound + ": " + SampleReceiptValidation.SampleValidationReason
                            .SAMPLE_NOT_IN_BSP
                            .getReasonMessage());
        }
    }

    /**
     * For a given collection of sample ids, this method will apply the validation violation that there are multiple
     * sample kits for the collection of samples received.  Not only will the error message be returned for processing,
     * but the receipt validation will be applied to the appropriate product order sample(s)
     *
     * @param messageCollection             collection of errors and/or warnings to be returned to the user
     * @param operator                      username of the person scanning in the received samples
     * @param associatedProductOrderSamples map of ProductOrderSamples to be processed indexed by their respective
     *                                      sample ID
     */
    private void addMultipleKitValidationViolations(MessageCollection messageCollection, String operator,
                                                    Map<String, Set<ProductOrderSample>> associatedProductOrderSamples) {
        for (Map.Entry<String, Set<ProductOrderSample>> entry : associatedProductOrderSamples.entrySet()) {
            //TODO SGM: Temporarily setting this to a warning since the ability to clear blocking errors will not be ready for this sprint
            addValidation(messageCollection, operator, entry.getValue(),
                    SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS,
                    SampleReceiptValidation.SampleValidationType.WARNING, String.format(
                    "%s: " + SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS
                            .getReasonMessage(), entry.getKey()));
        }
    }

    /**
     * Helper method to apply validation errors based on a set of parameters defined at call time.
     *
     * @param messageCollection collection of errors and/or warnings to be returned to the user
     * @param operator          username of the person scanning in the received samples
     * @param sampleCollection  collection of ProductOrderSamples to be processed
     * @param validationReason  Reason that this validation is being applied
     * @param validationType    type of validation to apply
     * @param message           readable message to associate with the Validation error
     */
    private void addValidation(MessageCollection messageCollection, String operator,
                               Collection<ProductOrderSample> sampleCollection,
                               SampleReceiptValidation.SampleValidationReason validationReason,
                               SampleReceiptValidation.SampleValidationType validationType, String message) {
        for (ProductOrderSample sample : sampleCollection) {
            sample.addValidation(new SampleReceiptValidation(bspUserList.getByUsername(operator).getUserId(),
                    validationType,
                    validationReason));
            switch (validationType) {
            case WARNING:
                messageCollection.addWarning(message);
                break;
            case BLOCKING:
                messageCollection.addError(message);
                break;
            }
        }
    }
}
