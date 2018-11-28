package org.broadinstitute.gpinformatics.mercury.presentation.receiving;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.rackscan.ScannerException;
import org.broadinstitute.bsp.client.response.SampleKitReceiptResponse;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ReceiveSamplesEjb;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.SampleInfo;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.SampleKitInfo;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.SampleKitReceivedBean;
import org.broadinstitute.gpinformatics.mercury.control.vessel.BSPRestService;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean;

import javax.inject.Inject;
import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@UrlBinding(ReceivingActionBean.ACTION_BEAN_URL)
public class ReceivingActionBean extends RackScanActionBean {
    private static final Log logger = LogFactory.getLog(ReceivingActionBean.class);

    public static final String ACTION_BEAN_URL = "/receiving/receiving.action";
    public static final String BY_KIT_SCAN_ACTION = "byKitScan";
    public static final String BY_SK_ACTION = "bySkId";
    public static final String BY_SAMPLE_SCAN_ACTION = "bySampleScan";
    public static final String BY_SCAN_AND_LINK_ACTION = "bySampleScanAndLink";
    public static final String SEARCH_BY_SAMPLE_ACTION = "findBySampleScan";
    public static final String RECEIVING_PAGE = "/receiving/receiving.jsp";
    public static final String RECEIVE_BY_SAMPLE_SCAN_PAGE = "/receiving/receive_by_sample.jsp";
    public static final String RECEIVE_BY_SK_PAGE = "/receiving/receive_by_sk.jsp";
    public static final String RECEIVE_BY_SCAN_AND_LINK_PAGE = "/receiving/receive_by_scan_and_link.jsp";
    public static final String FIRE_RACK_SCAN = "rackScan";
    public static final String RECEIVE_KIT_TO_BSP = "receiveKitToBsp";
    public static final String RECEIVE_BY_SAMPLE_TO_BSP = "receiveBySampleToBsp";
    public static final String RECEIVE_BY_SK_TO_BSP = "receiveBySkToBsp";
    public static final String RECEIVE_BY_SCAN_AND_LINK = "receiveByScanAndLink";
    public static final String FIND_SK_ACTION = "findSkId";
    public static final String FIND_COLLABORATOR_ACTION = "findCollaborator";

    @Inject
    protected BSPSampleDataFetcher bspSampleDataFetcher;

    @Inject
    protected BSPRestService bspRestService;

    @Inject
    protected ReceiveSamplesEjb receiveSamplesEjb;

    @Validate(required = true, on = {FIRE_RACK_SCAN, FIND_SK_ACTION})
    private String rackBarcode;

    @Validate(required = true, on = {FIND_COLLABORATOR_ACTION})
    private String collaboratorSampleId;

    private Map<VesselPosition, GetSampleDetails.SampleInfo> scanPositionToSampleInfo;

    private VesselGeometry vesselGeometry = RackOfTubes.RackType.Matrix96.getVesselGeometry();

    private List<GetSampleDetails.SampleInfo> sampleInfos;

    private MessageCollection messageCollection = new MessageCollection();

    protected boolean showLayout;

    private boolean showRackScan;

    private List<SampleData> sampleRows;

    private List<Map<String, String>> sampleCollaboratorRows;

    private List<String> selectedSampleIds = new ArrayList<>();

    private List<String> allSampleIds = new ArrayList<>();

    private String sampleIds;
    private SampleKitInfo sampleKitInfo;

    private Map<String, String> mapSampleToCollaborator;
    private Map<String, BspSampleData> mapIdToSampleData;

    @DefaultHandler
    @HandlesEvent(BY_KIT_SCAN_ACTION)
    public Resolution byKitScan() {
        showRackScan = true;
        return new ForwardResolution(RECEIVING_PAGE);
    }

    @HandlesEvent(BY_SK_ACTION)
    public Resolution bySkId() {
        return new ForwardResolution(RECEIVE_BY_SK_PAGE);
    }

    @HandlesEvent(BY_SCAN_AND_LINK_ACTION)
    public Resolution byScanAndLink() {
        return new ForwardResolution(RECEIVE_BY_SCAN_AND_LINK_PAGE);
    }

