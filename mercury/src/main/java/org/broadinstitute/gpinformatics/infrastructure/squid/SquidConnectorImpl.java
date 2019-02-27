package org.broadinstitute.gpinformatics.infrastructure.squid;

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

        Response response = JerseyUtils.getWebResource(squidWSUrl, MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(solexaRunSynopsis));

        SquidResponse squidResponse = new SquidResponse(response.getStatus(), response.readEntity(String.class));
        response.close();
        return squidResponse;
    }

}

