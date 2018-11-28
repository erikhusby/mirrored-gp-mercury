package org.broadinstitute.gpinformatics.infrastructure.datawh;

import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.datawh.LabEventEtl.EventFactDto;
import org.broadinstitute.gpinformatics.infrastructure.datawh.SequencingSampleFactEtl.SequencingRunDto;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;

/**
 * Web service resource used to invoke ETL methods.
 */

@Path("etl")
public class ExtractTransformResource {
    private ExtractTransform extractTransform;
    private GenericDao dao;

    public ExtractTransformResource() {
    }

    @Inject
    public ExtractTransformResource(ExtractTransform extractTransform ) {
        this.extractTransform = extractTransform;
        this.dao = dao;
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
        return extractTransform.backfillEtl(entityClassname, startId, endId);
    }

    /**
     * Runs ETL on any downstream events affected by a change associated with a lab vessel. <br/>
     * This was put in place due to GPLIM-5073 where an index plate was not registered and all downstream event_fact entries had no molecular index values.
     * After registration, all downstream events required individual backfills.
     *
     * @param barcode Vessel for which all downstream event_fact and sequencing_sample_fact data must be refreshed
     */
    @Path("backfillByVessel/{barcode}")
    @PUT
    @Produces("text/plain")
    public Response backfillByVessel( @PathParam("barcode") String barcode ){
        extractTransform.initConfig();
        return extractTransform.backfillEtlForVessel(barcode);
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
        int recordCount = extractTransform.incrementalEtl(startDateTime, endDateTime);
        if (recordCount >= 0) {
            return Response.status(ClientResponse.Status.OK).entity("created " + recordCount + " records").build();
        } else {
            return Response.status(ClientResponse.Status.INTERNAL_SERVER_ERROR)
                    .entity("Problem running incremental etl").build();
        }
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
                            ExtractTransform.formatTimestamp(dto.getLoadingVessel().getCreatedOn()) : "null",
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
        int columnCount = 10;

        Collection<EventFactDto> dtos = extractTransform.analyzeEvent(labEventId);

        // Supresses the "molecularIndex" column if no indexes are given.
        boolean showMolecularBarcodes = false;
        for (EventFactDto dto : dtos) {
            if (!StringUtils.isBlank(dto.getMolecularIndex())) {
                showMolecularBarcodes = true;
                columnCount = 11;
            }
            id = dto.getEventId();
            type = dto.getEventType().toString();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<html><head/><body>")
                .append("<p>LabEventId: ").append(id).append(", EventName: ").append(type).append(", Count: ").append(dtos.size()).append("</p>")
                .append("<table cellpadding=\"3\">");

        // Outputs the table header row.
        sb.append(showMolecularBarcodes ?
                formatHeaderRow("canEtl",
                        "labVessel",
                        "vesselPosition",
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
                        "vesselPosition",
                        "batchName",
                        "workflowName",
                        "sample",
                        "productOrder",
                        "workflow",
                        "process",
                        "step")
        );

        // Outputs a table row for each dto.
        for (EventFactDto dto : dtos) {
            sb.append(showMolecularBarcodes ?
                    formatRow(String.valueOf(dto.canEtl()),
                            dto.getVesselBarcode() == null ? "null": dto.getVesselBarcode(),
                            dto.getVesselPosition(),
                            dto.getMolecularIndex() == null ? "null": dto.getMolecularIndex(),
                            dto.getBatchName() == null ? "null": dto.getBatchName(),
                            dto.getWfName() == null ? "null": dto.getWfName(),
                            dto.getLcsetSampleId() == null ? "null": dto.getLcsetSampleId(),
                            dto.getPdoName() == null ? "null": dto.getPdoName(),
                            dto.getWfName() == null ? "null": dto.getWfName(),
                            dto.getWfProcessName() == null ? "null": dto.getWfProcessName(),
                            dto.getWfStepName() == null ? "null": dto.getWfStepName() ) :
                    formatRow(String.valueOf(dto.canEtl()),
                            dto.getVesselBarcode() == null ? "null": dto.getVesselBarcode(),
                            dto.getVesselPosition(),
                            dto.getBatchName() == null ? "null": dto.getBatchName(),
                            dto.getWfName() == null ? "null": dto.getWfName(),
                            dto.getLcsetSampleId() == null ? "null": dto.getLcsetSampleId(),
                            dto.getPdoName() == null ? "null": dto.getPdoName(),
                            dto.getWfName() == null ? "null": dto.getWfName(),
                            dto.getWfProcessName() == null ? "null": dto.getWfProcessName(),
                            dto.getWfStepName() == null ? "null": dto.getWfStepName() )
            );
            buildAncestryTable(sb, columnCount, dto.getAncestryDtos());
        }

        sb.append("</table></body></html>");

        return sb.toString();
    }

    private void buildAncestryTable( StringBuilder sb, int columnCount, List<EventAncestryEtlUtil.AncestryFactDto> ancestryFactDtoList ){
        if( ancestryFactDtoList == null || ancestryFactDtoList.isEmpty() ) {
            return;
        }
        sb.append("<tr><td>&nbsp;</td><td colspan=\"").append(columnCount - 1).append("\"><table cellpadding=\"3\">");
        sb.append(
                formatHeaderRow("ancestorEventID",
                        "ancestorLibraryName",
                        "ancestorLibraryType",
                        "ancestorLibraryCreated",
                        "childEventID",
                        "childLibraryName",
                        "childLibraryType",
                        "childLibraryCreated") );
        for(EventAncestryEtlUtil.AncestryFactDto ancestryDto : ancestryFactDtoList ){
            sb.append(
                formatRow(
                        ancestryDto.getAncestorEventId().toString(),
                        ancestryDto.getAncestorVesselId().toString(),
                        ancestryDto.getAncestorLibraryTypeName(),
                        ExtractTransform.formatTimestamp(ancestryDto.getAncestorCreated()),
                        ancestryDto.getChildEventId().toString(),
                        ancestryDto.getChildVesselId().toString(),
                        ancestryDto.getChildLibraryTypeName(),
                        ExtractTransform.formatTimestamp(ancestryDto.getChildCreated() ) ) );
        }
        sb.append("</td></table></tr>");
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
