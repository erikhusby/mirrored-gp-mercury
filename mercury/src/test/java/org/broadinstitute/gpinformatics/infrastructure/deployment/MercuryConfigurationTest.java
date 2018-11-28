package org.broadinstitute.gpinformatics.infrastructure.deployment;


import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraConfig;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftConfig;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

@Test(groups = TestGroups.DATABASE_FREE)
public class MercuryConfigurationTest {

    static final String GLOBAL_YAML = "/config/test-mercury-config.yaml";

    static final String LOCAL_YAML = "/config/test-mercury-config-local.yaml";


    public void testYaml() throws FileNotFoundException {

        URL url;
        FileInputStream is;
        Yaml yaml = new Yaml();

        url = getClass().getResource(GLOBAL_YAML);
        is = new FileInputStream(url.getFile());

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Map<String, String>>> globalDoc =
                (Map<String, Map<String, Map<String, String>>>) yaml.load(is);

        url = getClass().getResource(LOCAL_YAML);
        is = new FileInputStream(url.getFile());

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Map<String, String>>> localDoc =
                (Map<String, Map<String, Map<String, String>>>) yaml.load(is);

        MercuryConfiguration configuration = MercuryConfiguration.getInstance();

        configuration.clear();
        configuration.load(globalDoc, localDoc);

        ThriftConfig qaThriftConfig = (ThriftConfig) configuration.getConfig(ThriftConfig.class, Deployment.QA);
        Assert.assertEquals(qaThriftConfig.getHost(), "vsquidthriftserviceqa");

        ThriftConfig devThriftConfig = (ThriftConfig) configuration.getConfig(ThriftConfig.class, Deployment.DEV);

        // The Deployments passed to configReader are Mercury-centric, and both QA and DEV Mercurys point to QA thrift,
        // so these should actually be the same configs
        Assert.assertEquals(qaThriftConfig, devThriftConfig);

        // Test override of parameters

        SquidConfig devSquidConfig = (SquidConfig) configuration.getConfig(SquidConfig.class, Deployment.DEV);

        Assert.assertEquals(devSquidConfig.getUrl(), "http://localhost:9090");

        // More parameter override, make sure we still have the non-local parameters set
        JiraConfig testJiraConfig = (JiraConfig) configuration.getConfig(JiraConfig.class, Deployment.TEST);

        Assert.assertEquals(testJiraConfig.getPort(), 8888);
        Assert.assertEquals(testJiraConfig.getHost(), "labopsjiratest.broadinstitute.org");

        // And Deployment mappings
        BSPConfig testBSPConfig = (BSPConfig) configuration.getConfig(BSPConfig.class, Deployment.TEST);

        Assert.assertEquals(testBSPConfig.getHost(), "bsp.broadinstitute.org");

        // test Sets
        AppConfig appConfig = (AppConfig) configuration.getConfig(AppConfig.class, Deployment.TEST);
        Set<String> gpBillingManagers = appConfig.getGpBillingManagers();
        Assert.assertFalse(gpBillingManagers.isEmpty());
        Assert.assertEquals(gpBillingManagers.size(), 2);

    }
}
