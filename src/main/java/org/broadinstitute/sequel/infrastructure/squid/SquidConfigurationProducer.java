package org.broadinstitute.sequel.infrastructure.squid;


import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.infrastructure.deployment.*;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.*;

public class SquidConfigurationProducer implements BaseConfigurationProducer<SquidConfiguration> {


    @Inject
    private Log log;


    @Inject
    private Deployment deployment;

    /**
     * ConfigurationHolder tries to take some of the pain out of the restriction that managed beans cannot be
     * parameterized types, but we still end up with a slew of non-DRY @Produces methods that delegate to the
     * ConfigurationHolder.  I believe there is currently no way around this, the @Produces methods need to be on
     * the implementing class.
     */
    private ConfigurationHolder<SquidConfiguration> configurationHolder
            = new ConfigurationHolder<SquidConfiguration>();



    private void add(Deployment deployment, String url) {
        configurationHolder.add(deployment, new SquidConfiguration(url));
    }


    public SquidConfigurationProducer() {

        Deployment deployment;
        String url;


        // How do we allow for local overrides of DEV parameters?  or a la carte stubs?
        deployment = DEV;
        url        = "http://localhost:8080/squid";

        add(deployment, url);



        deployment = TEST;
        url        = "http://prodinfobuild.broadinstitute.org:8020/squid";

        add(deployment, url);



        deployment = QA;
        url        = "http://vsquidrc.broadinstitute.org:8000/squid";

        add(deployment, url);



        deployment = PROD;
        url        = "http://squid-ui.broadinstitute.org:8000/squid";

        add(deployment, url);



        configurationHolder.add(STUBBY, new SquidConfiguration(null) {
            @Override
            // base URL should never be consulted in a STUBBY deployment, this is a sign of something seriously wrong
            public String getBaseUrl() {
                throw new RuntimeException("Interrogating base URL for STUBBY configuration!");
            }
        });


    }



    @Produces
    @DevInstance
    public SquidConfiguration devInstance() {
        return configurationHolder.get(DEV);
    }



    @Produces
    @TestInstance
    public SquidConfiguration testInstance() {
        return configurationHolder.get(TEST);
    }




    @Produces
    @QAInstance
    public SquidConfiguration qaInstance() {
        return configurationHolder.get(QA);
    }



    @Produces
    @ProdInstance
    public SquidConfiguration prodInstance() {
        return configurationHolder.get(PROD);
    }



    @Produces
    @StubInstance
    public SquidConfiguration stubInstance() {
        return configurationHolder.get(STUBBY);
    }



    @Produces
    public SquidConfiguration produce() {
        return configurationHolder.get(deployment);

    }


    /**
     * The use case for this method is the SOAP service impl where CDI currently does not work.
     * See {@link org.broadinstitute.sequel.boundary.pass.PassSOAPServiceImpl}.
     *
     * @param deployment
     *
     * @return
     */
    public static SquidConfiguration produce(Deployment deployment) {
        return new SquidConfigurationProducer().configurationHolder.get(deployment);
    }


}
