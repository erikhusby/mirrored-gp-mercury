package org.broadinstitute.sequel.boundary.pmbridge;

import org.broadinstitute.sequel.boundary.*;
import org.broadinstitute.sequel.infrastructure.squid.SquidConfiguration;
import org.broadinstitute.sequel.infrastructure.squid.SquidConfigurationJNDIProfileDrivenImpl;

import javax.ejb.Stateless;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URL;


@WebService(targetNamespace = "urn:SquidTopic",
        portName = "SquidTopicService",
        serviceName = "SquidTopicService",
        name = "SquidTopicService",
        endpointInterface = "org.broadinstitute.sequel.boundary.SquidTopicPortype"
)
@Stateless
public class PMBridgeSOAPServiceImpl implements SquidTopicPortype {

    // @Inject not working in Glassfish @Webservices, either a hole in the spec or a bug in Glassfish.  See
    //
    // https://community.jboss.org/message/718492
    //
    // http://java.net/jira/browse/GLASSFISH-18406
    //
    // @Inject
    // private SquidConfiguration squidConfiguration;

    private SquidConfiguration squidConfiguration = new SquidConfigurationJNDIProfileDrivenImpl();


    private org.broadinstitute.sequel.boundary.squid.SquidTopicPortype squidServicePort;

    private org.broadinstitute.sequel.boundary.squid.SquidTopicPortype getSquidServicePort() {

        if (squidServicePort == null) {
            String namespace = "urn:SquidTopic";
            QName serviceName = new QName(namespace, "SquidTopicService");

            String wsdlURL = squidConfiguration.getBaseURL() + "services/SquidTopicService?WSDL";

            URL url;
            try {
                url = new URL(wsdlURL);
            }
            catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }

            Service service = Service.create(url, serviceName);
            squidServicePort = service.getPort(serviceName, org.broadinstitute.sequel.boundary.squid.SquidTopicPortype.class);
        }

        return squidServicePort;

    }


    @Override
    public String getGreeting() {
        return "Hello PMBridge!";
    }

    @Override
    public boolean canEditPasses(@WebParam(name = "login", partName = "login") String login) {
        return getSquidServicePort().canEditPasses(login);
    }


    @Override
    // TODO
    public String storePass(@WebParam(name = "pass", partName = "pass") AbstractPass pass) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    // TODO
    public PassCritique validatePass(@WebParam(name = "pass", partName = "pass") AbstractPass pass) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public void abandonPass(@WebParam(name = "passNumber", partName = "passNumber") String passNumber) {
        getSquidServicePort().abandonPass(passNumber);
    }



    @Override
    public SummarizedPassListResult searchPassesByCreator(@WebParam(name = "creator", partName = "creator") String creator) {

        final org.broadinstitute.sequel.boundary.squid.SummarizedPassListResult summarizedPassListResult =
                getSquidServicePort().searchPassesByCreator(creator);

        return ToSequel.map(summarizedPassListResult);
    }



    @Override
    public SummarizedPassListResult searchPassesByResearchProject(@WebParam(name = "researchProject", partName = "researchProject") String researchProject) {

        final org.broadinstitute.sequel.boundary.squid.SummarizedPassListResult summarizedPassListResult =
                getSquidServicePort().searchPassesByResearchProject(researchProject);

        return ToSequel.map(summarizedPassListResult);
    }


    @Override
    public SummarizedPassListResult searchPasses() {

        final org.broadinstitute.sequel.boundary.squid.SummarizedPassListResult summarizedPassListResult =
                getSquidServicePort().searchPasses();

        return ToSequel.map(summarizedPassListResult);
    }


    @Override
    public ReferenceSequenceListResult getReferenceSequencesByOrganism(@WebParam(name = "organism", partName = "organism") Organism organism) {

        final org.broadinstitute.sequel.boundary.squid.Organism squidOrganism = ToSquid.map(organism);

        final org.broadinstitute.sequel.boundary.squid.ReferenceSequenceListResult refseqs =
                getSquidServicePort().getReferenceSequencesByOrganism(squidOrganism);

        return ToSequel.map(refseqs);
    }

    @Override
    public ReferenceSequenceListResult getReferenceSequencesByBaitSet(@WebParam(name = "baitSet", partName = "baitSet") BaitSet baitSet) {

        final org.broadinstitute.sequel.boundary.squid.BaitSet squidBaitSet = ToSquid.map(baitSet);

        final org.broadinstitute.sequel.boundary.squid.ReferenceSequenceListResult refseqs =
                getSquidServicePort().getReferenceSequencesByBaitSet(squidBaitSet);

        return ToSequel.map(refseqs);
    }


    @Override
    public ReferenceSequenceListResult getReferenceSequences() {
        return ToSequel.map(getSquidServicePort().getReferenceSequences());
    }



    @Override
    public BaitSetListResult getBaitSetsByReferenceSequence(@WebParam(name = "referenceSequence", partName = "referenceSequence") ReferenceSequence referenceSequence) {

        final org.broadinstitute.sequel.boundary.squid.ReferenceSequence squidRefseq = ToSquid.map(referenceSequence);

        return ToSequel.map(getSquidServicePort().getBaitSetsByReferenceSequence(squidRefseq));
    }


    @Override
    public BaitSetListResult getBaitSets() {

        final org.broadinstitute.sequel.boundary.squid.BaitSetListResult baitSets = getSquidServicePort().getBaitSets();

        return ToSequel.map(baitSets);
    }



    @Override
    public OrganismListResult getOrganisms() {
        return ToSequel.map(getSquidServicePort().getOrganisms());
    }



    @Override
    // TODO
    public AbstractPass loadPassByNumber(@WebParam(name = "passNumber", partName = "passNumber") String passNumber) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }



    @Override
    public SquidPersonList getBroadPIList() {
        return ToSequel.map(getSquidServicePort().getBroadPIList());
    }


    @Override
    public boolean validatePassNumber(@WebParam(name = "passNumber", partName = "passNumber") String passNumber) {
        return getSquidServicePort().validatePassNumber(passNumber);
    }
}
