package org.broadinstitute.gpinformatics.infrastructure.squid;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.LaneReadStructure;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;
import org.broadinstitute.gpinformatics.mercury.squid.generated.SolexaRunLaneSynopsisBean;
import org.broadinstitute.gpinformatics.mercury.squid.generated.SolexaRunSynopsisBean;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

/**
 * @author Scott Matthews
 *         Date: 3/11/13
 *         Time: 1:46 PM
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

        ClientResponse response = Client.create().resource(squidConfig.getUrl() + "/resources/solexarun")
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
        solexaRunSynopsis.setLanesSequenced(readStructureData.getLanesSequenced());
        solexaRunSynopsis.setActualReadStructure(readStructureData.getActualReadStructure());
        solexaRunSynopsis.setSetupReadStructure(readStructureData.getSetupReadStructure());
        solexaRunSynopsis.setImagedAreaPerLaneMM2(readStructureData.getImagedArea());
        for (LaneReadStructure laneReadStructure : readStructureData.getLaneStructures()) {
            SolexaRunLaneSynopsisBean solexaRunLaneSynopsisBean = new SolexaRunLaneSynopsisBean();
            solexaRunLaneSynopsisBean.setLane(laneReadStructure.getLaneNumber().toString());
            solexaRunLaneSynopsisBean.setActualReadStructure(laneReadStructure.getActualReadStructure());
            solexaRunSynopsis.getSolexaRunLaneSynopsisBean().add(solexaRunLaneSynopsisBean);
        }


        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getClasses().add(JacksonJsonProvider.class);

        ClientResponse response = Client.create(clientConfig).resource(squidWSUrl)
                .type(MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON).entity(solexaRunSynopsis)
                .post(ClientResponse.class);

        return new SquidResponse(response.getStatus(), response.getEntity(String.class));
    }


}
