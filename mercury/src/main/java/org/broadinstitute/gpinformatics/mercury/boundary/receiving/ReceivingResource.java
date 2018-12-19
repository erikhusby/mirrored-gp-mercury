package org.broadinstitute.gpinformatics.mercury.boundary.receiving;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.response.SampleKitReceiptResponse;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ReceiveSamplesEjb;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A JAX-RS resource for Receiving Samples
 */
@Stateful
@RequestScoped
@Path("/receiving")
public class ReceivingResource {

    private static final Log log = LogFactory.getLog(ReceivingResource.class);

    @Inject
    private ReceiveSamplesEjb receiveSamplesEjb;

    @Inject
    protected SampleDataFetcher sampleDataFetcher;

    @Inject
    private BSPUserList bspUserList;

    /**
     * This function will receive a mix of SM-IDs and manufacturer tube barcodes.
     * @param tubeIdentifiers - List of SM-IDs or manufacturer tube barcodes.
     * @param username - Username of person receiving the samples.
     * @return OK Response if all samples have been received or already received
     */
    @PUT
    @Path("/receiveBySamplesAndDDP")
    @Consumes(MediaType.APPLICATION_XML)
    public Response receiveBySamplesAndDDP(@QueryParam("q") List<String> tubeIdentifiers,
                                           @QueryParam("username") String username) {
        MessageCollection messageCollection = new MessageCollection();

        BspUser bspUser = bspUserList.getByUsername(username);
        if (bspUser == null) {
            messageCollection.addError("Unknown user " + username);
        }

        Map<Boolean, List<String>> partition = tubeIdentifiers.stream()
                .collect(Collectors.partitioningBy(t -> t.contains("SM-")));

        List<String> sampleIds = partition.get(true);
        List<String> tubeSampleIds = parseTubeSampleIds(partition.get(false), messageCollection);
        sampleIds.addAll(tubeSampleIds);

        List<String> samplesThatCanBeReceived = parseSamplesThatCanBeReceived(sampleIds, messageCollection);

        if (messageCollection.hasErrors()) {
            String errors = StringUtils.join("", messageCollection.getErrors());
            return Response.serverError().entity(errors).build();
        } else if (samplesThatCanBeReceived.size() == 0) {
            String warning = "None of the samples are in state ready to be 'Received'";
            return Response.notModified().entity(warning).build();
        }

        try {
            SampleKitReceiptResponse response = receiveSamplesEjb.receiveSamples(new HashMap<>(), samplesThatCanBeReceived,
                    bspUser, messageCollection);

            if (messageCollection.hasErrors()) {
                String errors = StringUtils.join("", messageCollection.getErrors());
                return Response.serverError().entity(errors).build();
            }
            return Response.ok(response).build();
        } catch (JAXBException e) {
            log.error("Error binding response XML", e);
            return Response.serverError().entity("Internal Server Error binding response XML").build();
        }
    }

    /**
     * Return the sample IDs for each tube barcode and add any unknown barcode to the MessageCollection
     * error list.
     */
    private List<String> parseTubeSampleIds(List<String> tubeBarcodes, MessageCollection messageCollection) {
        if (tubeBarcodes.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, GetSampleDetails.SampleInfo> mapBarcodeToSampleInfo =
                sampleDataFetcher.fetchSampleDetailsByBarcode(tubeBarcodes);

        List<String> tubeSampleIds = new ArrayList<>();
        List<String> unknownTubeBarcodes = new ArrayList<>();
        for (String tubeBarcode: tubeBarcodes) {
            if (mapBarcodeToSampleInfo.get(tubeBarcode) == null) {
                unknownTubeBarcodes.add(tubeBarcode);
            } else {
                tubeSampleIds.add(mapBarcodeToSampleInfo.get(tubeBarcode).getSampleId());
            }
        }

        if (unknownTubeBarcodes.size() > 0) {
            String err = "Unknown tube barcodes: " + String.join(",", unknownTubeBarcodes);
            messageCollection.addError(err);
        }

        return tubeSampleIds;
    }

    /**
     * Returns the sample IDs that are in a state to be received: 'SHIPPED' and adds any unknown sample IDs to the
     * MessageCollection error list.
     */
    private List<String> parseSamplesThatCanBeReceived(List<String> sampleIds, MessageCollection messageCollection) {
        if (sampleIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, SampleData> mapIdToSampleData = sampleDataFetcher.fetchSampleData(sampleIds,
                BSPSampleSearchColumn.SAMPLE_STATUS);

        List<String> unknownSampleIds = sampleIds.stream()
                .filter(sampleId -> !mapIdToSampleData.containsKey(sampleId))
                .collect(Collectors.toList());

        if (unknownSampleIds.size() > 0) {
            String err = "Unknown sample IDs: " + String.join(",", unknownSampleIds);
            messageCollection.addError(err);
        }

        return mapIdToSampleData.values().stream()
                .filter(canReceive())
                .map(SampleData::getSampleId)
                .collect(Collectors.toList());
    }

    private Predicate<SampleData> canReceive() {
        return sampleData -> sampleData.getSampleStatus().equalsIgnoreCase("SHIPPED");
    }

    // Used only for testing
    public void setReceiveSamplesEjb(ReceiveSamplesEjb receiveSamplesEjb) {
        this.receiveSamplesEjb = receiveSamplesEjb;
    }
}
