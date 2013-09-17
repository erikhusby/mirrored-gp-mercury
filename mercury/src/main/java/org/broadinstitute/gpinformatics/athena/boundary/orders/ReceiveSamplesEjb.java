package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.bsp.client.response.SampleKitListResponse;
import org.broadinstitute.bsp.client.response.SampleKitReceiptResponse;
import org.broadinstitute.bsp.client.sample.Sample;
import org.broadinstitute.bsp.client.sample.SampleKit;
import org.broadinstitute.bsp.client.sample.SampleManager;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.samples.SampleReceiptValidation;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleReceiptService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * EJB for receiving samples within BSP.
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
     * @param sampleIds         Barcodes of the samples to receive.
     * @param username          Username of the currently logged in user.
     * @param messageCollection Messages to send back to the user.
     *
     * @return SampleKitReceiptResponse received from BSP.
     */
    public SampleKitReceiptResponse receiveSamples(List<String> sampleIds, String username,
                                                   MessageCollection messageCollection) throws JAXBException {

        SampleKitReceiptResponse receiptResponse = null;

        // Validate first, if there are no errors receive first in BSP, then do the mercury receipt work.
        validateForReceipt(sampleIds, messageCollection, username);

        if (!messageCollection.hasErrors()) {

            try {
                receiptResponse = receiptService.receiveSamples(sampleIds, username);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
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
                            parentVesselBeans, username);
                    sampleReceiptResource.notifyOfReceipt(sampleReceiptBean);
                }
            }
        }

        return receiptResponse;
    }

    /**
     * Run at the time of receipt, this method validates that samples receive meet a certain criteria.  If any of the
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
        SampleKitListResponse sampleKitsBySampleIds = bspSampleManager.getSampleKitsBySampleIds(
                new ArrayList<>(sampleIds));

        Map<SampleKit, List<String>> sampleIDsBySampleKit = new HashMap<>();
        List<String> allFoundSampleIds = new ArrayList<>();

        for (SampleKit currentKit : sampleKitsBySampleIds.getResult()) {
            if (sampleIDsBySampleKit.get(currentKit) == null) {
                sampleIDsBySampleKit.put(currentKit, new ArrayList<String>());
            }

            for (Sample currentKitSample : currentKit.getSamples()) {
                sampleIDsBySampleKit.get(currentKit).add(currentKitSample.getSampleId());
                allFoundSampleIds.add(currentKitSample.getSampleId());
            }
        }

        Map<String, Set<ProductOrderSample>> associatedProductOrderSamples =
                productOrderSampleDao.findMapBySamples(new ArrayList<>(sampleIds));

        /*
            Check to see if received samples span more than one sample kit
        */
        if (sampleKitsBySampleIds.getResult().size() > 1) {
            //Samples span multiple sample kits

            //Get all associated Product order samples
            //add blocker errors
            for (Map.Entry<String, Set<ProductOrderSample>> entries : associatedProductOrderSamples.entrySet()) {
                for (ProductOrderSample currentPOSample : entries.getValue()) {
                    //TODO SGM: Temporarily setting this to a warning since the ability to clear blocking errors will not be ready for this sprint
                    currentPOSample
                            .addValidation(new SampleReceiptValidation(bspUserList.getByUsername(operator).getUserId(),
                                    SampleReceiptValidation.SampleValidationType.WARNING,
                                    SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS));

                    messageCollection.addWarning(
                            "%s: " + SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS
                                    .getReasonMessage(), entries.getKey());
                }
            }
        }

        /*
            Check to see if any samples received are not currently recognized by BSP
         */
        List<String> requestedIdsLessFoundIds = new ArrayList<>(sampleIds);
        requestedIdsLessFoundIds.removeAll(allFoundSampleIds);

        //add blocker errors for unrecognized samples
        for (String currentSampleIdNotFound : requestedIdsLessFoundIds) {
            for (ProductOrderSample currentPOSample : associatedProductOrderSamples.get(currentSampleIdNotFound)) {
                currentPOSample
                        .addValidation(new SampleReceiptValidation(bspUserList.getByUsername(operator).getUserId(),
                                SampleReceiptValidation.SampleValidationType.BLOCKING,
                                SampleReceiptValidation.SampleValidationReason.SAMPLE_NOT_IN_BSP));
                messageCollection.addError(
                        "%s: " + SampleReceiptValidation.SampleValidationReason.SAMPLE_NOT_IN_BSP
                                .getReasonMessage(), currentSampleIdNotFound);
            }
        }

        //List warnings for all samples received which do not complete
        for (Map.Entry<SampleKit, List<String>> currentSampleKit : sampleIDsBySampleKit.entrySet()) {

            List<String> cloneSampleKitMembers = new ArrayList<>(currentSampleKit.getValue());
            List<String> secondCloneSampleKitMembers = new ArrayList<>(currentSampleKit.getValue());

            //Remove all sample IDs from the sample ID's of the found sample kit
            cloneSampleKitMembers.removeAll(sampleIds);

            //If there are any samples left over, they were in the kit but not in the receipt scan
            if (!cloneSampleKitMembers.isEmpty()) {
                secondCloneSampleKitMembers.removeAll(cloneSampleKitMembers);
                //Get all associated Product order samples for missing samples

                //add warning
                for (String currentSampleId : secondCloneSampleKitMembers) {
                    for (ProductOrderSample currentPOSample : associatedProductOrderSamples.get(currentSampleId)) {
                        currentPOSample
                                .addValidation(
                                        new SampleReceiptValidation(bspUserList.getByUsername(operator).getUserId(),
                                                SampleReceiptValidation.SampleValidationType.WARNING,
                                                SampleReceiptValidation.SampleValidationReason.MISSING_SAMPLE_FROM_SAMPLE_KIT));

                        messageCollection.addWarning(
                                "%s: kit %s " + SampleReceiptValidation.SampleValidationReason
                                        .MISSING_SAMPLE_FROM_SAMPLE_KIT
                                        .getReasonMessage(), currentSampleId,
                                currentSampleKit.getKey().getSampleKitId());
                    }
                }
            }
        }
    }
}
