package org.broadinstitute.gpinformatics.infrastructure.squid;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import edu.mit.broad.prodinfo.bean.generated.AutoWorkRequestInput;
import edu.mit.broad.prodinfo.bean.generated.AutoWorkRequestOutput;
import edu.mit.broad.prodinfo.bean.generated.CreateProjectOptions;
import edu.mit.broad.prodinfo.bean.generated.CreateWorkRequestOptions;
import edu.mit.broad.prodinfo.bean.generated.ExecutionTypes;
import edu.mit.broad.prodinfo.bean.generated.OligioGroups;
import edu.mit.broad.prodinfo.bean.generated.SampleReceptacleGroup;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.LaneReadStructure;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;
import org.broadinstitute.gpinformatics.mercury.squid.generated.SolexaRunSynopsisBean;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

/**
 *
 *
 *
 */
@Impl
public class SquidConnectorImpl implements SquidConnector {

    private SquidConfig squidConfig;

    @Inject
    public SquidConnectorImpl(SquidConfig squidConfig) {
        this.squidConfig = squidConfig;
    }

    @Override
    public SquidResponse createRun(SolexaRunBean runInformation) {

        ClientResponse response = getWebResource(squidConfig.getUrl() + "/resources/solexarun",
                MediaType.APPLICATION_XML_TYPE)
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
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

        ClientResponse response = getWebResource(squidWSUrl, MediaType.APPLICATION_JSON_TYPE).type(
                MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON)
                                                                                             .entity(solexaRunSynopsis)
                                                                                             .post(ClientResponse.class);

        return new SquidResponse(response.getStatus(), response.getEntity(String.class));
    }

    @Override
    public CreateProjectOptions getProjectCreationOptions() throws UniformInterfaceException {

        ClientResponse response = getWebResource(squidConfig.getUrl() + "/resources/projectresource/projectoptions",
                MediaType.APPLICATION_JSON_TYPE)
                .type(MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON).get(
                        ClientResponse.class);

        return response.getEntity(CreateProjectOptions.class);
    }

    @Override
    public CreateWorkRequestOptions getWorkRequestOptions(String executionType) throws UniformInterfaceException {

        ClientResponse response = getWebResource(squidConfig.getUrl() +
                                                 "/resources/projectresource/workrequestoptions/" + executionType,
                MediaType.APPLICATION_JSON_TYPE)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

        return response.getEntity(CreateWorkRequestOptions.class);
    }

    @Override
    public ExecutionTypes getProjectExecutionTypes() throws UniformInterfaceException {

        ClientResponse response = getWebResource(squidConfig.getUrl() + "/resources/projectresource/executiontypes",
                MediaType.APPLICATION_JSON_TYPE)
                .type(MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

        return response.getEntity(ExecutionTypes.class);
    }

    @Override
    public OligioGroups getOligioGroups() throws UniformInterfaceException {
        ClientResponse response = getWebResource(squidConfig.getUrl() + "/resources/projectresource/oligioGroups",
                MediaType.APPLICATION_JSON_TYPE)
                .type(MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

        return response.getEntity(OligioGroups.class);
    }

    @Override
    public SampleReceptacleGroup getGroupReceptacles(String groupName) throws UniformInterfaceException {
        ClientResponse response =
                getWebResource(squidConfig.getUrl() + "/resources/projectresource/groupReceptacles/" +groupName,
                        MediaType.APPLICATION_JSON_TYPE).type(MediaType.APPLICATION_JSON_TYPE)
                                                        .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

        return response.getEntity(SampleReceptacleGroup.class);
    }

    @Override
    public AutoWorkRequestOutput createSquidWorkRequest(AutoWorkRequestInput input) throws UniformInterfaceException {

        ClientResponse response = getWebResource(squidConfig.getUrl() + "/resources/projectresource/createWorkRequest",
                MediaType.APPLICATION_JSON_TYPE)
                .type(MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON).entity(input)
                .post(ClientResponse.class);

        return response.getEntity(AutoWorkRequestOutput.class);
    }

    private WebResource getWebResource(String squidWSUrl, MediaType mediaType) {
        Client client = Client.create();
        if (mediaType == MediaType.APPLICATION_JSON_TYPE) {
            ClientConfig clientConfig = new DefaultClientConfig();
            clientConfig.getClasses().add(JacksonJsonProvider.class);
            client = Client.create(clientConfig);
        }

        return client.resource(squidWSUrl);
    }
}

