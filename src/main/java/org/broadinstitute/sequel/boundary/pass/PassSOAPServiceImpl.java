package org.broadinstitute.sequel.boundary.pass;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.sequel.boundary.*;
import org.broadinstitute.sequel.control.pass.PassService;
import org.broadinstitute.sequel.infrastructure.deployment.Deployment;
import org.broadinstitute.sequel.infrastructure.deployment.DeploymentConfig;
import org.broadinstitute.sequel.infrastructure.deployment.Impl;
import org.broadinstitute.sequel.infrastructure.squid.SquidConfiguration;
import org.broadinstitute.sequel.infrastructure.squid.SquidConfigurationProducer;

import javax.ejb.EJB;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URL;

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
public class PassSOAPServiceImpl implements PassService {

    // @Injection does not work on @WebServices, see comments in constructor below
    private static final Log log = LogFactory.getLog(PassSOAPServiceImpl.class);

    private String baseUrl;

    @EJB
    // Using @EJB annotation since @Inject doesn't work on @WebServices, see comments below
    private DeploymentConfig deploymentConfig;



    public PassSOAPServiceImpl() {

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


    public PassSOAPServiceImpl(Deployment deployment) {

        final SquidConfiguration squidConfiguration = SquidConfigurationProducer.produce(deployment);

        baseUrl = squidConfiguration.getBaseUrl();
    }


    private String getBaseUrl() {

        // I wanted to make SquidConfigurationProducer a @Startup @Singleton bean so I could grab it directly through
        // a JNDI lookup or have it @EJB injected, but something seems to go very wrong when SquidConfigurationProducer
        // is so annotated and the webapp will not deploy.  Unfortunately I can't find any diagnostics in the log
        // to tell me what's going wrong.
        //
        // I wanted:
        //
        // @EJB
        // SquidConfigurationProducer squidConfigurationProducer;
        //
        //
        // which would have made the code below look like:
        //
        // baseUrl = squidConfigurationProducer.produce().getDeployment();






        if (baseUrl == null) {

            Deployment deployment = deploymentConfig.getDeployment();

            final SquidConfiguration squidConfiguration = SquidConfigurationProducer.produce(deployment);

            baseUrl = squidConfiguration.getBaseUrl();

        }

        return baseUrl;

    }



    private org.broadinstitute.sequel.boundary.squid.SquidTopicPortype squidServicePort;


    private org.broadinstitute.sequel.boundary.squid.SquidTopicPortype squidCall() {

        if (squidServicePort == null) {
            String namespace = "urn:SquidTopic";
            QName serviceName = new QName(namespace, "SquidTopicService");

            String wsdlURL = getBaseUrl() + "/services/SquidTopicService?WSDL";

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
