package org.broadinstitute.sequel.infrastructure.squid;


import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.infrastructure.deployment.*;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.*;

public class SquidConfigurationProducer implements BaseConfigurationProducer<SquidConfiguration> {


    @Inject
    private Log log;


    @Inject
    private Deployment deployment;


    private Map<Deployment, SquidConfiguration> configurationMap;



    public SquidConfigurationProducer() {

        SquidConfiguration [] configs = new SquidConfiguration[] {

                new SquidConfiguration(
                        DEV,
                        "http://localhost:8080/squid"
                ),

                new SquidConfiguration(
                        TEST,
                        "http://prodinfobuild.broadinstitute.org:8020/squid"
                ),

                new SquidConfiguration(
                        QA,
                        "http://vsquidrc.broadinstitute.org:8000/squid"
                ),

                new SquidConfiguration(
                        PROD,
                        "http://squid-ui.broadinstitute.org:8000/squid"
                ),

                new SquidConfiguration(
                        STUBBY,
                        null
                )

        };

        configurationMap = new HashMap<Deployment, SquidConfiguration>();


        for (SquidConfiguration config : configs)
            configurationMap.put(config.getDeployment(), config);


    }



    @Produces
    @DevInstance
    public SquidConfiguration devInstance() {
        log.info("explicitly asked for DEV instance");
        return configurationMap.get(DEV);
    }



    @Produces
    @TestInstance
    public SquidConfiguration testInstance() {
        log.info("explicitly asked for TEST instance");
        return configurationMap.get(TEST);
    }




    @Produces
    @QAInstance
    public SquidConfiguration qaInstance() {
        log.info("explicitly asked for QA instance");
        return configurationMap.get(QA);
    }



    @Produces
    @ProdInstance
    public SquidConfiguration prodInstance() {
        log.info("explicitly asked for PROD instance");
        return configurationMap.get(PROD);
    }



    @Produces
    @StubInstance
    public SquidConfiguration stubInstance() {
        throw new RuntimeException("explicitly asked for STUBBY instance of SquidConfiguration, should not happen!");
    }



    @Produces
    public SquidConfiguration produce() {
        log.info("Returning SquidConfiguration for deployment " + deployment);
        return configurationMap.get(deployment);

    }


    /**
     * The use case for this method is the SOAP service impl where direct @Injection of a @Singleton @Startup
     * SquidConfiguration currently does not work.
     * See {@link org.broadinstitute.sequel.boundary.pass.PassSOAPServiceImpl}.
     *
     * @param deployment
     *
     * @return
     */
    public static SquidConfiguration produce(Deployment deployment) {
        return new SquidConfigurationProducer().configurationMap.get(deployment);
    }


}
