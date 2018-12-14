package org.broadinstitute.gpinformatics.infrastructure.squid;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import edu.mit.broad.prodinfo.bean.generated.AutoWorkRequestInput;
import edu.mit.broad.prodinfo.bean.generated.AutoWorkRequestOutput;
import edu.mit.broad.prodinfo.bean.generated.CreateProjectOptions;
import edu.mit.broad.prodinfo.bean.generated.CreateWorkRequestOptions;
import edu.mit.broad.prodinfo.bean.generated.ExecutionTypes;
import edu.mit.broad.prodinfo.bean.generated.OligioGroups;
import edu.mit.broad.prodinfo.bean.generated.SampleReceptacleGroup;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.control.JerseyUtils;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.LaneReadStructure;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;
import org.broadinstitute.gpinformatics.mercury.squid.generated.SolexaRunSynopsisBean;

import javax.annotation.Nonnull;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 *
 *
 */
@Dependent
@Default
public class SquidConnectorImpl implements SquidConnector {

    private SquidConfig squidConfig;

    @Inject
    public SquidConnectorImpl(SquidConfig squidConfig) {
        this.squidConfig = squidConfig;
    }

    @Override
    public SquidResponse createRun(SolexaRunBean runInformation) {

        ClientResponse response =
                JerseyUtils.getWebResource(squidConfig.getUrl() + "/resources/solexarun",
                        MediaType.APPLICATION_XML_TYPE).accept(MediaType.APPLICATION_XML)
                                                       .entity(runInformation).post(ClientResponse.class);

        return new SquidResponse(response.getStatus(), response.getEntity(String.class));

    }

    @Override
    public SquidResponse saveReadStructure(@Nonnull ReadStructureRequest readStructureData,
                                           @Nonnull String squidWSUrl) throws UniformInterfaceException {

        SolexaRunSynopsisBean solexaRunSynopsis = new SolexaRunSynopsisBean();
        solexaRunSynopsis.setRunBarcode(readStructureData.getRunBarcode());
        solexaRunSynopsis.setRunName(readStructureData.getRunName());
        solexaRunSynopsis.setLanesSequenced(readStructureData.getLanesSequenced());
        solexaRunSynopsis.setActualReadStructure(readStructureData.getActualReadStructure());
        solexaRunSynopsis.setSetupReadStructure(readStructureData.getSetupReadStructure());
        solexaRunSynopsis.setImagedAreaPerLaneMM2(readStructureData.getImagedArea());
        for (LaneReadStructure laneReadStructure : readStructureData.getLaneStructures()) {
            SolexaRunSynopsisBean.SolexaRunLaneSynopsisBean solexaRunLaneSynopsisBean =
                    new SolexaRunSynopsisBean.SolexaRunLaneSynopsisBean();
            solexaRunLaneSynopsisBean.setLaneNumber(laneReadStructure.getLaneNumber());
            solexaRunLaneSynopsisBean.setActualReadStructure(laneReadStructure.getActualReadStructure());
            solexaRunSynopsis.getSolexaRunLaneSynopsisBean().add(solexaRunLaneSynopsisBean);
        }

        ClientResponse response =
                JerseyUtils.getWebResource(squidWSUrl, MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON)
                                                                           .entity(solexaRunSynopsis)
                                                                           .post(ClientResponse.class);

        return new SquidResponse(response.getStatus(), response.getEntity(String.class));
    }

    @Override
    public CreateProjectOptions getProjectCreationOptions() throws UniformInterfaceException {

        ClientResponse response = JerseyUtils.getWebResource(
                squidConfig.getUrl() + "/resources/projectresource/projectoptions",
                MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

        return response.getEntity(CreateProjectOptions.class);
    }

    @Override
    public CreateWorkRequestOptions getWorkRequestOptions(String executionType) throws UniformInterfaceException {

        ClientResponse response = JerseyUtils.getWebResource(squidConfig.getUrl() +
                                                             "/resources/projectresource/workrequestoptions/"
                                                             + executionType,
                MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

        return response.getEntity(CreateWorkRequestOptions.class);
    }

    @Override
    public ExecutionTypes getProjectExecutionTypes() throws UniformInterfaceException {

        ClientResponse response = JerseyUtils.getWebResource(
                squidConfig.getUrl() + "/resources/projectresource/executiontypes",
                MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

        return response.getEntity(ExecutionTypes.class);
    }

    @Override
    public OligioGroups getOligioGroups() throws UniformInterfaceException {
        ClientResponse response = JerseyUtils.getWebResource(
                squidConfig.getUrl() + "/resources/projectresource/oligioGroups",
                MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

        return response.getEntity(OligioGroups.class);
    }

    @Override
    public SampleReceptacleGroup getGroupReceptacles(String groupName) throws UniformInterfaceException {
        ClientResponse response =
                JerseyUtils.getWebResource(
                        squidConfig.getUrl() + "/resources/projectresource/groupReceptacles/" + groupName,
                        MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

        return response.getEntity(SampleReceptacleGroup.class);
    }

    @Override
    public AutoWorkRequestOutput createSquidWorkRequest(AutoWorkRequestInput input) throws UniformInterfaceException {

        ClientResponse response = JerseyUtils.getWebResource(
                squidConfig.getUrl() + "/resources/projectresource/createWorkRequest",
                MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON).entity(input)
                .post(ClientResponse.class);

        AutoWorkRequestOutput result = null;
        if(response.getStatus() == Response.Status.OK.getStatusCode()) {
            result = response.getEntity(AutoWorkRequestOutput.class);
        } else {
            throw new InformaticsServiceException(response.getEntity(String.class));
        }
        return result;
    }

}

