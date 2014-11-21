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
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.inject.Alternative;

@SuppressWarnings("unused")
public class ServiceAccessUtilityTest {

    public interface AnInterface {
        String getName();
    }

    public static class AnImplementation implements AnInterface {
        @Override
        public String getName() {
            return AnImplementation.class.getName();
        }
    }

    @Alternative
    public static class AlternativeImplementation implements AnInterface {
        @Override
        public String getName() {
            return AlternativeImplementation.class.getName();
        }
    }

    public static class AClass {
        String getName() {
            return AClass.class.getName();
        }
    }

    @Alternative
    public static class ASubclass extends AClass {
        @Override
        String getName() {
            return ASubclass.class.getName();
        }
    }

    @Test(groups = TestGroups.ALTERNATIVES)
    public static class InactiveAlternativeImplementation extends Arquillian {
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

    @Test(groups = TestGroups.ALTERNATIVES)
    public static class ActiveAlternativeImplementation extends Arquillian {
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
        return ShrinkWrap.create(WebArchive.class)
                .addClass(ServiceAccessUtility.class)
                .addClass(ServiceAccessUtilityTest.class)
                .addAsWebInfResource(beansXmlAsset, "beans.xml");
    }
}
