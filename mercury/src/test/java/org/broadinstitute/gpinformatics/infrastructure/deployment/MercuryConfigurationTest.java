package org.broadinstitute.gpinformatics.infrastructure.deployment;


import org.testng.Assert;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraConfig;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftConfig;
import org.testng.annotations.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Map;

@Test(groups = TestGroups.DATABASE_FREE)
public class MercuryConfigurationTest {

    final String GLOBAL_YAML = "/config/test-mercury-config.yaml";

    final String LOCAL_YAML = "/config/test-mercury-config-local.yaml";


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

        final MercuryConfiguration configuration = MercuryConfiguration.getInstance();

        configuration.clear();
        configuration.load(globalDoc, localDoc);

        final ThriftConfig qaThriftConfig = (ThriftConfig) configuration.getConfig(ThriftConfig.class, Deployment.QA);
        Assert.assertEquals(qaThriftConfig.getHost(), "seqtest04");

        final ThriftConfig devThriftConfig = (ThriftConfig) configuration.getConfig(ThriftConfig.class, Deployment.DEV);

        // The Deployments passed to configReader are Mercury-centric, and both QA and DEV Mercurys point to QA thrift,
        // so these should actually be the same configs
        Assert.assertTrue(qaThriftConfig == devThriftConfig);

        // Test override of parameters

        final SquidConfig devSquidConfig = (SquidConfig) configuration.getConfig(SquidConfig.class, Deployment.DEV);

        Assert.assertEquals(devSquidConfig.getUrl(), "http://localhost:9090");

        // More parameter override, make sure we still have the non-local parameters set
        final JiraConfig testJiraConfig = (JiraConfig) configuration.getConfig(JiraConfig.class, Deployment.TEST);

        Assert.assertEquals(testJiraConfig.getPort(), 8888);
        Assert.assertEquals(testJiraConfig.getHost(), "labopsjiratest.broadinstitute.org");

        // And Deployment mappings
        final BSPConfig testBSPConfig = (BSPConfig) configuration.getConfig(BSPConfig.class, Deployment.TEST);

        Assert.assertEquals(testBSPConfig.getHost(), "bsp.broadinstitute.org");
    }
}
