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

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EJB for receiving samples within BSP.
 */
@Stateful
@RequestScoped
public class ReceiveSamplesEjb {

    @Inject
    private BSPSampleReceiptService receiptService;

    @Inject
    private BSPManagerFactory managerFactory;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    @Inject
    private BSPUserList bspUserList;

    /**
     * Handles the receipt validation, and the receiving of the samples passed in.  Utilizes the current user's username
     * to make sure they have the correct BSP privileges to do the receipt.
     *
     * @param sampleIds         Barcodes of the samples to receive.
     * @param username          Username of the currently logged in user.
     * @param messageCollection Messages to send back to the user.
     * @return SampleKitReceiptResponse received from BSP.
     */
    public SampleKitReceiptResponse receiveSamples(List<String> sampleIds, String username,
                                                   MessageCollection messageCollection) {

        SampleKitReceiptResponse receiptResponse = null;

        // Validate first, if there are no errors receive first in BSP, then do the mercury receit work.
        validateForReceipt(sampleIds, messageCollection, username);

        if (!messageCollection.hasErrors()) {

            receiptResponse = receiptService.receiveSamples(sampleIds, username);

            if (receiptResponse.isSuccess()) {
                // TODO: Call the Mercury receipt registration code for the samples.
            }
        }

        return receiptResponse;
    }

    private void validateForReceipt(Collection<String> sampleInfos, MessageCollection messageCollection,
                                    String operator) {

        SampleManager bspSampleManager = managerFactory.createSampleManager();
        SampleKitListResponse sampleKitsBySampleIds = bspSampleManager.getSampleKitsBySampleIds(
                new ArrayList<>(sampleInfos));

        Map<SampleKit, List<String>> sampleIDsBySamplekit = new HashMap<>();
        List<String> totalSamplesFound = new ArrayList<>();

        for (SampleKit currentKit : sampleKitsBySampleIds.getResult()) {
            if (sampleIDsBySamplekit.get(currentKit) == null) {
                sampleIDsBySamplekit.put(currentKit, new ArrayList<String>());
            }

            for (Sample currentKitSample : currentKit.getSamples()) {
                sampleIDsBySamplekit.get(currentKit).add(currentKitSample.getSampleId());
                totalSamplesFound.add(currentKitSample.getSampleId());
            }
        }

        if (sampleKitsBySampleIds.getResult().size() > 1) {
            //Samples span multiple sample kits

            //Get all associated Product order samples
            Map<String, List<ProductOrderSample>> associatedProductOrderSamples =
                    productOrderSampleDao.findMapBySamples(new ArrayList<>(sampleInfos));

            //add blocker errors
            for (Map.Entry<String, List<ProductOrderSample>> entries : associatedProductOrderSamples.entrySet()) {
                for (ProductOrderSample currentPOSample : entries.getValue()) {
                    currentPOSample
                            .addValidation(new SampleReceiptValidation(bspUserList.getByUsername(operator).getUserId(),
                                    SampleReceiptValidation.SampleValidationType.BLOCKING,
                                    SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS));

                    messageCollection.addError(
                            "%s: " + SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS
                                    .getReasonMessage(), entries.getKey());
                }
            }

        } else {


            List<String> sourceIdsClone = new ArrayList<>(sampleInfos);

            sourceIdsClone.removeAll(sampleIDsBySamplekit.get(sampleIDsBySamplekit.keySet().iterator().next()));
            //Get all associated Product order samples
            Map<String, List<ProductOrderSample>> notFoundProductOrderSamples =
                    productOrderSampleDao.findMapBySamples(sourceIdsClone);
            //add blocker errors for unrecognized samples
            for (Map.Entry<String, List<ProductOrderSample>> entries : notFoundProductOrderSamples.entrySet()) {
                for (ProductOrderSample currentPOSample : entries.getValue()) {
                    currentPOSample
                            .addValidation(new SampleReceiptValidation(bspUserList.getByUsername(operator).getUserId(),
                                    SampleReceiptValidation.SampleValidationType.BLOCKING,
                                    SampleReceiptValidation.SampleValidationReason.SAMPLE_NOT_IN_BSP));
                    messageCollection.addError(
                            "%s: " + SampleReceiptValidation.SampleValidationReason.SAMPLE_NOT_IN_BSP
                                    .getReasonMessage(), entries.getKey());
                }
            }

            Collection<String> sampleIdDiff = sampleIDsBySamplekit.get(sampleIDsBySamplekit.keySet().iterator().next());

            sampleIdDiff.removeAll(sampleInfos);

            //Get all associated Product order samples for missing samples
            Map<String, List<ProductOrderSample>> notReceivedProductOrderSamples =
                    productOrderSampleDao.findMapBySamples(sourceIdsClone);

            //add warning
            for (Map.Entry<String, List<ProductOrderSample>> entries : notFoundProductOrderSamples.entrySet()) {
                for (ProductOrderSample currentPOSample : entries.getValue()) {
                    currentPOSample
                            .addValidation(new SampleReceiptValidation(bspUserList.getByUsername(operator).getUserId(),
                                    SampleReceiptValidation.SampleValidationType.WARNING,
                                    SampleReceiptValidation.SampleValidationReason.MISSING_SAMPLE_FROM_SAMPLE_KIT));

                    messageCollection.addWarning(
                            "%s: " + SampleReceiptValidation.SampleValidationReason.MISSING_SAMPLE_FROM_SAMPLE_KIT
                                    .getReasonMessage(), entries.getKey());
                }
            }
        }
    }
}
