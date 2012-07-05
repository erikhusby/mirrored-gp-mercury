package org.broadinstitute.sequel.boundary.pass;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.sequel.boundary.*;
import org.broadinstitute.sequel.boundary.squid.SquidTopicPortype;
import org.broadinstitute.sequel.control.pass.PassService;
import org.broadinstitute.sequel.infrastructure.deployment.Deployment;
import org.broadinstitute.sequel.infrastructure.deployment.DeploymentProducer;
import org.broadinstitute.sequel.infrastructure.deployment.Impl;
import org.broadinstitute.sequel.infrastructure.squid.SquidConfig;
import org.broadinstitute.sequel.infrastructure.squid.SquidConfigProducer;
import org.broadinstitute.sequel.infrastructure.squid.SquidWebServiceClient;

import javax.ejb.EJB;
import javax.jws.WebParam;
import javax.jws.WebService;

import static org.broadinstitute.sequel.boundary.pass.ToSequel.sequelize;
import static org.broadinstitute.sequel.boundary.pass.ToSquid.squidify;



/**
 * This class exposes a PASS SOAP interface that mimics that of Squid.  The implementations of the web service
 * methods all live in Squid, so SequeL just delegates all calls over to Squid (except for the trivial
 * {@link #getGreeting()} method which is handled here).  Beginning with the Exome Express project we would like to
 * point PMBridge at SequeL instead of Squid.  Most calls would just pass through SequeL, but Exome Express
 * PASSes would kick off the pipeline in SequeL as well.
 *
 */
@WebService(targetNamespace = "urn:SquidTopic",
        portName = "SquidTopicService",
        serviceName = "SquidTopicService",
        name = "SquidTopicService",
        endpointInterface = "org.broadinstitute.sequel.boundary.SquidTopicPortype"
)
@Impl
public class PassServiceImpl extends SquidWebServiceClient<SquidTopicPortype> implements PassService {

    // @Injection does not work on @WebServices, see comments in constructor below
    private static final Log log = LogFactory.getLog(PassServiceImpl.class);

    @EJB
    // Using @EJB annotation since @Inject doesn't work on @WebServices, see comments below
    private DeploymentProducer deploymentProducer;


    private SquidConfig squidConfig;


    @Override
    protected String getNameSpace() {
        return "urn:SquidTopic";
    }

    @Override
    protected String getServiceName() {
        return "SquidTopicService";
    }

    @Override
    protected String getWsdlLocation() {
        return "/services/SquidTopicService?WSDL";
    }


    @Override
    protected SquidConfig getSquidConfig() {

        // I wanted to make SquidConnectionParametersProducer a @Startup @Singleton bean so I could grab it directly
        // through
        // a JNDI lookup or have it @EJB injected, but something seems to go very wrong when
        // SquidConnectionParametersProducer
        // is so annotated and the webapp will not deploy.  Unfortunately I can't find any diagnostics in the log
        // to tell me what's going wrong.
        //
        // I wanted:
        //
        // @EJB
        // SquidConnectionParametersProducer squidConfigurationProducer;

        if ( squidConfig == null ) {

            final Deployment deployment = deploymentProducer.produce();
            squidConfig = SquidConfigProducer.produce(deployment);
        }


        return squidConfig;
    }


    /**
     * JEE actually wants this no-arg constructor despite what IntelliJ says about it not being used
     */
    public PassServiceImpl() {

        // @Inject currently not working in Glassfish @WebServices, either a hole in the spec or a bug in Glassfish.
        // See
        //
        // https://community.jboss.org/message/718492
        //
        // http://java.net/jira/browse/GLASSFISH-18406

        // Link for @EJB workaround:
        //
        // http://www.coderanch.com/t/499391/Web-Services/java/JEE-Inject-ApplicationScoped-bean-WebService
        //
        //

        log.info("PassSOAPServiceImpl() constructor invoked!");
    }


    /**
     * Deployment-specific constructor, not driven by SEQUEL_DEPLOYMENT
     *
     * @param deployment
     */
    public PassServiceImpl(Deployment deployment) {

        squidConfig = SquidConfigProducer.produce(deployment);

    }


    public PassServiceImpl(SquidConfig squidConfig) {

        this.squidConfig = squidConfig;
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



/* R3_725

    @Override
    public void abandonPass(@WebParam(name = "passNumber", partName = "passNumber") String passNumber) {

        squidCall().abandonPass(passNumber);

    }
*/



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
