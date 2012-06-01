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

import static org.broadinstitute.sequel.boundary.pmbridge.ToSequel.sequelize;
import static org.broadinstitute.sequel.boundary.pmbridge.ToSquid.squidify;


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

    private org.broadinstitute.sequel.boundary.squid.SquidTopicPortype squidCall() {

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
        // the one method I'm not proxying
        return "Hello PMBridge!";
    }


    @Override
    public boolean canEditPasses(@WebParam(name = "login", partName = "login") String login) {
        return squidCall().canEditPasses(login);
    }



    @Override
    public String storePass(@WebParam(name = "pass", partName = "pass") AbstractPass pass) {
        return squidCall().storePass(squidify(pass));
    }



    @Override
    public PassCritique validatePass(@WebParam(name = "pass", partName = "pass") AbstractPass pass) {

        return sequelize(squidCall().validatePass(squidify(pass)));

    }


    @Override
    public void abandonPass(@WebParam(name = "passNumber", partName = "passNumber") String passNumber) {

        squidCall().abandonPass(passNumber);

    }



    @Override
    public SummarizedPassListResult searchPassesByCreator(@WebParam(name = "creator", partName = "creator") String creator) {

        return sequelize(squidCall().searchPassesByCreator(creator));
    }



    @Override
    public SummarizedPassListResult searchPassesByResearchProject(@WebParam(name = "researchProject", partName = "researchProject") String researchProject) {

        return sequelize(squidCall().searchPassesByResearchProject(researchProject));
    }


    @Override
    public SummarizedPassListResult searchPasses() {

        return sequelize(squidCall().searchPasses());
    }


    @Override
    public ReferenceSequenceListResult getReferenceSequencesByOrganism(@WebParam(name = "organism", partName = "organism") Organism organism) {

        return sequelize(squidCall().getReferenceSequencesByOrganism(squidify(organism)));
    }


    @Override
    public ReferenceSequenceListResult getReferenceSequencesByBaitSet(@WebParam(name = "baitSet", partName = "baitSet") BaitSet baitSet) {

        return sequelize(squidCall().getReferenceSequencesByBaitSet(squidify(baitSet)));
    }



    @Override
    public ReferenceSequenceListResult getReferenceSequences() {

        return sequelize(squidCall().getReferenceSequences());

    }



    @Override
    public BaitSetListResult getBaitSetsByReferenceSequence(@WebParam(name = "referenceSequence", partName = "referenceSequence") ReferenceSequence referenceSequence) {

        return sequelize(squidCall().getBaitSetsByReferenceSequence(squidify(referenceSequence)));
    }


    @Override
    public BaitSetListResult getBaitSets() {

        return sequelize(squidCall().getBaitSets());
    }



    @Override
    public OrganismListResult getOrganisms() {

        return sequelize(squidCall().getOrganisms());

    }



    @Override
    public AbstractPass loadPassByNumber(@WebParam(name = "passNumber", partName = "passNumber") String passNumber) {

        return sequelize(squidCall().loadPassByNumber(passNumber));

    }



    @Override
    public SquidPersonList getBroadPIList() {

        return sequelize(squidCall().getBroadPIList());

    }


    @Override
    public boolean validatePassNumber(@WebParam(name = "passNumber", partName = "passNumber") String passNumber) {

        return squidCall().validatePassNumber(passNumber);

    }


}
