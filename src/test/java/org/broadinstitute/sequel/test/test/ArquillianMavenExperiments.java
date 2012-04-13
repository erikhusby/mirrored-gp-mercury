package org.broadinstitute.sequel.test.test;

import org.broadinstitute.sequel.test.DeploymentBuilder;
import org.broadinstitute.sequel.test.beans.SimpleService;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
//import org.jboss.shrinkwrap.resolver.api.maven.MavenImporter;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;

/**
 * @author breilly
 */
public class ArquillianMavenExperiments extends Arquillian {

    private static final String BEANS_XML =
            "<beans>\n" +
            "    <alternatives>\n" +
            "        <class>org.broadinstitute.sequel.DummyContainerTestService</class>\n" +
            "    </alternatives>\n" +
            "</beans>";

    @Inject
    private SimpleService service;

//    @Deployment
    public static Archive makeArchive() {
        Archive archive = DeploymentBuilder.buildSequelWar(BEANS_XML);
        System.out.println(archive.toString(true));
        return archive;
    }

    @Deployment
    public static JavaArchive createJarOverrideNoMaven() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class)
                .addPackages(true, "")
                .as(ExplodedImporter.class).importDirectory("src/main/resources").as(JavaArchive.class)
//                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
//                .add(new StringAsset(BEANS_XML), "beans.xml")
//                .addAsManifestResource(new StringAsset(BEANS_XML), "beans.xml")
//                .add(new StringAsset(BEANS_XML), "WEB-INF/beans.xml")
                .add(EmptyAsset.INSTANCE, "addedAsset.asset")
                .add(EmptyAsset.INSTANCE, "WEB-INF/web-inf.asset")
                .addAsResource(EmptyAsset.INSTANCE, "asResource.asset")
                .addAsManifestResource(EmptyAsset.INSTANCE, "asManifestResource.asset")
        ;
        System.out.println(archive.toString(true));
        return archive;
    }

//    @Deployment
    public static JavaArchive createJarOverrideBeforeMaven() {
        JavaArchive archive = addMavenBuildOutput(ShrinkWrap.create(JavaArchive.class)
//                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
//                .add(new StringAsset(BEANS_XML), "beans.xml")
//                .addAsManifestResource(new StringAsset(BEANS_XML), "beans.xml")
//                .add(new StringAsset(BEANS_XML), "WEB-INF/beans.xml")
                .add(EmptyAsset.INSTANCE, "addedAsset.asset")
                .add(EmptyAsset.INSTANCE, "WEB-INF/web-inf.asset")
                .addAsResource(EmptyAsset.INSTANCE, "asResource.asset")
                .addAsManifestResource(EmptyAsset.INSTANCE, "asManifestResource.asset")
        );
//        System.out.println(archive.toString(true));
        return archive;
    }

//    @Deployment
    public static JavaArchive createJarOverrideAfterMaven() {
        JavaArchive archive = addMavenBuildOutput(ShrinkWrap.create(JavaArchive.class))
//                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
//                .add(new StringAsset(BEANS_XML), "beans.xml")
//                .addAsManifestResource(new StringAsset(BEANS_XML), "beans.xml")
//                .add(new StringAsset(BEANS_XML), "WEB-INF/beans.xml")
                .add(EmptyAsset.INSTANCE, "addedAsset.asset")
                .add(EmptyAsset.INSTANCE, "WEB-INF/web-inf.asset")
                .addAsResource(EmptyAsset.INSTANCE, "asResource.asset")
                .addAsManifestResource(EmptyAsset.INSTANCE, "asManifestResource.asset")
        ;
//        System.out.println(archive.toString(true));
        return archive;
    }

//    @Deployment
    public static WebArchive createWarOverrideNoMaven() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class)
                .addPackages(true, "")
//                .add(new StringAsset(BEANS_XML), "beans.xml")
//                .addAsManifestResource(new StringAsset(BEANS_XML), "beans.xml")
//                .addAsWebInfResource(new StringAsset(BEANS_XML), "beans.xml")
                .add(EmptyAsset.INSTANCE, "addedAsset.asset")
                .add(EmptyAsset.INSTANCE, "WEB-INF/web-inf.asset")
                .addAsResource(EmptyAsset.INSTANCE, "asResource.asset")
                .addAsManifestResource(EmptyAsset.INSTANCE, "asManifestResource.asset")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "asWebInfResource.asset")
        ;
//        System.out.println(archive.toString(true));
        return archive;
    }

//    @Deployment
    public static WebArchive createWarOverrideBeforeMaven() {
        WebArchive archive = addMavenBuildOutput(ShrinkWrap.create(WebArchive.class)
//                .add(new StringAsset(BEANS_XML), "beans.xml")
//                .addAsManifestResource(new StringAsset(BEANS_XML), "beans.xml")
//                .addAsWebInfResource(new StringAsset(BEANS_XML), "beans.xml")
                .add(EmptyAsset.INSTANCE, "addedAsset.asset")
                .add(EmptyAsset.INSTANCE, "WEB-INF/web-inf.asset")
                .addAsResource(EmptyAsset.INSTANCE, "asResource.asset")
                .addAsManifestResource(EmptyAsset.INSTANCE, "asManifestResource.asset")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "asWebInfResource.asset")
        );
//        System.out.println(archive.toString(true));
        return archive;
    }

//    @Deployment
    public static WebArchive createWarOverrideAfterMaven() {
        WebArchive archive = addMavenBuildOutput(ShrinkWrap.create(WebArchive.class))
//                .add(new StringAsset(BEANS_XML), "beans.xml")
//                .addAsManifestResource(new StringAsset(BEANS_XML), "beans.xml")
//                .addAsResource(new StringAsset(BEANS_XML), "asResource.asset")
//                .addAsWebInfResource(new StringAsset(BEANS_XML), "beans.xml")
                .add(EmptyAsset.INSTANCE, "addedAsset.asset")
                .add(EmptyAsset.INSTANCE, "WEB-INF/web-inf.asset")
                .addAsResource(EmptyAsset.INSTANCE, "asResource.asset")
                .addAsManifestResource(EmptyAsset.INSTANCE, "asManifestResource.asset")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "asWebInfResource.asset")
        ;
//        System.out.println(archive.toString(true));
        return archive;
    }

    public static JavaArchive addMavenBuildOutput(JavaArchive archive) {
/* commented out in order to remove dependency from maven
        return archive.as(MavenImporter.class)
                .loadEffectivePom("pom.xml").importBuildOutput()
                .as(JavaArchive.class);
*/
        return archive;
    }

    public static WebArchive addMavenBuildOutput(WebArchive archive) {
/* commented out in order to remove dependency from maven
        return archive.as(MavenImporter.class)
                .loadEffectivePom("pom.xml").importBuildOutput()
                .as(WebArchive.class);
*/
        return archive;
    }

//    @Test
    public void testContainer() {
        Assert.fail(service.getName());
    }
}
