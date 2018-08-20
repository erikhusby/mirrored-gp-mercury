package org.broadinstitute.gpinformatics.infrastructure.common;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link ServiceAccessUtility}. Note that this top-level class is a container for the actual @Test-annotated
 * test classes and some supporting classes. The test scenarios require different deployment configurations, so separate
 * Arquillian test classes are needed. Making them inner classes of the same top-level class helps to keep these related
 * tests together.
 */
@SuppressWarnings("unused")
@Dependent
public class ServiceAccessUtilityTest {

    public ServiceAccessUtilityTest(){}

    /**
     * A simple interface to act as a bean type for injection.
     */
    public interface AnInterface {
        String getName();
    }

    /**
     * The primary implementation of AnInterface.
     */
    @Default
    @Dependent
    public static class AnImplementation implements AnInterface {
        public AnImplementation(){}
        @Override
        public String getName() {
            return AnImplementation.class.getName();
        }
    }

    /**
     * An alternative implementation of AnInterface.
     */
    @Alternative
    @Dependent
    public static class AlternativeImplementation implements AnInterface {
        public AlternativeImplementation(){}
        @Override
        public String getName() {
            return AlternativeImplementation.class.getName();
        }
    }

    /**
     * A simple class to act as a bean type for injection (without an interface).
     */
    @Dependent
    public static class AClass {
        public AClass(){}
        String getName() {
            return AClass.class.getName();
        }
    }

    /**
     * A subclass of AClass acting as an alternative implementation.
     */
    @Alternative
    @Dependent
    public static class ASubclass extends AClass {
        public ASubclass(){}
        @Override
        String getName() {
            return ASubclass.class.getName();
        }
    }

    /**
     * Tests for {@link ServiceAccessUtility#getBean(Class)} where there are alternative implementations that have NOT
     * been activated in beans.xml.
     */
    @Test(groups = TestGroups.ALTERNATIVES)
    @Dependent
    public static class InactiveAlternativeImplementation extends Arquillian {

        public InactiveAlternativeImplementation(){}

        @Deployment
        public static Archive createDeployment() {
            return ServiceAccessUtilityTest.createDeployment(EmptyAsset.INSTANCE);
        }

        public void testInactiveAlternativeImplementation() {
            AnInterface bean = ServiceAccessUtility.getBean(AnInterface.class);
            Assert.assertEquals(bean.getName(), AnImplementation.class.getName());
        }

        public void testInactiveAlternativeSubclass() {
            AClass bean = ServiceAccessUtility.getBean(AClass.class);
            Assert.assertEquals(bean.getName(), AClass.class.getName());
        }
    }

    /**
     * Tests for {@link ServiceAccessUtility#getBean(Class)} where there are active alternative implementations. Note
     * that these tests would sometimes pass with a previous implementation of getBean() due to the use of
     * iterator().next() on a Set returned from a BeanManager call.
     */
    @Test(groups = TestGroups.ALTERNATIVES)
    @Dependent
    public static class ActiveAlternativeImplementation extends Arquillian {

        public ActiveAlternativeImplementation(){}

        @Deployment
        public static Archive createDeployment() {
            return ServiceAccessUtilityTest.createDeployment(
                    new StringAsset(DeploymentBuilder.buildBeansXml(AlternativeImplementation.class, ASubclass.class)));
        }

        public void testActiveAlternativeImplementation() {
            AnInterface bean = ServiceAccessUtility.getBean(AnInterface.class);
            Assert.assertEquals(bean.getName(), AlternativeImplementation.class.getName());
        }

        public void testActiveAlternativeSubclass() {
            AClass bean = ServiceAccessUtility.getBean(AClass.class);
            Assert.assertEquals(bean.getName(), ASubclass.class.getName());
        }
    }

    private static Archive createDeployment(Asset beansXmlAsset) {
        WebArchive war = ShrinkWrap.create(WebArchive.class)
                .addClass(ServiceAccessUtility.class)
                .addClass(ServiceAccessUtilityTest.class)
                .addClass(ServiceAccessUtilityTest.AClass.class)
                .addClass(ServiceAccessUtilityTest.ActiveAlternativeImplementation.class)
                .addClass(ServiceAccessUtilityTest.AlternativeImplementation.class)
                .addClass(ServiceAccessUtilityTest.AnImplementation.class)
                .addClass(ServiceAccessUtilityTest.AnInterface.class)
                .addClass(ServiceAccessUtilityTest.ASubclass.class)
                .addClass(ServiceAccessUtilityTest.InactiveAlternativeImplementation.class)
                .addAsWebInfResource(beansXmlAsset, "beans.xml");


        // An arquillian test needs to import Maven runtime dependencies for arquillian into the deployment
        List<File> artifacts = new ArrayList<>();

        for (MavenResolvedArtifact artifact : Maven.resolver().loadPomFromFile("pom.xml")
                .importDependencies( ScopeType.TEST )
                .resolve().withTransitivity().asResolvedArtifact()) {
            // This is some old stuff I had to pull up to use new API and be consistent
            // TODO: remove all test-scoped dependencies; optionally explicitly add certain test dependencies that we commit to supporting
            // TODO: remove exclusion of xerces, which is a workaround until all test-scoped dependencies are removed
            // TODO: remove exclusion of dom4j, WildFly problem with an older release in it's runtime classpath
            if( artifact.getExtension().equals("jar")
                    && !artifact.getCoordinate().getArtifactId().contains("xerces")
                    // Pulled in with another dependency
                    && !artifact.getCoordinate().getArtifactId().contains("dom4j") ) {
                artifacts.add(artifact.asFile());
            }
        }

        return war.addAsLibraries(artifacts.toArray(new File[0] ));
    }
}