    @ValidationMethod(on = {FIND_SK_ACTION, FIRE_RACK_SCAN}, priority = 0)
    public void validateSampleKitInformation() {
        sampleKitInfo = fetchSampleKitDetails(rackBarcode);
        if (sampleKitInfo == null) {
            addValidationError("rackBarcode", "Failed to find SK.");
            return;
        }
        if (sampleKitInfo.getSampleInfos() == null || sampleKitInfo.getSampleInfos().isEmpty()) {
            addValidationError("rackBarcode", "No samples found for SK.");
        }

        if (!sampleKitInfo.getPlate()) {
            Set<String> cantBeRackScanned = sampleKitInfo.getSampleInfos().stream()
                    .filter(sampleInfo -> !sampleInfo.isCanBeRackScanned())
                    .map(SampleInfo::getSampleId)
                    .collect(Collectors.toSet());
            if (cantBeRackScanned.size() > 0) {
                String errMsg = "Sample Kit contains vessels that shouldn't be scanned: " + StringUtils.join(cantBeRackScanned, ",");
                addValidationError("rackBarcode",  errMsg);
            }
        }

        if (!sampleKitInfo.getStatus().equalsIgnoreCase("SHIPPED") &&
            !sampleKitInfo.getStatus().equalsIgnoreCase("PARTIALLYRECEIVED")) {
            addValidationError("rackBarcode", "Unexpected status found for SK found " + sampleKitInfo.getStatus());
        }
    }

    public SampleKitInfo fetchSampleKitDetails(String rackBarcode) {
        try {
            return bspRestService.getSampleKitDetails(rackBarcode);
        } catch (Exception e) {
            logger.error("Failed to find sample kit details for " + rackBarcode);
        }
        return null;
    }

    @ValidationMethod(on = {FIND_SK_ACTION}, priority = 1)
    public void validateFindSkId() {
        if (!sampleKitInfo.getPlate()) {
            addValidationError("rackBarcode", "This page is only for receiving plates by SK-ID.");
            return;
        }

        // Convert to Sample Data list to re-use table from Sample Scan
        List<BspSampleData> bspSampleData = new ArrayList<>();
        for (SampleInfo sampleInfo: sampleKitInfo.getSampleInfos()) {
            Map<BSPSampleSearchColumn, String> dataMap = new HashMap<>();
            dataMap.put(BSPSampleSearchColumn.SAMPLE_ID, sampleInfo.getSampleId());
            dataMap.put(BSPSampleSearchColumn.SAMPLE_STATUS, sampleInfo.getStatus());
            dataMap.put(BSPSampleSearchColumn.SAMPLE_KIT, sampleKitInfo.getKitId());
            dataMap.put(BSPSampleSearchColumn.ORIGINAL_MATERIAL_TYPE, sampleInfo.getOriginalMaterialType());
            BspSampleData sampleData = new BspSampleData(dataMap);
            bspSampleData.add(sampleData);
        }

        sampleRows = checkStatusOfSamples(bspSampleData);
    }

    @HandlesEvent(FIND_SK_ACTION)
    public Resolution findSampleKitInfo() {
        showLayout = true;
        return new ForwardResolution(RECEIVE_BY_SK_PAGE);
    }

    @HandlesEvent(BY_SAMPLE_SCAN_ACTION)
    public Resolution bySampleScan() {
        return new ForwardResolution(RECEIVE_BY_SAMPLE_SCAN_PAGE);
    }

    @ValidationMethod(on = SEARCH_BY_SAMPLE_ACTION)
    public void validateSearchBySample() {
        if (sampleIds == null) {
            addValidationError("sampleIds", "Enter at least one sample ID.");
        }
    }

