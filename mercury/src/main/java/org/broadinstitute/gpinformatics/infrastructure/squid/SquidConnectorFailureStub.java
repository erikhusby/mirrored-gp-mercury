package org.broadinstitute.gpinformatics.infrastructure.squid;

import com.sun.jersey.api.client.UniformInterfaceException;
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
import javax.ws.rs.core.Response;

/**
 * Stub for Squid Connector testing (non-CDI managed)
 */
public class SquidConnectorFailureStub implements SquidConnector {

    @Override
    public SquidResponse createRun(SolexaRunBean runInformation) {
        return new SquidResponse(Response.Status.BAD_REQUEST.getStatusCode(), "");
    }

    @Override
    public SquidResponse saveReadStructure(@Nonnull ReadStructureRequest readStructureData, @Nonnull String squidWSUrl)
            throws UniformInterfaceException {
        return new SquidResponse(Response.Status.BAD_REQUEST.getStatusCode(), "");
    }

    @Override
    public CreateProjectOptions getProjectCreationOptions() throws UniformInterfaceException {
        return null;
    }

    @Override
    public CreateWorkRequestOptions getWorkRequestOptions(String executionType) throws UniformInterfaceException {
        return null;
    }

    @Override
    public ExecutionTypes getProjectExecutionTypes() throws UniformInterfaceException {
        return null;
    }

    @Override
    public AutoWorkRequestOutput createSquidWorkRequest(AutoWorkRequestInput input) throws UniformInterfaceException {
        return null;
    }

    @Override
    public OligioGroups getOligioGroups() throws UniformInterfaceException {
        return null;
    }

    @Override
    public SampleReceptacleGroup getGroupReceptacles(String groupName) throws UniformInterfaceException {
        return null;
    }
}
