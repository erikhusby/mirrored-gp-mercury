package org.broadinstitute.sequel;

import static org.broadinstitute.sequel.TestGroups.BOOT_WELD;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

public class WeldBooter extends Arquillian {

    @Deployment
    public static JavaArchive createDeployment() {
/*
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class)
                .addPackages(true, "")
                .addAsManifestResource(new File("src/test/resources/META-INF/beans.xml"))
                ;
*/
        JavaArchive archive = createBaseDeployment()
                .addPackages(true, "")
                ;
//        System.out.println(archive.toString(true));
        return archive;
    }

    public static JavaArchive createBaseDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addAsManifestResource(new File("src/test/resources/META-INF/beans.xml"));
    }

/*
    protected WeldUtil weldUtil;

    @BeforeClass
    public void bootWeld() {
         weldUtil = TestUtilities.bootANewWeld();
     }
*/
}
