package org.broadinstitute.sequel.boundary.pmbridge;

import org.broadinstitute.sequel.boundary.*;

import javax.jws.WebParam;
import javax.jws.WebService;


@WebService(targetNamespace = "urn:SquidTopic",
        portName = "SquidTopicService",
        serviceName = "SquidTopicService",
        name = "SquidTopicService",
        endpointInterface = "org.broadinstitute.sequel.boundary.SquidTopicPortype"
)
public class PMBridgeSOAPServiceImpl implements SquidTopicPortype {


    @Override
    public String getGreeting() {
        return "Hello PMBridge!";
    }

    @Override
    public boolean canEditPasses(@WebParam(name = "login", partName = "login") String login) {
        return true;
    }

    @Override
    public String storePass(@WebParam(name = "pass", partName = "pass") AbstractPass pass) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public PassCritique validatePass(@WebParam(name = "pass", partName = "pass") AbstractPass pass) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void abandonPass(@WebParam(name = "passNumber", partName = "passNumber") String passNumber) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SummarizedPassListResult searchPassesByCreator(@WebParam(name = "creator", partName = "creator") String creator) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SummarizedPassListResult searchPassesByResearchProject(@WebParam(name = "researchProject", partName = "researchProject") String researchProject) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SummarizedPassListResult searchPasses() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ReferenceSequenceListResult getReferenceSequencesByOrganism(@WebParam(name = "organism", partName = "organism") Organism organism) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ReferenceSequenceListResult getReferenceSequencesByBaitSet(@WebParam(name = "baitSet", partName = "baitSet") BaitSet baitSet) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ReferenceSequenceListResult getReferenceSequences() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public BaitSetListResult getBaitSetsByReferenceSequence(@WebParam(name = "referenceSequence", partName = "referenceSequence") ReferenceSequence referenceSequence) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public BaitSetListResult getBaitSets() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public OrganismListResult getOrganisms() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public AbstractPass loadPassByNumber(@WebParam(name = "passNumber", partName = "passNumber") String passNumber) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SquidPersonList getBroadPIList() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean validatePassNumber(@WebParam(name = "passNumber", partName = "passNumber") String passNumber) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
