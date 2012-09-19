package org.broadinstitute.gpinformatics.mercury.infrastructure.gap;

import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.gpinformatics.athena.entity.experiments.gap.GapExperimentRequest;
import org.broadinstitute.gpinformatics.athena.entity.person.Person;
import org.broadinstitute.gpinformatics.mercury.infrastructure.SubmissionException;
import org.broadinstitute.gpinformatics.mercury.infrastructure.ValidationException;

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

    // Snapshot of web service output on 7/3/2012, using URL:
    // http://gap/ws/project_management/get_experiment_platforms
    private static final String PLATFORM_XML_SOURCE = "<platforms>\n"
            +"    <platform>\n"
            +"        <name>HT Genotyping</name>\n"
            +"        <vendor>Illumina</vendor>\n"
            +"        <products>\n"
            +"            <product name=\"Omni1M\" active=\"true\"/>\n"
            +"            <product name=\"Omni Express\" active=\"true\"/>\n"
            +"            <product name=\"Omni5M\" active=\"true\"/>\n"
            +"            <product name=\"Methylation 450k\" active=\"true\"/>\n"
            +"            <product name=\"Metabochip\" active=\"true\"/>\n"
            +"            <product name=\"1KG Human Exome\" active=\"true\"/>\n"
            +"            <product name=\"Cyto-12\" active=\"true\"/>\n"
            +"            <product name=\"Omni Express + Exome\" active=\"true\"/>\n"
            +"            <product name=\"Omni2.5M-8\" active=\"true\"/>\n"
            +"            <product name=\"Human Exome\" active=\"false\"/>\n"
            +"            <product name=\"Omni 1M Quad\" active=\"true\"/>\n"
            +"            <product name=\"Canine Array\" active=\"true\"/>\n"
            +"            <product name=\"Immunochip\" active=\"true\"/>\n"
            +"        </products>\n"
            +"    </platform>\n"
            +"</platforms>";

    @Override
    public Platforms getPlatforms() {
        return ObjectMarshaller.unmarshall(Platforms.class, PLATFORM_XML_SOURCE);
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
