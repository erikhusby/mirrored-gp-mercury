package org.broadinstitute.pmbridge.infrastructure.gap;

import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.pmbridge.entity.experiments.gap.GapExperimentRequest;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.infrastructure.SubmissionException;
import org.broadinstitute.pmbridge.infrastructure.ValidationException;

import javax.enterprise.inject.Default;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/29/12
 * Time: 1:20 PM
 */
@Default
public class MockGenotypingService implements GenotypingService {


    public MockGenotypingService() {
    }

    @Override
    public GapExperimentRequest getPlatformRequest(final ExperimentRequestSummary experimentRequestSummary) {
        //TODO
        throw new IllegalStateException("Not Yet Implemented");
        //return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public GapExperimentRequest saveExperimentRequest(final Person programMgr, final GapExperimentRequest gapExperimentRequest) throws ValidationException, SubmissionException {
        //TODO
        throw new IllegalStateException("Not Yet Implemented");
        //return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public GapExperimentRequest submitExperimentRequest(final Person programMgr, final GapExperimentRequest gapExperimentRequest) throws ValidationException, SubmissionException {
        //TODO
        throw new IllegalStateException("Not Yet Implemented");
        //return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<ExperimentRequestSummary> getRequestSummariesByCreator(final Person creator) {
        //TODO
        throw new IllegalStateException("Not Yet Implemented");
        //return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Product lookupTechnologyProductById(final Integer productId) throws ProductNotFoundException {

        return new Product("SuperChipCytoSNP-12v1-0_D", "Super Chip", "007");
    }

    //    protected Client getJerseyClient() {
//
//        DefaultClientConfig clientConfig = new DefaultClientConfig();
//        customizeConfig(clientConfig);
//
//        Client mockClient = EasyMock.createMock(  Client.class ).create( clientConfig );
//        WebResource mockWebResource = EasyMock.createMock(WebResource.class);
//        EasyMock.reset(mockClient);
//        EasyMock.reset(mockWebResource);
//        EasyMock.expect( mockClient.resource((String) EasyMock.anyObject()) ).andReturn( (WebResource) EasyMock.anyObject() ).atLeastOnce();
//
//        EasyMock.replay(mockClient);
//
//        customizeClient(mockClient);
//
//        return mockClient;
//    }


}
