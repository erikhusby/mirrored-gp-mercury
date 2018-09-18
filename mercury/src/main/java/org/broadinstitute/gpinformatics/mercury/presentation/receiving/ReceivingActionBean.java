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
import java.util.List;
import java.util.Map;
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

    @ValidationMethod(on = FIND_SK_ACTION)
    public void validateFindSkId() {
        sampleKitInfo = bspRestService.getSampleKitDetails(rackBarcode);
        if (sampleKitInfo == null) {
            addValidationError("rackBarcode", "Failed to find SK.");
            return;
        }
        if (sampleKitInfo.getSampleInfos() == null || sampleKitInfo.getSampleInfos().isEmpty()) {
            addValidationError("rackBarcode", "No samples found for SK.");
        }

        // Attempt to grab CO if its a plate
        if (sampleKitInfo.isPlate()) {
            bspRestService.getSampleInfoForContainer(sampleKitInfo.getKitId());
        }

        // Convert to Sample Data list to re-use table from Sample Scan
        sampleRows = new ArrayList<>();
        for (SampleInfo sampleInfo: sampleKitInfo.getSampleInfos()) {
            Map<BSPSampleSearchColumn, String> dataMap = new HashMap<>();
            dataMap.put(BSPSampleSearchColumn.SAMPLE_ID, sampleInfo.getSampleId());
            dataMap.put(BSPSampleSearchColumn.SAMPLE_STATUS, sampleInfo.getStatus());
            dataMap.put(BSPSampleSearchColumn.SAMPLE_KIT, sampleKitInfo.getKitId());
            dataMap.put(BSPSampleSearchColumn.ORIGINAL_MATERIAL_TYPE, sampleInfo.getOriginalMaterialType());
            BspSampleData bspSampleData = new BspSampleData(dataMap);
            sampleRows.add(bspSampleData);
        }

        sampleRows = checkStatusOfSamples(mapIdToSampleData.values());
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
                BSPSampleSearchColumn.ORIGINAL_MATERIAL_TYPE, BSPSampleSearchColumn.SAMPLE_KIT,
                BSPSampleSearchColumn.SAMPLE_STATUS);
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
        receiveSamples(messageCollection);
        addMessages(messageCollection);
        return new ForwardResolution(RECEIVE_BY_SAMPLE_SCAN_PAGE);
    }

    @HandlesEvent(RECEIVE_BY_SK_TO_BSP)
    public Resolution receiveBySkToBspSubmit() throws JAXBException {
        receiveSamples(messageCollection);
        addMessages(messageCollection);
        if (!messageCollection.hasErrors()) {
            validateFindSkId();
            return findSampleKitInfo();
        } else {
            return new ForwardResolution(RECEIVE_BY_SK_PAGE);
        }
    }

    @ValidationMethod(on = FIND_COLLABORATOR_ACTION)
    public void validateFindCollaborator() {
        mapIdToSampleData = bspSampleDataFetcher.fetchSampleData(
                Collections.singletonList(sampleIds),
                BSPSampleSearchColumn.ORIGINAL_MATERIAL_TYPE, BSPSampleSearchColumn.SAMPLE_KIT,
                BSPSampleSearchColumn.SAMPLE_STATUS, BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID);
        if (mapIdToSampleData == null || mapIdToSampleData.isEmpty()) {
            addValidationError("sampleId","Failed to find sample ID");
        } else {
            BspSampleData bspSampleData = mapIdToSampleData.values().iterator().next();
            if (bspSampleData.hasData() && StringUtils.isNotBlank(bspSampleData.getCollaboratorsSampleName())) {
                addValidationError("sampleId", "Sample already linked to " + bspSampleData.getCollaboratorsSampleName());
            }
        }

        if (mapSampleToCollaborator != null) {
            if (mapSampleToCollaborator.containsKey(sampleIds)) {
                addValidationError("sampleId", "Sample already added " + sampleIds);
            }
            if (mapSampleToCollaborator.containsValue(collaboratorSampleId)) {
                addValidationError("collaboratorSampleId", "Collaborator Sample ID already added.");
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

    private boolean receiveSamples(MessageCollection messageCollection) throws JAXBException {
        SampleKitReceiptResponse response = receiveSamplesEjb.receiveSamples(selectedSampleIds,
                getUserBean().getBspUser(), messageCollection);

        for (String error : response.getMessages()) {
            addGlobalValidationError(error);
        }

        addMessages(messageCollection);
        if (!messageCollection.hasErrors() && response.isSuccess()) {
            messageCollection.addInfo("Sucessfully received samples in BSP: " +
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
            messageCollection.addInfo("Sucessfully received samples in BSP: " +
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

        if (rackScanEmpty) {
            messageCollection.addError("No results from rack scan");
        }

        if (messageCollection.hasErrors()) {
            showLayout = false;
            addMessages(messageCollection);
        } else {
            showLayout = true;
        }
        return new ForwardResolution(RECEIVING_PAGE)
                .addParameter(BY_KIT_SCAN_ACTION, "");
    }

    @HandlesEvent(RECEIVE_KIT_TO_BSP)
    public Resolution receiveToBspByKitScan() throws JAXBException {
        if (sampleInfos == null) {
            messageCollection.addError("Error occured when posting rack scan data");
            return null;
        }

        List<String> sampleIds = sampleInfos.stream().map(GetSampleDetails.SampleInfo::getSampleId).collect(Collectors.toList());
        SampleKitReceiptResponse response = receiveSamplesEjb.receiveSamples(sampleIds,
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
