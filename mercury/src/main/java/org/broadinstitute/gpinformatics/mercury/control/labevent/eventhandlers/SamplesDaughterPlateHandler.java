/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.MultiPart;
import org.apache.poi.ss.usermodel.Workbook;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.SpreadsheetCreator;
import org.broadinstitute.gpinformatics.mercury.BSPRestClient;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles bettalims messages that need to be passed to a BSP REST service for processing.
 */
public class SamplesDaughterPlateHandler {
    public static final String BSP_TRANSFER_REST_URL = "plate/transfer";
    private static final String BSP_KIT_REST_URL = "kit";

    @Inject
    private BSPRestClient bspRestClient;

    public void postToBsp(BettaLIMSMessage message, String bspRestUrl) {

        // Posts message to BSP using the specified REST url.
        String urlString = bspRestClient.getUrl(bspRestUrl);
        WebResource webResource = bspRestClient.getWebResource(urlString);
        ClientResponse response = webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, message);

        // This is called in context of bettalims message handling which handles errors via RuntimeException.
        if (response.getClientResponseStatus().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new RuntimeException("POST to " + urlString + " returned: " + response.getEntity(String.class));
        }

    }

    @XmlRootElement
    public static class KitSample {
        private String bspSampleId;
        private String collaboratorSampleId;

        public KitSample(String bspSampleId, String collaboratorSampleId) {
            this.bspSampleId = bspSampleId;
            this.collaboratorSampleId = collaboratorSampleId;
        }

        /** Used by JAXB. */
        @SuppressWarnings("unused")
        public KitSample() {
        }

        public String getBspSampleId() {
            return bspSampleId;
        }

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

    @XmlRootElement
    public static class CreateKitReturn {
        private List<KitSample> samples = new ArrayList<>();

        public CreateKitReturn() {
        }

        private void addSample(KitSample sample) {
            samples.add(sample);
        }

        public List<KitSample> getSamples() {
            return samples;
        }

        public void setSamples(List<KitSample> samples) {
            this.samples = samples;
        }
    }

    public void x() {
        String sheetName = "Sample Submission Form";
        Object[][] rows = {
                {"Collaborator Sample ID", "Collaborator Patient ID", "Submitted Material Type", "Original Material Type", "Sample Type", "Tumor Type", "Patient Gender", "Patient Diagnosis or Disease"},
                {"JT3-BUFFY", "JT3-PT", "Whole Blood:Buffy Coat", "Whole Blood:Whole Blood", "Tumor", "Primary", "Male", "Test"}
        };
        Workbook workbook = SpreadsheetCreator.createSpreadsheet(sheetName, rows);
        String urlString = bspRestClient.getUrl(BSP_KIT_REST_URL);
        WebResource webResource = bspRestClient.getWebResource(urlString);
        try (FormDataMultiPart formDataMultiPart = new FormDataMultiPart()) {
            formDataMultiPart.field("collection", "Jon's Collection");
            formDataMultiPart.field("materialType", "Whole Blood:Buffy Coat");
            formDataMultiPart.field("receptacleType", "Matrix Tube Screw cap [0.5mL]");
            formDataMultiPart.field("datasetName", "NewRoots");
            formDataMultiPart.field("domain", "VIRAL");
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            workbook.write(byteArrayOutputStream);
            MultiPart multiPart = formDataMultiPart.bodyPart(
                     new FormDataBodyPart("spreadsheet", new ByteArrayInputStream(byteArrayOutputStream.toByteArray()),
                             MediaType.APPLICATION_OCTET_STREAM_TYPE));
            CreateKitReturn createKitReturn = webResource.type(MediaType.MULTIPART_FORM_DATA_TYPE).post(
                    CreateKitReturn.class, multiPart);
            createKitReturn.getSamples();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
