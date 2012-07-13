package org.broadinstitute.sequel.infrastructure.deployment;


import junit.framework.Assert;
import org.broadinstitute.sequel.infrastructure.bsp.BSPConfig;
import org.broadinstitute.sequel.infrastructure.squid.SquidConfig;
import org.broadinstitute.sequel.infrastructure.thrift.ThriftConfig;
import org.testng.annotations.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Map;

@Test
public class DeploymentTest  {

    final String GLOBAL_YAML = "/config/test-sequel-config.yaml";

    final String LOCAL_YAML = "/config/test-sequel-config-local.yaml";



    public void testYaml() throws FileNotFoundException {

        URL url;
        FileInputStream is;
        final Yaml yaml = new Yaml();

        url = getClass().getResource(GLOBAL_YAML);
        is = new FileInputStream(url.getFile());


        final Map<String, Map> globalDoc = (Map<String, Map>) yaml.load(is);


        url = getClass().getResource(LOCAL_YAML);
        is = new FileInputStream(url.getFile());

        final Map<String, Map> localDoc = (Map<String, Map>) yaml.load(is);




        final SequelConfigReader configReader = SequelConfigReader.getInstance();

        configReader.clear();
        configReader.load(globalDoc, localDoc);


        final ThriftConfig qaThriftConfig = (ThriftConfig) configReader.getConfig(ThriftConfig.class, Deployment.QA);
        Assert.assertEquals(qaThriftConfig.getHost(), "seqtest04");


        final ThriftConfig devThriftConfig = (ThriftConfig) configReader.getConfig(ThriftConfig.class, Deployment.DEV);


        // The Deployments passed to configReader are SequeL-centric, and both QA and DEV SequeLs point to QA thrift,
        // so these should actually be the same configs
        Assert.assertTrue(qaThriftConfig == devThriftConfig);


        // Test override of parameters

        final SquidConfig devSquidConfig = (SquidConfig) configReader.getConfig(SquidConfig.class, Deployment.DEV);

        Assert.assertEquals(devSquidConfig.getUrl(), "http://localhost:9090");


        // And Deployment mappings

        final BSPConfig testBSPConfig = (BSPConfig) configReader.getConfig(BSPConfig.class, Deployment.TEST);

        Assert.assertEquals(testBSPConfig.getHost(), "bsp.broadinstitute.org");




    }
}
