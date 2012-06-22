package org.broadinstitute.sequel.boundary.pass;

import org.broadinstitute.sequel.boundary.*;
import org.broadinstitute.sequel.infrastructure.squid.PassSquidWSConnector;
import org.broadinstitute.sequel.infrastructure.squid.SquidConfiguration;
import org.broadinstitute.sequel.infrastructure.squid.SquidConfigurationJNDIProfileDrivenImpl;

import javax.inject.Inject;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URL;

import static org.broadinstitute.sequel.boundary.pass.ToSequel.sequelize;
import static org.broadinstitute.sequel.boundary.pass.ToSquid.squidify;


@WebService(targetNamespace = "urn:SquidTopic",
        portName = "SquidTopicService",
        serviceName = "SquidTopicService",
        name = "SquidTopicService",
        endpointInterface = "org.broadinstitute.sequel.boundary.SquidTopicPortype"
)
public class PassSOAPService implements SquidTopicPortype {

    // @Inject not working in Glassfish @Webservices, either a hole in the spec or a bug in Glassfish.  See
    //
    // https://community.jboss.org/message/718492
    //
    // http://java.net/jira/browse/GLASSFISH-18406
    //
    // @Inject
    // private SquidConfiguration squidConfiguration;

    @Inject
    PassSquidWSConnector wsConnector;



    @Override
    public String getGreeting() {
        // the one method I'm not proxying
        return "Hello PMBridge!";
    }


    @Override
    public boolean canEditPasses(@WebParam(name = "login", partName = "login") String login) {
        return this.wsConnector.squidCall().canEditPasses(login);
    }



    @Override
    public String storePass(@WebParam(name = "pass", partName = "pass") AbstractPass pass) {
        return this.wsConnector.squidCall().storePass(squidify(pass));
    }



    @Override
    public PassCritique validatePass(@WebParam(name = "pass", partName = "pass") AbstractPass pass) {

        return sequelize(this.wsConnector.squidCall().validatePass(squidify(pass)));

    }


    @Override
    public void abandonPass(@WebParam(name = "passNumber", partName = "passNumber") String passNumber) {

        this.wsConnector.squidCall().abandonPass(passNumber);

    }



    @Override
    public SummarizedPassListResult searchPassesByCreator(@WebParam(name = "creator", partName = "creator") String creator) {

        return sequelize(this.wsConnector.squidCall().searchPassesByCreator(creator));
    }



    @Override
    public SummarizedPassListResult searchPassesByResearchProject(@WebParam(name = "researchProject", partName = "researchProject") String researchProject) {

        return sequelize(this.wsConnector.squidCall().searchPassesByResearchProject(researchProject));
    }


    @Override
    public SummarizedPassListResult searchPasses() {

        return sequelize(this.wsConnector.squidCall().searchPasses());
    }


    @Override
    public ReferenceSequenceListResult getReferenceSequencesByOrganism(@WebParam(name = "organism", partName = "organism") Organism organism) {

        return sequelize(this.wsConnector.squidCall().getReferenceSequencesByOrganism(squidify(organism)));
    }


    @Override
    public ReferenceSequenceListResult getReferenceSequencesByBaitSet(@WebParam(name = "baitSet", partName = "baitSet") BaitSet baitSet) {

        return sequelize(this.wsConnector.squidCall().getReferenceSequencesByBaitSet(squidify(baitSet)));
    }



    @Override
    public ReferenceSequenceListResult getReferenceSequences() {

        return sequelize(this.wsConnector.squidCall().getReferenceSequences());

    }



    @Override
    public BaitSetListResult getBaitSetsByReferenceSequence(@WebParam(name = "referenceSequence", partName = "referenceSequence") ReferenceSequence referenceSequence) {

        return sequelize(this.wsConnector.squidCall().getBaitSetsByReferenceSequence(squidify(referenceSequence)));
    }


    @Override
    public BaitSetListResult getBaitSets() {

        return sequelize(this.wsConnector.squidCall().getBaitSets());
    }



    @Override
    public OrganismListResult getOrganisms() {

        return sequelize(this.wsConnector.squidCall().getOrganisms());

    }



    @Override
    public AbstractPass loadPassByNumber(@WebParam(name = "passNumber", partName = "passNumber") String passNumber) {

        return sequelize(this.wsConnector.squidCall().loadPassByNumber(passNumber));

    }



    @Override
    public SquidPersonList getBroadPIList() {

        return sequelize(this.wsConnector.squidCall().getBroadPIList());

    }


    @Override
    public boolean validatePassNumber(@WebParam(name = "passNumber", partName = "passNumber") String passNumber) {

        return this.wsConnector.squidCall().validatePassNumber(passNumber);

    }


}
