package org.broadinstitute.gpinformatics.infrastructure.test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import java.io.File;

@Deprecated
public class WeldBooter extends Arquillian {

    @Deployment
    public static JavaArchive createDeployment() {
        return createBaseDeployment().addPackages(true, "");
    }

    public static JavaArchive createBaseDeployment() {
        return ShrinkWrap.create(JavaArchive.class).addAsManifestResource(new File("src/test/resources/META-INF/beans.xml"));
    }
}