    @HandlesEvent(SEARCH_BY_SAMPLE_ACTION)
    public Resolution searchBySample() {
        showLayout = true;
        String[] splitSampleIds = sampleIds.trim().split("\\s+");
        Map<String, BspSampleData> mapIdToSampleData = bspSampleDataFetcher.fetchSampleData(Arrays.asList(splitSampleIds),
                BSPSampleSearchColumn.ORIGINAL_MATERIAL_TYPE, BSPSampleSearchColumn.MATERIAL_TYPE,
                BSPSampleSearchColumn.SAMPLE_KIT, BSPSampleSearchColumn.SAMPLE_STATUS);
        for (String sampleKey: splitSampleIds) {
            if (!mapIdToSampleData.containsKey(sampleKey)) {
                messageCollection.addError("Failed to find sample: " + sampleKey);
            } else if (!mapIdToSampleData.get(sampleKey).hasData()) {
                messageCollection.addError("No sample data found for: " + sampleKey);
            }
        }
        sampleRows = checkStatusOfSamples(mapIdToSampleData.values());

        if (messageCollection.hasErrors()) {
            showLayout = false;
        } else if (sampleRows.size() == 0) {
            showLayout = false;
            messageCollection.addInfo("All samples are already received.");
        }
        addMessages(messageCollection);
        return new ForwardResolution(RECEIVE_BY_SAMPLE_SCAN_PAGE);
    }

    @ValidationMethod(on = {RECEIVE_BY_SAMPLE_TO_BSP, RECEIVE_BY_SK_TO_BSP, RECEIVE_BY_SCAN_AND_LINK})
    public void validateReceiveBySample() {
        if (selectedSampleIds == null || selectedSampleIds.size() == 0) {
            addGlobalValidationError("Must check at least one sample.");
        }
    }

    @HandlesEvent(RECEIVE_BY_SAMPLE_TO_BSP)
    public Resolution receiveBySampleToBspSubmit() throws JAXBException {
        receiveSamples(messageCollection, new HashMap<>());
        addMessages(messageCollection);
        return new ForwardResolution(RECEIVE_BY_SAMPLE_SCAN_PAGE);
    }

    @HandlesEvent(RECEIVE_BY_SK_TO_BSP)
    public Resolution receiveBySkToBspSubmit() throws JAXBException {
        SampleKitInfo sampleKitDetails = bspRestService.getSampleKitDetails(rackBarcode);
        Map<String, SampleKitInfo> sampleKitInfoMap = new HashMap<>();
        sampleKitInfoMap.put(rackBarcode, sampleKitDetails);
        receiveSamples(messageCollection, sampleKitInfoMap);
        addMessages(messageCollection);
        return new ForwardResolution(RECEIVE_BY_SK_PAGE);
    }

    @ValidationMethod(on = FIND_COLLABORATOR_ACTION)
    public void validateFindCollaborator() {
        mapIdToSampleData = bspSampleDataFetcher.fetchSampleData(
                Collections.singletonList(sampleIds),
                BSPSampleSearchColumn.ORIGINAL_MATERIAL_TYPE, BSPSampleSearchColumn.MATERIAL_TYPE,
                BSPSampleSearchColumn.SAMPLE_KIT, BSPSampleSearchColumn.SAMPLE_STATUS,
                BSPSampleSearchColumn.RECEPTACLE_TYPE, BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID);
        if (mapIdToSampleData == null || mapIdToSampleData.isEmpty()) {
            addValidationError("sampleId","Failed to find sample ID");
        } else {
            BspSampleData bspSampleData = mapIdToSampleData.values().iterator().next();
            if (bspSampleData.hasData() && StringUtils.isNotBlank(bspSampleData.getCollaboratorsSampleName())) {
                addValidationError("sampleId", "Sample already linked to " + bspSampleData.getCollaboratorsSampleName());
            }
        }

        String receptacleType = mapIdToSampleData.get(sampleIds).getReceptacleType();
        if (receptacleType == null) {
            addValidationError("sampleId", "Unknown Receptacle Type");
        } else if (receptacleType.startsWith("Matrix") || receptacleType.startsWith("Abgene")) {
            addValidationError("sampleId", "Barcoded tubes should be received by rack scan: " + receptacleType);
        }

        if (mapSampleToCollaborator != null) {
            if (mapSampleToCollaborator.containsKey(sampleIds)) {
                addValidationError("sampleId", "Sample already added " + sampleIds);
            }
        }
    }

