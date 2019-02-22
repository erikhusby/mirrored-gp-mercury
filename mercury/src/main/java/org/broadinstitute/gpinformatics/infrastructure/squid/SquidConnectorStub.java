package org.broadinstitute.gpinformatics.infrastructure.squid;

import edu.mit.broad.prodinfo.bean.generated.AutoWorkRequestInput;
import edu.mit.broad.prodinfo.bean.generated.AutoWorkRequestOutput;
import edu.mit.broad.prodinfo.bean.generated.CreateProjectOptions;
import edu.mit.broad.prodinfo.bean.generated.CreateWorkRequestOptions;
import edu.mit.broad.prodinfo.bean.generated.ExecutionTypes;
import edu.mit.broad.prodinfo.bean.generated.OligioGroups;
import edu.mit.broad.prodinfo.bean.generated.SampleReceptacleGroup;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;

import javax.annotation.Nonnull;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * Stub for Squid Connector testing (non-CDI managed)
 */
public class SquidConnectorStub implements SquidConnector{

    @Override
    public SquidResponse createRun(SolexaRunBean runInformation) {
        return new SquidConnector.SquidResponse(Response.Status.CREATED.getStatusCode(), "");
    }

    @Override
    public SquidResponse saveReadStructure(@Nonnull ReadStructureRequest readStructureData, @Nonnull String squidWSUrl)
            throws WebApplicationException {
        return new SquidConnector.SquidResponse(Response.Status.CREATED.getStatusCode(), "");
    }

    @Override
    public CreateProjectOptions getProjectCreationOptions() throws WebApplicationException {
        return new CreateProjectOptions();
    }


    @Override
    public CreateWorkRequestOptions getWorkRequestOptions(String executionType) throws WebApplicationException {
        return new CreateWorkRequestOptions();
    }

    @Override
    public ExecutionTypes getProjectExecutionTypes() throws WebApplicationException {
        return new ExecutionTypes();
    }

    @Override
    public AutoWorkRequestOutput createSquidWorkRequest(AutoWorkRequestInput input) throws WebApplicationException {
        return new AutoWorkRequestOutput();
    }

    @Override
    public OligioGroups getOligioGroups() throws WebApplicationException {
        return null;
    }

    @Override
    public SampleReceptacleGroup getGroupReceptacles(String groupName) throws WebApplicationException {
        return null;
    }
}
