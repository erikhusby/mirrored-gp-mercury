package org.broadinstitute.gpinformatics.athena.boundary.products;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.athena.entity.products.GenotypingChipMapping;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.run.InfiniumRunResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeArchetypeFixupTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
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

    @Inject
    private AttributeArchetypeFixupTest attributeArchetypeFixupTest;

    @Inject
    private AttributeArchetypeDao attributeArchetypeDao;

    @Inject
    private UserTransaction utx;

    // The product part number that has both no pdo substring, and "Danish" pdo substring.
    private String sharedPartNumber = null;
    private String danishChipName = null;
    private String nonDanishChipName = null;

    @BeforeTest
    public void init() {
        for (String key : AttributeArchetypeFixupTest.INITIAL_PRODUCT_PART_TO_GENO_CHIP.keySet()) {
            if (key.contains("Danish")) {
                sharedPartNumber = key.split(GenotypingChipMapping.DELIMITER)[0];
                danishChipName = AttributeArchetypeFixupTest.INITIAL_PRODUCT_PART_TO_GENO_CHIP.get(key);
            }
        }
        // Gets the chip name mapped to the danish part number when pdo name does not contain "Danish".
        nonDanishChipName = AttributeArchetypeFixupTest.INITIAL_PRODUCT_PART_TO_GENO_CHIP.get(sharedPartNumber);
    }

    @Test(enabled = true)
    public void testPdoNameMatching() throws Exception {
        Assert.assertNotNull(sharedPartNumber);
        Assert.assertNotNull(danishChipName);
        Assert.assertNotNull(nonDanishChipName);

        for (String key : AttributeArchetypeFixupTest.INITIAL_PRODUCT_PART_TO_GENO_CHIP.keySet()) {
            String productPartNumber = key.split(GenotypingChipMapping.DELIMITER)[0];

            Pair<String, String> chipFamilyAndName1 = productEjb.getGenotypingChip(productPartNumber,
                    "pdo name", AttributeArchetypeFixupTest.EARLIEST_XSTAIN_DATE);
            Pair<String, String> chipFamilyAndName2 = productEjb.getGenotypingChip(productPartNumber,
                    "The Danish Study", AttributeArchetypeFixupTest.EARLIEST_XSTAIN_DATE);

            Assert.assertEquals(chipFamilyAndName1.getLeft(), InfiniumRunResource.INFINIUM_GROUP);
            Assert.assertEquals(chipFamilyAndName2.getLeft(), InfiniumRunResource.INFINIUM_GROUP);

            if (productPartNumber.equals(sharedPartNumber)) {
                Assert.assertEquals(chipFamilyAndName1.getRight(), nonDanishChipName);
                Assert.assertEquals(chipFamilyAndName2.getRight(), danishChipName);
            } else {
                Assert.assertEquals(chipFamilyAndName1.getRight(),
                        AttributeArchetypeFixupTest.INITIAL_PRODUCT_PART_TO_GENO_CHIP.get(key));
                Assert.assertEquals(chipFamilyAndName2.getRight(), chipFamilyAndName1.getRight());
            }
        }
    }

    @Test(enabled = true)
    public void testPdoNameMatchingOrder() throws Exception {
        utx.begin();

        // Adds a new mapping for the shared part number but with an extended substring.
        String chipName2 = "chip" + System.currentTimeMillis();
        String mappingName = sharedPartNumber + GenotypingChipMapping.DELIMITER + "Danish2";
        GenotypingChipMapping danish2Mapping = new GenotypingChipMapping(mappingName,
                InfiniumRunResource.INFINIUM_GROUP, chipName2, AttributeArchetypeFixupTest.EARLIEST_XSTAIN_DATE);
        attributeArchetypeDao.persist(danish2Mapping);
        attributeArchetypeDao.flush();

        // Should now have three mappings for the part number, depending on pdo name.
        Assert.assertEquals(productEjb.getGenotypingChip(sharedPartNumber, "any name",
                AttributeArchetypeFixupTest.EARLIEST_XSTAIN_DATE).getRight(), nonDanishChipName);
        Assert.assertEquals(productEjb.getGenotypingChip(sharedPartNumber, "Danish",
                AttributeArchetypeFixupTest.EARLIEST_XSTAIN_DATE).getRight(), danishChipName);
        Assert.assertEquals(productEjb.getGenotypingChip(sharedPartNumber, "Danish2",
                AttributeArchetypeFixupTest.EARLIEST_XSTAIN_DATE).getRight(), chipName2);

        utx.rollback();
    }

    @Test(enabled = true)
    public void testDateLookup() throws Exception {
        utx.begin();

        long now = System.currentTimeMillis();
        String partNumber = "ABCD" + now;
        String family = "tech" + now;
        String[] chipNames = {"name0" + now, "name1" + now, "name2" + now};

        // Makes a sequence of three different chip mappings for the same product, each active at different times.
        Date[] testDates = {new Date(now - 600000), new Date(now - 400000), new Date(now - 200000), new Date(now)};
        Date[] chipDates = {new Date(now - 500000), new Date(now - 300000), new Date(now - 100000)};

        GenotypingChipMapping[] mappings = {
                new GenotypingChipMapping(partNumber, family, chipNames[0], chipDates[0]),
                new GenotypingChipMapping(partNumber, family, chipNames[1], chipDates[1]),
                new GenotypingChipMapping(partNumber, family, chipNames[2], chipDates[2])
        };
        // Inactivates the mappings so they don't overlap. Last mapping remains active.
        mappings[0].setInactiveDate(chipDates[1]);
        mappings[1].setInactiveDate(chipDates[2]);

        for (int i = 0; i < mappings.length; ++i) {
            attributeArchetypeDao.persist(mappings[i]);
        }
        attributeArchetypeDao.flush();

        // Get all mappings should return the new ones.
        boolean[] found = {false, false, false};
        for (GenotypingChipMapping mapping : attributeArchetypeDao.findGenotypingChipMappings()) {
            for (int i = 0; i < chipNames.length; ++i) {
                if (mapping.getChipName().equals(chipNames[i])) {
                    found[i] = true;
                }
            }
        }
        Assert.assertTrue(found[0]);
        Assert.assertTrue(found[1]);
        Assert.assertTrue(found[2]);

        // Tests mapping right on the active/inactive date.
        for (int i = 0; i < chipNames.length; ++i) {
            Assert.assertEquals(productEjb.getGenotypingChip(partNumber, "pdo name", chipDates[i]).getRight(),
                    chipNames[i], "At index " + i);
        }

        // Tests which chip gets mapped at each test date.
        for (int i = 0; i < testDates.length; ++i) {
            Pair<String, String> chipFamilyAndName = productEjb.getGenotypingChip(partNumber, "pdo name", testDates[i]);
            if (i == 0) {
                Assert.assertNull(chipFamilyAndName.getLeft());
                Assert.assertNull(chipFamilyAndName.getRight());
            } else {
                Assert.assertEquals(chipFamilyAndName.getLeft(), family);
                Assert.assertEquals(chipFamilyAndName.getRight(), chipNames[i - 1]);
            }
        }

        // Tests the current mapping for the test product.
        String currentChip = null;
        for (Triple<String, String, String> familyAndNameAndSubstring :
                productEjb.getCurrentMappedGenotypingChips(partNumber)) {
            if (familyAndNameAndSubstring.getLeft().equals(family)) {
                Assert.assertNull(currentChip);
                currentChip = familyAndNameAndSubstring.getMiddle();
                Assert.assertNull(familyAndNameAndSubstring.getRight());
            }
        }
        Assert.assertEquals(currentChip, chipNames[2]);

        utx.rollback();
    }

    @Test(enabled = true)
    public void testDateOverlaps() throws Exception {
        utx.begin();

        long now = System.currentTimeMillis();
        String partNumber = "ABCD" + now;
        String family = "tech" + now;
        String[] chipNames = {"name0" + now, "name1" + now};
        Date chipDate = new Date(now - 500000);

        GenotypingChipMapping[] mappings = {
                new GenotypingChipMapping(partNumber, family, chipNames[0], chipDate),
                new GenotypingChipMapping(partNumber, family, chipNames[1], chipDate)
        };
        for (int i = 0; i < mappings.length; ++i) {
            attributeArchetypeDao.persist(mappings[i]);
        }
        attributeArchetypeDao.flush();

        // Detects the overlap since both chips are active.
        try {
            productEjb.getGenotypingChip(partNumber, "", chipDate);
            Assert.fail("Did not find overlapping active mappings.");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getMessage().toLowerCase().contains("multiple genotyping chip mappings"));
        }
    }
}
