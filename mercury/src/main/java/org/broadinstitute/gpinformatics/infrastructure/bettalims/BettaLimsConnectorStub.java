package org.broadinstitute.gpinformatics.infrastructure.bettalims;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.LibraryQuantRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.QpcrRunBean;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.ws.rs.core.Response;

/**
 * Stub for BettaLims Connector
 */
@Stub
@Alternative
@Dependent
public class BettaLimsConnectorStub implements BettaLimsConnector {

    public BettaLimsConnectorStub(){}

    @Override
    public BettaLimsResponse sendMessage(String message) {
        return new BettaLimsResponse(500, "");
    }

    @Override
    public Response createQpcrRun(QpcrRunBean qpcrRunBean) {
        return  Response.status(500).build();
    }

    @Override
    public Response createLibraryQuants(LibraryQuantRunBean qpcrRunBean) {
        return Response.status(500).build();
    }
}
