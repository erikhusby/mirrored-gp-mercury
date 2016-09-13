package org.broadinstitute.gpinformatics.athena.boundary.products;

import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.GenotypingChipMapping;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeArchetypeFixupTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
    private ProductDao productDao;

    @Inject
    private UserTransaction utx;

    @Test(enabled = true)
    public void testPdoNameMatching() throws Exception {
        utx.begin();
        try {
            long now = System.currentTimeMillis();
            String partNumber = "ABCD" + now;
            String family = "tech" + now;
            String[] chipNames = {"chip0" + now, "chip1" + now, "chip2" + now};
            String[] pdoSubstrings = {"0" + now, "named0" + now, "PDO named1" + now};

            // Mappings based on different pdo substrings.
            List<Triple<String, String, String>> genoChipInfos = new ArrayList<>();
            for (int i = 0; i < chipNames.length; ++i) {
                genoChipInfos.add(Triple.of(family, chipNames[i], pdoSubstrings[i]));
            }
            productEjb.persistGenotypingChipMappings(partNumber, genoChipInfos);
            attributeArchetypeDao.flush();

            Date date0 = new Date();
            Assert.assertEquals(productEjb.getGenotypingChip(partNumber,
                    "A pdo named " + pdoSubstrings[0], date0).getRight(), chipNames[0]);
            // This pdo name contains both pdoSubstrings[0] and pdoSubstrings[1], but since the
            // latter is more complex (longer) it should be the one that is selected.
            Assert.assertEquals(productEjb.getGenotypingChip(partNumber,
                    "Another pdo named " + pdoSubstrings[1], date0).getRight(), chipNames[1]);
            Assert.assertEquals(productEjb.getGenotypingChip(partNumber,
                    "A pdo named " + pdoSubstrings[2], date0).getRight(), chipNames[2]);

        } finally {
            utx.rollback();
        }
    }

    @Test(enabled = true)
    public void testDateOverlaps() throws Exception {
        utx.begin();
        try {
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

            boolean foundMapping0 = false;
            boolean foundMapping1 = false;
            for (GenotypingChipMapping mapping : attributeArchetypeDao.findGenotypingChipMappings()) {
                if (mapping.getChipName().equals(chipNames[0])) {
                    foundMapping0 = true;
                }
                if (mapping.getChipName().equals(chipNames[1])) {
                    foundMapping1 = true;
                }
            }
            Assert.assertTrue(foundMapping0 && foundMapping1);

            // Detects the overlap since both mappings are active.
            try {
                productEjb.getGenotypingChip(partNumber, "", chipDate);
                Assert.fail("Did not find overlapping active mappings.");
            } catch (RuntimeException e) {
                Assert.assertTrue(e.getMessage().toLowerCase().contains("multiple genotyping chip mappings"));
            }
        } finally {
            utx.rollback();
        }
    }

    @Test(enabled = true)
    public void testMappingUpdate() throws Exception {
        utx.begin();
        try {
            long now = System.currentTimeMillis();
            String partNumber = "ABCD" + now;
            String[] chipNames = {"name0" + now, "name1" + now};
            Date[] dates = new Date[chipNames.length + 1];

            // Repeats the final iteration just to check that gratuitous updates get suppressed.
            for (int i = 0; i < chipNames.length + 1; ++i) {
                int idx = Math.min(i, chipNames.length - 1);
                productEjb.persistGenotypingChipMappings(partNumber,
                        Collections.singletonList(Triple.of("family" + now, chipNames[idx], "")));
                attributeArchetypeDao.flush();
                dates[i] = new Date();
                Thread.sleep(1000);
            }

            // An update should invalidate the old mapping but still leave it accessible, and make a new active one.
            Assert.assertNull(productEjb.getGenotypingChip(partNumber, "", new Date(now - 100000)).getRight());
            Assert.assertEquals(productEjb.getGenotypingChip(partNumber, "", dates[0]).getRight(), chipNames[0]);
            Assert.assertEquals(productEjb.getGenotypingChip(partNumber, "", dates[1]).getRight(), chipNames[1]);
            Assert.assertEquals(productEjb.getGenotypingChip(partNumber, "", dates[2]).getRight(), chipNames[1]);

        } finally {
            utx.rollback();
        }
    }
}
