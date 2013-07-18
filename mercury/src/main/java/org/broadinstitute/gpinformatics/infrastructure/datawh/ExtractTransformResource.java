package org.broadinstitute.gpinformatics.infrastructure.datawh;

import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.datawh.LabEventEtl.EventFactDto;
import org.broadinstitute.gpinformatics.infrastructure.datawh.SequencingSampleFactEtl.SequencingRunDto;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * Web service resource used to invoke ETL methods.
 */

@Path("etl")
public class ExtractTransformResource {
    private ExtractTransform extractTransform;

    public ExtractTransformResource() {
    }

    @Inject
    public ExtractTransformResource(ExtractTransform extractTransform) {
        this.extractTransform = extractTransform;
    }

    /**
     * Runs ETL on one entity class for the given range of entity ids (possibly all).
     *
     * @param entityClassname The fully qualified classname of the Mercury class to ETL.
     * @param startId         First entity id of a range of ids to backfill.  Set to 0 for minimum.
     * @param endId           Last entity id of a range of ids to backfill.  Set to -1 for maximum.
     */
    @Path("backfill/{entityClassname}/{startId}/{endId}")
    @PUT
    public Response entityIdRangeEtl(@PathParam("entityClassname") String entityClassname,
                                     @PathParam("startId") long startId,
                                     @PathParam("endId") long endId) {

        extractTransform.initConfig();
        Response.Status status = extractTransform.backfillEtl(entityClassname, startId, endId);
        return Response.status(status).build();
    }

    /**
     * Runs ETL for audit changes that occurred in the specified time interval.
     * Normally the range is from previous etl end time to the current second.
     *
     * @param startDateTime start of interval of audited changes, in yyyyMMddHHmmss format,
     *                      or "0" to use previous end time.
     * @param endDateTime   end of interval of audited changes, in yyyyMMddHHmmss format, or "0" for now.
     *                      Excludes endpoint.  "0" will withhold updating the lastEtlRun file.
     */
    @Path("incremental/{startDateTime}/{endDateTime}")
    @PUT
    public Response auditDateRangeEtl(@PathParam("startDateTime") String startDateTime,
                                      @PathParam("endDateTime") String endDateTime) {

        extractTransform.initConfig();
        extractTransform.incrementalEtl(startDateTime, endDateTime);
        return Response.status(ClientResponse.Status.ACCEPTED).build();
    }

    /**
     * Returns an etl-type breakdown of a sequencing run.
     *
     * @param sequencingRunId the entity id of the sequencing run.
     *
     * @return htm table of what etl would put in the sequencing sample fact table.
     */
    @Path("analyze/sequencingRun/{sequencingRunId:[0-9]+}")
    @Produces("text/html")
    @GET
    public String analyzeSequencingRun(@PathParam("sequencingRunId") long sequencingRunId) {
        StringBuilder sb = new StringBuilder()
                .append("<html><head/><body>")
                .append("<p>SequencingRunId ").append(sequencingRunId).append("</p>")
                .append("<table cellpadding=\"3\">");

        // Outputs the html table header.
        sb.append(formatHeaderRow(
                "canEtl",
                "flowcellBarcode",
                "position",
                "molecularIndexingSchemeName",
                "productOrderId",
                "sampleKey",
                "researchProjectId",
                "loadedLibraryBarcode",
                "loadedLibraryCreationDate",
                "batchName"));

        // Outputs html table row for each dto.
        for (SequencingRunDto dto : extractTransform.analyzeSequencingRun(sequencingRunId)) {
            sb.append(formatRow(
                    String.valueOf(dto.canEtl()),
                    dto.getFlowcellBarcode(),
                    dto.getPosition(),
                    dto.getMolecularIndexingSchemeName(),
                    dto.getProductOrderId(),
                    dto.getSampleKey(),
                    dto.getResearchProjectId(),
                    dto.getLoadingVessel() != null ? dto.getLoadingVessel().getLabel() : "null",
                    dto.getLoadingVessel() != null && dto.getLoadingVessel().getCreatedOn() != null ?
                            ExtractTransform.secTimestampFormat.format(dto.getLoadingVessel().getCreatedOn()) : "null",
                    dto.getBatchName()
            ));
        }
        sb.append("</table></body></html>");
        return sb.toString();
    }

