package org.broadinstitute.gpinformatics.athena.boundary.products;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.run.InfiniumRunResource;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeArchetypeFixupTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Date;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.STANDARD, singleThreaded = true)
public class ProductEjbTest extends Arquillian {

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Inject
    private ProductEjb productEjb;

    @Test(enabled = true)
    public void testInitialMapping() throws Exception {
        Date effectiveDate = new Date(0);
        for (String productPartNumber : AttributeArchetypeFixupTest.INITIAL_PRODUCT_PART_TO_GENO_CHIP.keySet()) {
            Pair<String, String> chipFamilyAndName1 = productEjb.getGenotypingChip(productPartNumber, "pdo name",
                    effectiveDate);
            Pair<String, String> chipFamilyAndName2 =
                    productEjb.getGenotypingChip(productPartNumber, "The Danish Study",
                            effectiveDate);
            if (productPartNumber.contains("Danish")) {
                Assert.assertNull(chipFamilyAndName1.getRight());
                Assert.assertNull(chipFamilyAndName1.getLeft());
                Assert.assertEquals(chipFamilyAndName2.getLeft(), InfiniumRunResource.INFINIUM_GROUP);
                Assert.assertEquals(chipFamilyAndName2.getRight(),
                        AttributeArchetypeFixupTest.INITIAL_PRODUCT_PART_TO_GENO_CHIP.get(productPartNumber));
            } else {
                Assert.assertEquals(chipFamilyAndName1.getLeft(), InfiniumRunResource.INFINIUM_GROUP);
                Assert.assertEquals(chipFamilyAndName1.getRight(),
                        AttributeArchetypeFixupTest.INITIAL_PRODUCT_PART_TO_GENO_CHIP.get(productPartNumber));
                Assert.assertEquals(chipFamilyAndName2.getLeft(), chipFamilyAndName1.getLeft());
                Assert.assertEquals(chipFamilyAndName2.getRight(), chipFamilyAndName1.getRight());
            }
        }
    }


}
