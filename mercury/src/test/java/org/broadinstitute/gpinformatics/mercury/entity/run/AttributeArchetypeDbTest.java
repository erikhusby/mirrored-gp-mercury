package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtilityKeepScope;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.run.InfiniumRunResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Tests the AttributeArchetype, its related entities, and its dao.
 */
@Test(groups = TestGroups.ALTERNATIVES, singleThreaded = true)
public class AttributeArchetypeDbTest extends Arquillian {

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

    @Inject
    private AttributeArchetypeDao dao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(DEV, "dev", SessionContextUtilityKeepScope.class);
    }

    final private String testPrefix = String.format("%s", System.currentTimeMillis());
    final private String[] testFamily = {testPrefix + "family0", testPrefix + "family1", testPrefix + "family2"};
    final private String[] testArchetype = {testPrefix + "arch0", testPrefix + "arch1", testPrefix + "arch2"};
    final private String[] testAttribute = {testPrefix + "attrib0", testPrefix + "attrib1", testPrefix + "attrib2"};
    final private String[] testGlobal = {testPrefix + "global0", testPrefix + "global1"};

    @Test(groups = TestGroups.ALTERNATIVES)
    public void testArchetype() throws Exception {
        Assert.assertNotNull(utx);
        utx.begin();

        // Ensures uniqueness.
        Assert.assertTrue(CollectionUtils.isEmpty(dao.findFamilyNamesByPrefix(testPrefix)));

        for (String family : testFamily) {
            Assert.assertTrue(CollectionUtils.isEmpty(dao.findAllByFamily(family)));
            // Adds attribute definitions.
            for (String attribute : testAttribute) {
                dao.persist(new AttributeDefinition(family, attribute));
            }
            // Adds archetype and attributes.
            for (String archetypeName : testArchetype) {
                AttributeArchetype archetype = new AttributeArchetype(family, archetypeName);
                dao.persist(archetype);
                dao.flush(); // Must flush here to prevent attribute unique key failure (probably archetype=null).
                for (String attributeName : testAttribute) {
                    ArchetypeAttribute attribute =
                            new ArchetypeAttribute(archetype, attributeName, attributeName + "value");
                    archetype.getAttributes().add(attribute);
                    dao.persist(attribute);
                }
            }
        }
        // Adds globals on family 0 only.
        for (String global : testGlobal) {
            dao.persist(new AttributeDefinition(testFamily[0], global, global + "value"));
        }
        dao.flush();

        // Families should be present.
        List<String> familyByPrefix = dao.findFamilyNamesByPrefix(testPrefix);
        Assert.assertEquals(familyByPrefix.size(), testFamily.length);
        Assert.assertTrue(familyByPrefix.containsAll(Arrays.asList(testFamily)));

        // Checks the number of attributes defined for each family.
        Assert.assertEquals(dao.findAttributeDefinitionsByFamily(testFamily[0]).size(),
                testGlobal.length + testAttribute.length);
        Assert.assertEquals(dao.findAttributeDefinitionsByFamily(testFamily[1]).size(), testAttribute.length);
        Assert.assertEquals(dao.findAttributeDefinitionsByFamily(testFamily[2]).size(), testAttribute.length);

        // A family attribute ("global" attribute) must have a non-null value in AttributeDefinition.
        Assert.assertEquals(dao.findAttributeDefinitionsByFamily(testFamily[0], testGlobal[0]).
                getAttributeFamilyClassFieldValue(), testGlobal[0] + "value");
        // Verfies archetype attribute is present but its value is null in AttributeDefinition.
        Assert.assertNotNull(dao.findAttributeDefinitionsByFamily(testFamily[0], testAttribute[0]));
        Assert.assertNull(dao.findAttributeDefinitionsByFamily(testFamily[0], testAttribute[0]).
                getAttributeFamilyClassFieldValue());

        // Tests the archetypes and their attributes.
        for (String family : testFamily) {
            Assert.assertEquals(dao.findAllByFamily(family).size(), testAttribute.length);
            for (String archetypeName : testArchetype) {
                AttributeArchetype archetype = dao.findByName(family, archetypeName);
                Assert.assertNotNull(archetype);
                // Test lookups by date.
                Assert.assertNull(dao.findByName(family, archetypeName, new Date(0)));
                Assert.assertEquals(dao.findByName(family, archetypeName, archetype.getCreatedDate()), archetype);
                Assert.assertEquals(dao.findByName(family, archetypeName, new Date()), archetype);
                // Test the attributes.
                Assert.assertEquals(archetype.getAttributes().size(), testAttribute.length);
                for (ArchetypeAttribute attribute : archetype.getAttributes()) {
                    Assert.assertTrue(Arrays.asList(testAttribute).contains(attribute.getAttributeName()));
                    Assert.assertEquals(attribute.getArchetype(), archetype);
                    Assert.assertEquals(attribute.getAttributeValue(), attribute.getAttributeName() + "value");
                }
            }
        }

        // Adds two archetype versions.
        String family = testFamily[0];
        String archetypeName = testArchetype[0];
        List<Date> versionDates = new ArrayList<>();
        versionDates.add(new Date());
        for (int i = 1; i < 3; ++i) {
            Thread.sleep(500);
            versionDates.add(new Date());
            AttributeArchetype archetype = new AttributeArchetype(family, archetypeName);
            if (i == 1) {
                // Marks the middle version as overriding the earlier one.
                archetype.setOverridesEarlierVersions(true);
            }
            dao.persist(archetype);
            String attributeName = testAttribute[i];
            String attributeData = String.valueOf(i);
            ArchetypeAttribute attribute = new ArchetypeAttribute(archetype, attributeName, attributeData);
            archetype.getAttributes().add(attribute);
            dao.persist(attribute);
        }
        dao.flush();

        // Checks the list of versions, already sorted by date, most recent first.
        List<AttributeArchetype> archetypeVersions = dao.findAllVersionsByFamilyAndName(family, archetypeName);
        Assert.assertEquals(archetypeVersions.size(), 3);

        // Oldest one should have 3 attributes
        Map<String, String> map = archetypeVersions.get(2).getAttributeMap();
        Assert.assertEquals(map.size(), 3);
        for (int j = 0; j < testAttribute.length; ++j) {
            Assert.assertEquals(map.get(testAttribute[j]), testAttribute[j] + "value");
        }

        // Looking for archetype by effective date should only reveal two versions, since the middle one
        // was marked to override the earlier version.
        for (int i = 0; i < versionDates.size(); ++i) {
            AttributeArchetype archetype = dao.findByName(family, archetypeName, versionDates.get(i));
            Assert.assertNotNull(archetype);
            switch (i) {
            case 0:
            case 1:
                Assert.assertNull(archetype.getAttributeMap().get(testAttribute[0]));
                Assert.assertEquals(archetype.getAttributeMap().get(testAttribute[1]), "1");
                Assert.assertNull(archetype.getAttributeMap().get(testAttribute[2]));
                break;
            case 2:
                Assert.assertNull(archetype.getAttributeMap().get(testAttribute[0]));
                Assert.assertNull(archetype.getAttributeMap().get(testAttribute[1]));
                Assert.assertEquals(archetype.getAttributeMap().get(testAttribute[2]), "2");
                break;
            default:
                Assert.fail("Shouldn't be here");
            }
        }
        utx.rollback();
    }
}
