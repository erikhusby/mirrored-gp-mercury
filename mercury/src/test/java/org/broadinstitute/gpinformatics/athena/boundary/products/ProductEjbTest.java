package org.broadinstitute.gpinformatics.athena.boundary.products;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.entity.products.GenotypingChipMapping;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
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
        Date pastDate = DateUtils.parseDate("1/1/2015");
        Date effectiveDate = new Date();

        String danishPartNumber = null;
        String danishChipName = null;
        for (String key : AttributeArchetypeFixupTest.INITIAL_PRODUCT_PART_TO_GENO_CHIP.keySet()) {
            if (key.contains("Danish")) {
                danishPartNumber = key.split(GenotypingChipMapping.DELIMITER)[0];
                danishChipName = AttributeArchetypeFixupTest.INITIAL_PRODUCT_PART_TO_GENO_CHIP.get(key);
            }
        }
        // Gets the expected chip name for the part number that does not have "Danish" substring match.
        String nonDanishChipName = AttributeArchetypeFixupTest.INITIAL_PRODUCT_PART_TO_GENO_CHIP.
                get(danishPartNumber.split(GenotypingChipMapping.DELIMITER)[0]);

        for (String key : AttributeArchetypeFixupTest.INITIAL_PRODUCT_PART_TO_GENO_CHIP.keySet()) {
            String productPartNumber = key.split(GenotypingChipMapping.DELIMITER)[0];

            Pair<String, String> chipFamilyAndName1 = productEjb.getGenotypingChip(productPartNumber,
                    "pdo name", effectiveDate);
            Pair<String, String> chipFamilyAndName2 = productEjb.getGenotypingChip(productPartNumber,
                    "The Danish Study", effectiveDate);
            Assert.assertNull(productEjb.getGenotypingChip(productPartNumber, "any pdo", pastDate).getLeft());

            Assert.assertEquals(chipFamilyAndName1.getLeft(), InfiniumRunResource.INFINIUM_GROUP);
            Assert.assertEquals(chipFamilyAndName2.getLeft(), InfiniumRunResource.INFINIUM_GROUP);

            if (productPartNumber.equals(danishPartNumber)) {
                Assert.assertEquals(chipFamilyAndName1.getRight(), nonDanishChipName);
                Assert.assertEquals(chipFamilyAndName2.getRight(), danishChipName);
            } else {
                Assert.assertEquals(chipFamilyAndName1.getRight(),
                        AttributeArchetypeFixupTest.INITIAL_PRODUCT_PART_TO_GENO_CHIP.get(key));
                Assert.assertEquals(chipFamilyAndName2.getRight(), chipFamilyAndName1.getRight());
            }
        }
    }


}
