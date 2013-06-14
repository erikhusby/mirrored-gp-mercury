package org.broadinstitute.gpinformatics.infrastructure.datawh;

import com.sun.jersey.api.client.ClientResponse;
import org.broadinstitute.gpinformatics.infrastructure.datawh.LabEventEtl.EventFactDto;
import org.broadinstitute.gpinformatics.infrastructure.datawh.SequencingSampleFactEtl.SequencingRunDto;

import javax.inject.Inject;
import javax.ws.rs.*;
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
        * @param startId First entity id of a range of ids to backfill.  Set to 0 for minimum.
        * @param endId Last entity id of a range of ids to backfill.  Set to -1 for maximum.
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
     * @param endDateTime end of interval of audited changes, in yyyyMMddHHmmss format, or "0" for now.
     *                    Excludes endpoint.  "0" will withhold updating the lastEtlRun file.
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
     * @return htm table of what etl would put in the sequencing sample fact table.
     */
    @Path("analyze/sequencingRun/{sequencingRunId:[0-9]+}")
    @Produces("text/html")
    @GET
    public String analyzeSequencingRun(@PathParam("sequencingRunId") long sequencingRunId) {
        StringBuilder sb = new StringBuilder()
                .append("<html><head/><body>")
                .append("<p>SequencingRunId " + sequencingRunId + "</p>")
                .append("<table cellpadding=\"3\">")
                .append("<tr><th>canEtl")
                .append("</th><th>flowcellBarcode")
                .append("</th><th>position")
                .append("</th><th>molecularIndexingSchemeName")
                .append("</th><th>productOrderId")
                .append("</th><th>sampleKey")
                .append("</th><th>researchProjectId")
                .append("</th></tr>");

        for (SequencingRunDto dto : extractTransform.analyzeSequencingRun(sequencingRunId)) {
            sb.append("<tr><td>")
                    .append(dto.isComplete())
                    .append("</td><td>")
                    .append(dto.getFlowcellBarcode())
                    .append("</td><td>")
                    .append(dto.getPosition())
                    .append("</td><td>")
                    .append(dto.getMolecularIndexingSchemeName())
                    .append("</td><td>")
                    .append(dto.getProductOrderId())
                    .append("</td><td>")
                    .append(dto.getSampleKey())
                    .append("</td><td>")
                    .append(dto.getResearchProjectId())
                    .append("</td><tr>");
        }
        sb.append("</table></body></html>");
        return sb.toString();
    }

    /**
     * Returns an etl-type breakdown of a lab event.
     *
     * @param labEventId the entity id of the lab event
     * @return htm table of what etl would put in the event fact table
     */
    @Path("analyze/event/{labEventId:[0-9]+}")
    @Produces("text/html")
    @GET
    public String analyzeEvent(@PathParam("labEventId") long labEventId) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head/><body><table cellpadding=\"3\">")
                .append("<tr><th>canEtl")
                .append("</th><th>labEventId")
                .append("</th><th>eventName")
                .append("</th><th>labVessel")
                .append("</th><th>molecularBarcodes")
                .append("</th><th>labBatch")
                .append("</th><th>labBatchId")
                .append("</th><th>workflowName")
                .append("</th><th>sample")
                .append("</th><th>productOrder")
                .append("</th><th>workflow")
                .append("</th><th>process")
                .append("</th><th>step")
                .append("</th></tr>");

        for (EventFactDto dto : extractTransform.analyzeEvent(labEventId)) {
            sb.append("<tr><td>").append(dto.isComplete())
                    .append("</td><td>")
                    .append(dto.getLabEvent() != null ? dto.getLabEvent().getLabEventId() : null)
                    .append("</td><td>")
                    .append(dto.getLabEvent() != null ? dto.getLabEvent().getLabEventType().toString() : null)
                    .append("</td><td>")
                    .append(dto.getLabVessel() != null ? dto.getLabVessel().getLabel() : null)
                    .append("</td><td>")
                    .append(dto.getSampleInstanceIndexes())
                    .append("</td><td>")
                    .append(dto.getLabBatch() != null ? dto.getLabBatch().getBusinessKey() : null)
                    .append("</td><td>")
                    .append(dto.getLabBatchId())
                    .append("</td><td>")
                    .append(dto.getLabBatch() != null ? dto.getLabBatch().getWorkflowName() : null)
                    .append("</td><td>")
                    .append(dto.getSample() != null ? dto.getSample().getSampleKey() : null)
                    .append("</td><td>")
                    .append(dto.getProductOrder() != null ? dto.getProductOrder().getBusinessKey() : null)
                    .append("</td><td>")
                    .append(dto.getWfDenorm() != null ? dto.getWfDenorm().getProductWorkflowName() : null)
                    .append("</td><td>")
                    .append(dto.getWfDenorm() != null ? dto.getWfDenorm().getWorkflowProcessName() : null)
                    .append("</td><td>")
                    .append(dto.getWfDenorm() != null ? dto.getWfDenorm().getWorkflowStepName() : null)
                    .append("</td></tr>");
        }
        sb.append("</table></body></html>");

        return sb.toString();
    }

}
