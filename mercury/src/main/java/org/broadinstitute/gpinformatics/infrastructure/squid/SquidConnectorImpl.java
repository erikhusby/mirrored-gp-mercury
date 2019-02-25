package org.broadinstitute.gpinformatics.infrastructure.squid;

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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
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

        Response response =
                JerseyUtils.getWebResource(squidConfig.getUrl() + "/resources/solexarun",
                        MediaType.APPLICATION_XML_TYPE).accept(MediaType.APPLICATION_XML)
                                                       .post(Entity.xml(runInformation));

        SquidResponse squidResponse = new SquidResponse(response.getStatus(), response.readEntity(String.class));
        response.close();
        return squidResponse;

    }

    @Override
    public SquidResponse saveReadStructure(@Nonnull ReadStructureRequest readStructureData,
                                           @Nonnull String squidWSUrl) throws WebApplicationException {

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

        Response response = JerseyUtils.getWebResource(squidWSUrl, MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML_TYPE).post(Entity.xml(solexaRunSynopsis));

        SquidResponse squidResponse = new SquidResponse(response.getStatus(), response.readEntity(String.class));
        response.close();
        return squidResponse;
    }

    @Override
    public CreateProjectOptions getProjectCreationOptions() throws WebApplicationException {

        Response response = JerseyUtils.getWebResource(
                squidConfig.getUrl() + "/resources/projectresource/projectoptions",
                MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON).get();

        CreateProjectOptions createProjectOptions = response.readEntity(CreateProjectOptions.class);
        response.close();
        return createProjectOptions;
    }

    @Override
    public CreateWorkRequestOptions getWorkRequestOptions(String executionType) throws WebApplicationException {

        Response response = JerseyUtils.getWebResource(squidConfig.getUrl() +
                                                             "/resources/projectresource/workrequestoptions/"
                                                             + executionType,
                MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON).get();

        CreateWorkRequestOptions createWorkRequestOptions = response.readEntity(CreateWorkRequestOptions.class);
        response.close();
        return createWorkRequestOptions;
    }

    @Override
    public ExecutionTypes getProjectExecutionTypes() throws WebApplicationException {

        Response response = JerseyUtils.getWebResource(
                squidConfig.getUrl() + "/resources/projectresource/executiontypes",
                MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON).get();

        ExecutionTypes executionTypes = response.readEntity(ExecutionTypes.class);
        response.close();
        return executionTypes;
    }

    @Override
    public OligioGroups getOligioGroups() throws WebApplicationException {
        Response response = JerseyUtils.getWebResource(
                squidConfig.getUrl() + "/resources/projectresource/oligioGroups",
                MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON).get();

        OligioGroups oligioGroups = response.readEntity(OligioGroups.class);
        response.close();
        return oligioGroups;
    }

    @Override
    public SampleReceptacleGroup getGroupReceptacles(String groupName) throws WebApplicationException {
        Response response =
                JerseyUtils.getWebResource(
                        squidConfig.getUrl() + "/resources/projectresource/groupReceptacles/" + groupName,
                        MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON).get();

        SampleReceptacleGroup sampleReceptacleGroup = response.readEntity(SampleReceptacleGroup.class);
        response.close();
        return sampleReceptacleGroup;
    }

    @Override
    public AutoWorkRequestOutput createSquidWorkRequest(AutoWorkRequestInput input) throws WebApplicationException {

        Response response = JerseyUtils.getWebResource(
                squidConfig.getUrl() + "/resources/projectresource/createWorkRequest",
                MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON)
                .post(Entity.json(input));

        AutoWorkRequestOutput result = null;
        if(response.getStatus() == Response.Status.OK.getStatusCode()) {
            result = response.readEntity(AutoWorkRequestOutput.class);
        } else {
            throw new InformaticsServiceException(response.readEntity(String.class));
        }
        response.close();
        return result;
    }

}

