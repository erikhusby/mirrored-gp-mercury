package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.MultiPart;
import org.apache.poi.ss.usermodel.Workbook;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.SpreadsheetCreator;
import org.broadinstitute.gpinformatics.mercury.BSPRestClient;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToVesselTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Creates a new root sample in BSP, e.g. for Blood Biopsy plasma and buffy coat samples, so they can have
 * different collaborator sample ID suffixes.
 */
public class BspNewRootHandler extends AbstractEventHandler {

    private static final String BSP_KIT_REST_URL = "kit";

    @Inject
    private BSPRestClient bspRestClient;

    @Inject
    private BSPSampleDataFetcher bspSampleDataFetcher;

    /** Mirrors definition in BSP KitResource. */
    @XmlRootElement
    public static class KitSample {
        private String bspSampleId;
        private String collaboratorSampleId;

        public String getBspSampleId() {
            return bspSampleId;
        }

        // Used by JAXB
        @SuppressWarnings("unused")
        public void setBspSampleId(String bspSampleId) {
            this.bspSampleId = bspSampleId;
        }

        public String getCollaboratorSampleId() {
            return collaboratorSampleId;
        }

        public void setCollaboratorSampleId(String collaboratorSampleId) {
            this.collaboratorSampleId = collaboratorSampleId;
        }
    }

    /** Mirrors definition in BSP KitResource. */
    @XmlRootElement
    public static class CreateKitReturn {
        private List<KitSample> samples = new ArrayList<>();

        public List<KitSample> getSamples() {
            return samples;
        }

        public void setSamples(List<KitSample> samples) {
            this.samples = samples;
        }
    }

    private void createBspKit(List<LabVessel> labVessels, String receptacleType, String materialType,
            String collabSampleSuffix, String tumorNormal) {

        // Get data from BSP
        List<String> sampleNames = new ArrayList<>();
        for (LabVessel labVessel : labVessels) {
            sampleNames.add(labVessel.getSampleInstancesV2().iterator().next().getRootOrEarliestMercurySample().
                    getSampleKey());
        }
        Map<String, BspSampleData> mapIdToSampleData = bspSampleDataFetcher.fetchSampleData(sampleNames,
                BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID,
                BSPSampleSearchColumn.ORIGINAL_MATERIAL_TYPE,  BSPSampleSearchColumn.GENDER,
                BSPSampleSearchColumn.COLLECTION);

        // Prepare data to send to web service
        Object[][] rows = new Object[labVessels.size() + 1][];
        rows[0] = new Object[] {"Collaborator Sample ID", "Collaborator Patient ID", "Submitted Material Type",
                "Original Material Type", "Sample Type", "Patient Gender",
                "External ID", "Original Root"};
        String collection = null;
        for (int i = 0; i < labVessels.size(); i++) {
            LabVessel labVessel = labVessels.get(i);
            StringBuilder originalRoots = new StringBuilder();
            for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                if (originalRoots.length() > 0) {
                    originalRoots.append("||");
                }
                originalRoots.append(sampleInstanceV2.getRootOrEarliestMercurySampleName());
            }
            BspSampleData bspSampleData = mapIdToSampleData.get(sampleNames.get(i));
            collection = bspSampleData.getCollection();
            // "Collection" search result is actually Group / Collection, we want just Collection
            collection = collection.substring(collection.lastIndexOf('/') + 2);
            rows[i + 1] = new Object[] {
                    bspSampleData.getCollaboratorsSampleName() + collabSampleSuffix,
                    bspSampleData.getCollaboratorParticipantId(),
                    materialType,
                    bspSampleData.getOriginalMaterialType(),
                    tumorNormal,
                    bspSampleData.getGender(),
                    labVessel.getLabel(),
                    originalRoots.toString()};
        }

        // Call BSP KitResource web service
        String sheetName = "Sample Submission Form";
        Workbook workbook = SpreadsheetCreator.createSpreadsheet(sheetName, rows);
        String urlString = bspRestClient.getUrl(BSP_KIT_REST_URL);
        WebResource webResource = bspRestClient.getWebResource(urlString);
        try (FormDataMultiPart formDataMultiPart = new FormDataMultiPart()) {
            formDataMultiPart.field("collection", collection);
            formDataMultiPart.field("materialType", materialType);
            formDataMultiPart.field("receptacleType", receptacleType);
            formDataMultiPart.field("datasetName", "NewRoots");
            formDataMultiPart.field("domain", "VIRAL");
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            workbook.write(byteArrayOutputStream);
            MultiPart multiPart = formDataMultiPart.bodyPart(
                    new FormDataBodyPart("spreadsheet", new ByteArrayInputStream(byteArrayOutputStream.toByteArray()),
                            MediaType.APPLICATION_OCTET_STREAM_TYPE));
            CreateKitReturn createKitReturn = webResource.type(MediaType.MULTIPART_FORM_DATA_TYPE).post(
                    CreateKitReturn.class, multiPart);

            // Set new sampleIds on vessels
            List<KitSample> samples = createKitReturn.getSamples();
            for (int i = 0; i < samples.size(); i++) {
                KitSample kitSample = samples.get(i);
                // Indicate new root on MercurySample
                labVessels.get(i).getMercurySamples().add(new MercurySample(kitSample.getBspSampleId(),
                        MercurySample.MetadataSource.BSP, true));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleEvent(LabEvent targetEvent, StationEventType stationEvent) {
        Set<LabVessel> labVessels = new LinkedHashSet<>();
        String receptacleType = null;

        for (VesselToVesselTransfer vesselToVesselTransfer : targetEvent.getVesselToVesselTransfers()) {
            BarcodedTube barcodedTube = OrmUtil.proxySafeCast(vesselToVesselTransfer.getTargetVessel(), BarcodedTube.class);
            receptacleType = barcodedTube.getTubeType().getDisplayName();
            labVessels.add(barcodedTube);
        }

        for (CherryPickTransfer cherryPickTransfer : targetEvent.getCherryPickTransfers()) {
            LabVessel labVessel = cherryPickTransfer.getTargetVesselContainer().getVesselAtPosition(
                    cherryPickTransfer.getTargetPosition());
            BarcodedTube barcodedTube = OrmUtil.proxySafeCast(labVessel, BarcodedTube.class);
            receptacleType = barcodedTube.getTubeType().getDisplayName();
            labVessels.add(barcodedTube);
        }

        LabEventType labEventType = targetEvent.getLabEventType();
        createBspKit(new ArrayList<>(labVessels), receptacleType,
                labEventType.getResultingMaterialType().getDisplayName(), labEventType.getCollabSampleSuffix(),
                labEventType.getMetadataValue());
    }

}
