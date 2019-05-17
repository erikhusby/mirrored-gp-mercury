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
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.QueueEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules.PicoEnqueueOverride;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ChildVesselBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ParentVesselBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.SampleInfo;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.SampleKitInfo;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.SampleKitReceivedBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.SampleReceiptBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.SampleReceiptResource;
import org.broadinstitute.gpinformatics.mercury.control.vessel.BSPRestService;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.WellAndSourceTubeType;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueOrigin;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueSpecialization;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

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
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private BSPRestService bspRestService;
    private LabVesselDao labVesselDao;
    private QueueEjb queueEjb;

    public ReceiveSamplesEjb() {
    }

    @Inject
    public ReceiveSamplesEjb(BSPSampleReceiptService receiptService,
                             BSPManagerFactory managerFactory,
                             ProductOrderSampleDao productOrderSampleDao,
                             BSPUserList bspUserList,
                             BSPRestService bspRestService,
                             SampleReceiptResource sampleReceiptResource,
                             LabVesselDao labVesselDao,
                             QueueEjb queueEjb) {
        this.receiptService = receiptService;
        this.managerFactory = managerFactory;
        this.productOrderSampleDao = productOrderSampleDao;
        this.bspUserList = bspUserList;
        this.sampleReceiptResource = sampleReceiptResource;
        this.bspRestService = bspRestService;
        this.labVesselDao = labVesselDao;
        this.queueEjb = queueEjb;
    }

    /**
     * Handles the receipt validation, and the receiving of the samples passed in.  Utilizes the current user's username
     * to make sure they have the correct BSP privileges to do the receipt.
     *
     *
     * @param sampleKitBarcode  SK Barcode to receive
     * @param sampleIds         Selected sample ids to receive
     * @param wellAndTubes      Well/Tube pairs from rack scan for associated selected samples
     * @param bspUser           The currently logged in user.
     * @param messageCollection Messages to send back to the user.
     *
     * @return SampleKitReceiptResponse received from BSP.
     */
    public SampleKitReceivedBean receiveByKitScan(String sampleKitBarcode, List<String> sampleIds,
                                                  List<WellAndSourceTubeType> wellAndTubes, BspUser bspUser,
                                                  MessageCollection messageCollection) {

        SampleKitReceivedBean receiptResponse = null;

        // Validate first, if there are no errors receive first in BSP, then do the mercury receipt work.
        validateForReceipt(sampleIds, messageCollection, bspUser.getUsername());

        if (!messageCollection.hasErrors()) {

            receiptResponse = bspRestService.receiveByKitScan(sampleKitBarcode, wellAndTubes, bspUser.getUsername());

            Map<String, String> mapBarcodeToWell = wellAndTubes.stream()
                    .collect(Collectors.toMap(WellAndSourceTubeType::getTubeBarcode, WellAndSourceTubeType::getWellName));

            if (receiptResponse.isSuccess()) {
                for (SampleKitReceivedBean.KitInfo kitInfo: receiptResponse.getReceivedSamplesPerKit()) {

                    Map<String, String> sampleToType = receiptResponse.getTubeTypePerSample()
                            .stream().collect(Collectors.toMap(SampleKitReceivedBean.TubeTypePerSample::getSampleId,
                                    SampleKitReceivedBean.TubeTypePerSample::getTubeType));
                    List<ChildVesselBean> childVesselBeans = new ArrayList<>();
                    ParentVesselBean parentVesselBean = new ParentVesselBean(kitInfo.getKitId(), null,
                            kitInfo.getReceptacleType(), childVesselBeans);
                    for (SampleKitReceivedBean.SampleBarcodes barcode : kitInfo.getSampleBarcodes()) {
                        String tubeType = sampleToType.get(barcode.getSampleBarcode());
                        String well = mapBarcodeToWell.get(barcode.getExternalBarcode());
                        childVesselBeans.add(new ChildVesselBean(
                                barcode.getExternalBarcode(), barcode.getSampleBarcode(), tubeType,well));
                    }
                    List<ParentVesselBean> parentVesselBeans = new ArrayList<>();
                    parentVesselBeans.add(parentVesselBean);
                    SampleReceiptBean sampleReceiptBean = new SampleReceiptBean(new Date(), kitInfo.getKitId(),
                            parentVesselBeans, bspUser.getUsername());
                    sampleReceiptResource.notifyOfReceipt(sampleReceiptBean);
                }
            } else {
                messageCollection.addErrors(receiptResponse.getMessages());
            }
        }

        return receiptResponse;
    }

    /**
     * Handles the receipt validation, and the receiving of the samples passed in.  Utilizes the current user's username
     * to make sure they have the correct BSP privileges to do the receipt.
     *
     *
     * @param mapSktoKitInfo    Map SampleIds to kits of the samples to receive.
     * @param bspUser           The currently logged in user.
     * @param messageCollection Messages to send back to the user.
     *
     * @return SampleKitReceiptResponse received from BSP.
     */
    public SampleKitReceiptResponse receiveSamples(Map<String, SampleKitInfo> mapSktoKitInfo, List<String> sampleIds,
                                                   BspUser bspUser, MessageCollection messageCollection) throws JAXBException {

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
                    SampleKitInfo sampleKitInfo = mapSktoKitInfo.get(entry.getKey());
                    List<ParentVesselBean> parentVesselBeans = new ArrayList<>();
                    if (sampleKitInfo == null || !sampleKitInfo.getPlate()) {
                        for (SampleKitReceiptResponse.Barcodes barcodes : entry.getValue()) {
                            parentVesselBeans.add(new ParentVesselBean(barcodes.getExternalBarcode(),
                                    barcodes.getSampleBarcode(),
                                    receiptResponse.getTubeTypePerSample().get(barcodes.getSampleBarcode()),
                                    null));
                        }
                    } else {
                        Map<String, SampleInfo> mapSampleToInfo = sampleKitInfo.getSampleInfos().stream()
                                .collect(Collectors.toMap(SampleInfo::getSampleId, Function.identity()));
                        List<ChildVesselBean> childVesselBeans = new ArrayList<>();
                        parentVesselBeans.add(new ParentVesselBean(entry.getKey(), null, sampleKitInfo.getReceptacleType(),
                                childVesselBeans));
                        for (SampleKitReceiptResponse.Barcodes barcodes : entry.getValue()) {
                            childVesselBeans.add(new ChildVesselBean(barcodes.getExternalBarcode(),
                                    barcodes.getSampleBarcode(),
                                    receiptResponse.getTubeTypePerSample().get(barcodes.getSampleBarcode()),
                                    mapSampleToInfo.get(barcodes.getSampleBarcode()).getPosition()));
                        }
                    }
                    SampleReceiptBean sampleReceiptBean = new SampleReceiptBean(new Date(), entry.getKey(),
                            parentVesselBeans, bspUser.getUsername());
                    sampleReceiptResource.notifyOfReceipt(sampleReceiptBean);
                }

                addToPicoQueueIfNecessary(sampleIds, messageCollection);
            }
        }

        return receiptResponse;
    }

    /**
     * Adds all DNA samples to pico queue.
     *
     * @param sampleIds             SampleIDs which are being received.
     * @param messageCollection     Messages back to the user.
     */
    private void addToPicoQueueIfNecessary(List<String> sampleIds, MessageCollection messageCollection) {
        List<LabVessel> vesselsForPico = new ArrayList<>();
        List<LabVessel> labVessels = labVesselDao.findBySampleKeyOrLabVesselLabel(sampleIds);
        labVessels.addAll(labVesselDao.findByBarcodes(sampleIds).values());

        for (LabVessel labVessel : labVessels) {
            if (labVessel.isDNA()) {
                vesselsForPico.add(labVessel);
            }
        }
        QueueSpecialization queueSpecialization =
                PicoEnqueueOverride.determinePicoQueueSpecialization(vesselsForPico);
        queueEjb.enqueueLabVessels(vesselsForPico, QueueType.PICO,
                "Received on " + DateUtils.convertDateTimeToString(new Date()), messageCollection,
                QueueOrigin.RECEIVING, queueSpecialization);
    }

    /**
     * For samples that arrive in Non-Broad Tubes, e.g. Blood Spot Cards without SM-IDs but have a Collaborator
     * Sample ID. Technician will link these Collaborator Sample IDs to newly created SM-IDs.
     * @return SampleKitReceivedBean received from BSP.
     */
    public SampleKitReceivedBean receiveNonBroadTubes(Map<String, String> sampleToCollaborator,
                                                      BspUser bspUser, MessageCollection messageCollection) {

        SampleKitReceivedBean sampleKitReceivedBean = null;
        // Validate first, if there are no errors receive first in BSP, then do the mercury receipt work.
        validateForReceipt(sampleToCollaborator.keySet(), messageCollection, bspUser.getUsername());

        if (!messageCollection.hasErrors()) {

            sampleKitReceivedBean = bspRestService.receiveNonBroadSamples(sampleToCollaborator, bspUser.getUsername());

            if (sampleKitReceivedBean.isSuccess()) {

                for (SampleKitReceivedBean.KitInfo kit: sampleKitReceivedBean.getReceivedSamplesPerKit()) {

                    Map<String, String> sampleToType = sampleKitReceivedBean.getTubeTypePerSample()
                            .stream().collect(Collectors.toMap(SampleKitReceivedBean.TubeTypePerSample::getSampleId,
                                    SampleKitReceivedBean.TubeTypePerSample::getTubeType));
                    List<ParentVesselBean> parentVesselBeans = new ArrayList<>();
                    for (SampleKitReceivedBean.SampleBarcodes barcodes : kit.getSampleBarcodes()) {
                        String tubeType = sampleToType.get(barcodes.getSampleBarcode());
                        parentVesselBeans.add(new ParentVesselBean(null, barcodes.getSampleBarcode(), tubeType,null));
                    }

                    addToPicoQueueIfNecessary(kit.getSamples(), messageCollection);

                    SampleReceiptBean sampleReceiptBean = new SampleReceiptBean(new Date(), kit.getKitId(),
                            parentVesselBeans, bspUser.getUsername());
                    sampleReceiptResource.notifyOfReceipt(sampleReceiptBean);
                }
            }
        }

        return sampleKitReceivedBean;
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