    /**
     * Returns an etl-type breakdown of a lab event.
     *
     * @param labEventId the entity id of the lab event
     *
     * @return htm table of what etl would put in the event fact table
     */
    @Path("analyze/event/{labEventId:[0-9]+}")
    @Produces("text/html")
    @GET
    public String analyzeEvent(@PathParam("labEventId") long labEventId) {
        Long id = null;
        String type = null;

        // Supresses the "molecularIndex" column if no indexes are given.
        boolean showMolecularBarcodes = false;
        for (EventFactDto dto : extractTransform.analyzeEvent(labEventId)) {
            id = (dto.getLabEvent() != null) ? dto.getLabEvent().getLabEventId() : null;
            type = (dto.getLabEvent() != null ? dto.getLabEvent().getLabEventType().toString() : null);
            if (!StringUtils.isBlank(dto.getSampleInstanceIndexes())) {
                showMolecularBarcodes = true;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<html><head/><body>")
                .append("<p>LabEventId ").append(id).append(", EventName ").append(type).append("</p>")
                .append("<table cellpadding=\"3\">");

        // Outputs the table header row.
        sb.append(showMolecularBarcodes ?
                formatHeaderRow("canEtl",
                        "labVessel",
                        "molecularIndex",
                        "batchName",
                        "workflowName",
                        "sample",
                        "productOrder",
                        "workflow",
                        "process",
                        "step") :
                formatHeaderRow("canEtl",
                        "labVessel",
                        "batchName",
                        "workflowName",
                        "sample",
                        "productOrder",
                        "workflow",
                        "process",
                        "step")
        );

        // Outputs a table row for each dto.
        for (EventFactDto dto : extractTransform.analyzeEvent(labEventId)) {
            sb.append(showMolecularBarcodes ?
                    formatRow(String.valueOf(dto.canEtl()),
                            dto.getLabVessel() != null ? dto.getLabVessel().getLabel() : "null",
                            dto.getSampleInstanceIndexes(),
                            dto.getBatchName(),
                            dto.getWorkflowName(),
                            dto.getSample() != null ? dto.getSample().getSampleKey() : "null",
                            dto.getProductOrder() != null ? dto.getProductOrder().getBusinessKey() : "null",
                            dto.getWfDenorm() != null ? dto.getWfDenorm().getProductWorkflowName() : "null",
                            dto.getWfDenorm() != null ? dto.getWfDenorm().getWorkflowProcessName() : "null",
                            dto.getWfDenorm() != null ? dto.getWfDenorm().getWorkflowStepName() : "null") :
                    formatRow(String.valueOf(dto.canEtl()),
                            dto.getLabVessel() != null ? dto.getLabVessel().getLabel() : "null",
                            dto.getBatchName(),
                            dto.getWorkflowName(),
                            dto.getSample() != null ? dto.getSample().getSampleKey() : "null",
                            dto.getProductOrder() != null ? dto.getProductOrder().getBusinessKey() : "null",
                            dto.getWfDenorm() != null ? dto.getWfDenorm().getProductWorkflowName() : "null",
                            dto.getWfDenorm() != null ? dto.getWfDenorm().getWorkflowProcessName() : "null",
                            dto.getWfDenorm() != null ? dto.getWfDenorm().getWorkflowStepName() : "null")
            );
        }

        sb.append("</table></body></html>");

        return sb.toString();
    }


    private String formatRow(String... columns) {
        return formatRowOrHeader("td", columns);
    }

    private String formatHeaderRow(String... columns) {
        return formatRowOrHeader("th", columns);
    }

    private String formatRowOrHeader(String element, String... columns) {
        StringBuilder sb = new StringBuilder("<tr>");
        for (String column : columns) {
            sb.append("<").append(element).append(">");
            sb.append(column);
            sb.append("</").append(element).append(">");
        }
        sb.append("</tr>");
        return sb.toString();
    }

}