    @HandlesEvent(FIND_COLLABORATOR_ACTION)
    public Resolution addSampleAndCollaborator() {
        if (mapSampleToCollaborator == null) {
            mapSampleToCollaborator = new HashMap<>();
        }
        if (sampleCollaboratorRows == null) {
            sampleCollaboratorRows = new ArrayList<>();
        }
        mapSampleToCollaborator.put(sampleIds, collaboratorSampleId);
        BspSampleData sampleData = mapIdToSampleData.get(sampleIds);
        if (checkStatusOfSampleIsInShippedState(sampleData)) {
            Map<String, String> dataMap = new HashMap<>();
            dataMap.put(BSPSampleSearchColumn.SAMPLE_ID.name(), sampleData.getSampleId());
            dataMap.put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID.name(), collaboratorSampleId);
            dataMap.put(BSPSampleSearchColumn.SAMPLE_STATUS.name(), sampleData.getSampleStatus());
            dataMap.put(BSPSampleSearchColumn.SAMPLE_KIT.name(), sampleData.getSampleKitId());
            dataMap.put(BSPSampleSearchColumn.ORIGINAL_MATERIAL_TYPE.name(), sampleData.getOriginalMaterialType());
            dataMap.put(BSPSampleSearchColumn.MATERIAL_TYPE.name(), sampleData.getMaterialType());
            sampleCollaboratorRows.add(dataMap);
        }
        addMessages(messageCollection);

