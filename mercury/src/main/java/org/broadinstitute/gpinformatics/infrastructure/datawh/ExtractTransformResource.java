package org.broadinstitute.gpinformatics.infrastructure.datawh;

import com.sun.jersey.api.client.ClientResponse;

import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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


}
