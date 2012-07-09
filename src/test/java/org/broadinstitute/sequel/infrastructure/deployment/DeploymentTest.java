package org.broadinstitute.sequel.infrastructure.deployment;


import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;

@Test
public class DeploymentTest  {

    final String SEQUEL_CONFIG_YAML = "sequel-config.yaml";



    public void testYaml() throws FileNotFoundException {
//        InputStream is = getClass().getResourceAsStream(SEQUEL_CONFIG_YAML);

        URL url = getClass().getResource("/" + SEQUEL_CONFIG_YAML);
        FileInputStream is = new FileInputStream(url.getFile());

        SequelConfigReader.getInstance().loadGlobal(is);

        System.out.println("FOO");

    }
}