        return new ForwardResolution(RECEIVE_BY_SCAN_AND_LINK_PAGE);
    }

    @HandlesEvent(RECEIVE_BY_SCAN_AND_LINK)
    public Resolution receiveByScanAndLinkSubmit() throws JAXBException {
        Map<String, String> selectedSampleToCollaborator = new HashMap<>();
        for (String sampleId: selectedSampleIds) {
            String collaborator = mapSampleToCollaborator.get(sampleId);
            selectedSampleToCollaborator.put(sampleId, collaborator);
        }
        receiveSamplesAndLink(selectedSampleToCollaborator, messageCollection);
        addMessages(messageCollection);

        // Filter Selected Samples that have been processed.
        sampleCollaboratorRows = sampleCollaboratorRows.stream()
                .filter(dataMap -> !selectedSampleIds.contains(dataMap.get(BSPSampleSearchColumn.SAMPLE_ID.name())))
                .collect(Collectors.toList());

        return new ForwardResolution(RECEIVE_BY_SCAN_AND_LINK_PAGE);
    }

    private List<SampleData> checkStatusOfSamples(Collection<BspSampleData> sampleData) {
        List<SampleData> retList = new ArrayList<>();
        for (BspSampleData sample: sampleData) {
            if (checkStatusOfSampleIsInShippedState(sample)) {
                retList.add(sample);
            }
        }
        return retList;
    }

    private boolean checkStatusOfSampleIsInShippedState(BspSampleData sample) {
        if (sample.getSampleStatus().equalsIgnoreCase("shipped")) {
            return true;
        } else if (sample.getSampleStatus().equalsIgnoreCase("received")) {
            messageCollection.addInfo(sample.getSampleId() + " already received.");
        } else {
            messageCollection.addError("Sample is not in a status that is ready to be received: "
                                       + sample.getSampleId() + " (" + sample.getSampleStatus() + ")");
        }
        return false;
    }

    private boolean receiveSamples(MessageCollection messageCollection, Map<String, SampleKitInfo> sampleKitInfoMap)
            throws JAXBException {
        SampleKitReceiptResponse response = receiveSamplesEjb.receiveSamples(sampleKitInfoMap, selectedSampleIds,
                getUserBean().getBspUser(), messageCollection);

        for (String error : response.getMessages()) {
            addGlobalValidationError(error);
        }

        addMessages(messageCollection);
        if (!messageCollection.hasErrors() && response.isSuccess()) {
            messageCollection.addInfo("Successfully received samples in BSP: " +
                                      StringUtils.join(selectedSampleIds, ","));
            return true;
        } else {
            sampleIds = StringUtils.join(selectedSampleIds, " ");
            return false;
        }
    }

    private boolean receiveSamplesAndLink(
            Map<String, String> selectedSampleToCollaborator,
            MessageCollection messageCollection) throws JAXBException {
        SampleKitReceivedBean response = receiveSamplesEjb.receiveNonBroadTubes(selectedSampleToCollaborator,
                getUserBean().getBspUser(), messageCollection);

        for (String error : response.getMessages()) {
            addGlobalValidationError(error);
        }

        addMessages(messageCollection);
        if (!messageCollection.hasErrors() && response.isSuccess()) {
            messageCollection.addInfo("Successfully received samples in BSP: " +
                                      StringUtils.join(selectedSampleIds, ","));
            return true;
        } else {
            sampleIds = StringUtils.join(selectedSampleIds, " ");
            return false;
        }
    }

    @HandlesEvent(FIRE_RACK_SCAN)
    public Resolution fireRackScan() throws ScannerException {
        scan();
        boolean rackScanEmpty = true;
        scanPositionToSampleInfo = new HashMap<>();
        if(getRackScan() != null) {
            if (rackScan == null || rackScan.isEmpty()) {
                messageCollection.addError("No results from rack scan");
            } else {
                List<String> barcodes = new ArrayList<>(rackScan.values());
                Map<String, GetSampleDetails.SampleInfo> mapBarcodeToSampleInfo =
                        bspSampleDataFetcher.fetchSampleDetailsByBarcode(barcodes);
                for (Map.Entry<String, String> entry : rackScan.entrySet()) {
                    if (StringUtils.isNotEmpty(entry.getValue())) {
                        rackScanEmpty = false;
                        String position = entry.getKey();
                        String barcode = entry.getValue();
                        GetSampleDetails.SampleInfo sampleInfo = mapBarcodeToSampleInfo.get(barcode);
                        if (sampleInfo == null) {
                            messageCollection.addError("Unrecognized tube barcode: " + barcode);
                        } else {
                            VesselPosition vesselPosition = VesselPosition.getByName(position);
                            if (vesselPosition == null) {
                                messageCollection.addError("Unrecognized position: " + position);
                            } else {
                                scanPositionToSampleInfo.put(vesselPosition, sampleInfo);
                            }
                        }
                    }
                }
            }
        }

        Set<String> setOfSamplesInRackScan = scanPositionToSampleInfo.values().stream()
                .map(GetSampleDetails.SampleInfo::getSampleId)
                .collect(Collectors.toSet());

        if (sampleKitInfo != null) {
            Set<String> setOfSamplesInSK = sampleKitInfo.getSampleInfos().stream()
                    .map(SampleInfo::getSampleId)
                    .collect(Collectors.toSet());

            // Filter samples expected in SK
            Set<String> samplesMissingInSK = setOfSamplesInSK.stream()
                    .filter(sm -> !setOfSamplesInRackScan.contains(sm))
                    .collect(Collectors.toSet());

            // Filter samples missing from SK found in rack scan
            Set<String> samplesAddedInRackScan = setOfSamplesInRackScan.stream()
                    .filter(sm -> !setOfSamplesInSK.contains(sm))
                    .collect(Collectors.toSet());

            for (String sample: samplesMissingInSK) {
                messageCollection.addWarning("Expected to find " + sample + " in Rack Scan.");
            }

            for (String sample: samplesAddedInRackScan) {
                messageCollection.addError(sample + " not expected in " + rackBarcode);
            }

            Set<String> intersection = new HashSet<>(setOfSamplesInSK);
            intersection.retainAll(setOfSamplesInRackScan);
            Map<String, BspSampleData> mapIdToSampleData = bspSampleDataFetcher.fetchSampleData(intersection,
                    BSPSampleSearchColumn.MATERIAL_TYPE, BSPSampleSearchColumn.ORIGINAL_MATERIAL_TYPE,
                    BSPSampleSearchColumn.SAMPLE_KIT, BSPSampleSearchColumn.SAMPLE_STATUS);

            sampleRows = checkStatusOfSamples(mapIdToSampleData.values());
        }


        if (rackScanEmpty) {
            messageCollection.addError("No results from rack scan");
        }

        if (messageCollection.hasErrors()) {
            showLayout = false;
            addMessages(messageCollection);
        } else if (messageCollection.hasWarnings()) {
            showLayout = true;
            addMessages(messageCollection);
        } else {
            showLayout = true;
        }
        showRackScan = true;
        return new ForwardResolution(RECEIVING_PAGE)
                .addParameter(BY_KIT_SCAN_ACTION, "");
    }

    @HandlesEvent(RECEIVE_KIT_TO_BSP)
    public Resolution receiveToBspByKitScan() throws JAXBException {
        if (sampleInfos == null) {
            messageCollection.addError("Error occurred when posting rack scan data");
            return null;
        }

        List<String> sampleIds = sampleInfos.stream()
                .map(GetSampleDetails.SampleInfo::getSampleId)
                .filter(sm -> selectedSampleIds.contains(sm))
                .collect(Collectors.toList());

        SampleKitInfo sampleKitDetails = bspRestService.getSampleKitDetails(rackBarcode);
        Map<String, SampleKitInfo> sampleKitInfoMap = new HashMap<>();
        sampleKitInfoMap.put(rackBarcode, sampleKitDetails);

        SampleKitReceiptResponse response = receiveSamplesEjb.receiveSamples(sampleKitInfoMap, sampleIds,
                getUserBean().getBspUser(), messageCollection);

        for (String error : response.getMessages()) {
            addGlobalValidationError(error);
        }

        addMessages(messageCollection);
        if (!messageCollection.hasErrors()) {
            addMessage("Sucessfully received samples in BSP");
        }

        return new ForwardResolution(RECEIVING_PAGE);
    }

    @Override
    public String getRackScanPageUrl() {
        return ACTION_BEAN_URL;
    }

    @Override
    public String getPageTitle() {
        return "Receiving";
    }


    public boolean isShowLayout() {
        return showLayout;
    }

    public void setShowLayout(boolean showLayout) {
        this.showLayout = showLayout;
    }

    public String getRackBarcode() {
        return rackBarcode;
    }

    public void setRackBarcode(String rackBarcode) {
        this.rackBarcode = rackBarcode;
    }

    public String getCollaboratorSampleId() {
        return collaboratorSampleId;
    }

    public void setCollaboratorSampleId(String collaboratorSampleId) {
        this.collaboratorSampleId = collaboratorSampleId;
    }

    public Map<VesselPosition, GetSampleDetails.SampleInfo> getScanPositionToSampleInfo() {
        return scanPositionToSampleInfo;
    }

    public void setScanPositionToSampleInfo(
            Map<VesselPosition, GetSampleDetails.SampleInfo> scanPositionToSampleInfo) {
        this.scanPositionToSampleInfo = scanPositionToSampleInfo;
    }

    public VesselGeometry getVesselGeometry() {
        return vesselGeometry;
    }

    public List<GetSampleDetails.SampleInfo> getSampleInfos() {
        return sampleInfos;
    }

    public void setSampleInfos(List<GetSampleDetails.SampleInfo> sampleInfos) {
        this.sampleInfos = sampleInfos;
    }

    public boolean isShowRackScan() {
        return showRackScan;
    }

    public void setShowRackScan(boolean showRackScan) {
        this.showRackScan = showRackScan;
    }

    public List<SampleData> getSampleRows() {
        return sampleRows;
    }

    public List<String> getSelectedSampleIds() {
        return selectedSampleIds;
    }

    public void setSelectedSampleIds(List<String> selectedSampleIds) {
        this.selectedSampleIds = selectedSampleIds;
    }

    public String getSampleIds() {
        return sampleIds;
    }

    public void setSampleIds(String sampleIds) {
        this.sampleIds = sampleIds;
    }

    public SampleKitInfo getSampleKitInfo() {
        return sampleKitInfo;
    }

    public Map<String, String> getMapSampleToCollaborator() {
        return mapSampleToCollaborator;
    }

    public void setMapSampleToCollaborator(Map<String, String> mapSampleToCollaborator) {
        this.mapSampleToCollaborator = mapSampleToCollaborator;
    }

    public List<Map<String, String>> getSampleCollaboratorRows() {
        return sampleCollaboratorRows;
    }

    public void setSampleCollaboratorRows(
            List<Map<String, String>> sampleCollaboratorRows) {
        this.sampleCollaboratorRows = sampleCollaboratorRows;
    }

    public List<String> getAllSampleIds() {
        return allSampleIds;
    }

    public void setAllSampleIds(List<String> allSampleIds) {
        this.allSampleIds = allSampleIds;
    }
}
