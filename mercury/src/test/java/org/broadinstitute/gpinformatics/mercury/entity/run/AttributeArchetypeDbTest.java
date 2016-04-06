package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtilityKeepScope;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
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
    final private String[] testFamilyAttribute = {testPrefix + "famAttr0", testPrefix + "famAttr1"};

    @Test(groups = TestGroups.ALTERNATIVES)
    public void testArchetype() throws Exception {
        Assert.assertNotNull(utx);
        utx.begin();

        // Ensure uniqueness.
        Assert.assertEquals(dao.findFamiliesIdentifiedByAttribute(testFamilyAttribute[0], testPrefix).size(), 0);

        // Adds family attributes on family 0.
        for (int i = 0; i < testFamilyAttribute.length; ++i) {
            AttributeDefinition attributeDefinition = new AttributeDefinition(testFamily[0], testFamilyAttribute[i],
                    (i == 0) ? testPrefix : null, false);
            dao.persist(attributeDefinition);
        }
        for (String family : testFamily) {
            Assert.assertTrue(CollectionUtils.isEmpty(dao.findAllByFamily(family)));
            // Adds attribute definitions.
            for (String attribute : testAttribute) {
                dao.persist(new AttributeDefinition(family, attribute, true));
            }
            // Adds archetype and attributes.
            for (String archetypeName : testArchetype) {
                AttributeArchetype archetype = new AttributeArchetype(family, archetypeName);
                dao.persist(archetype);
                // Must flush here to define archetypeId and avoid subsequent ArchetypeAttribute unique key failure.
                dao.flush();
                for (String attributeName : testAttribute) {
                    ArchetypeAttribute attribute =
                            new ArchetypeAttribute(archetype, attributeName, attributeName + "value");
                    archetype.getAttributes().add(attribute);
                }
            }
        }
        dao.flush();

        // Tests lookup family name by identifier attribute.
        Assert.assertEquals(dao.findFamiliesIdentifiedByAttribute(testAttribute[0], testAttribute[0] + "value").size(), 0);
        Assert.assertEquals(dao.findFamiliesIdentifiedByAttribute(testFamilyAttribute[0], null).size(), 0);
        List<String> identifiedFamilies = dao.findFamiliesIdentifiedByAttribute(testFamilyAttribute[0], testPrefix);
        Assert.assertEquals(identifiedFamilies.size(), 1);
        Assert.assertEquals(identifiedFamilies.get(0), testFamily[0]);

        // Checks the number of attributes defined for each family.
        Assert.assertEquals(dao.findAttributeDefinitionsByFamily(testFamily[0]).size(),
                testFamilyAttribute.length + testAttribute.length);
        Assert.assertEquals(dao.findAttributeDefinitionsByFamily(testFamily[1]).size(), testAttribute.length);
        Assert.assertEquals(dao.findAttributeDefinitionsByFamily(testFamily[2]).size(), testAttribute.length);

        // Verifies the family attributes.
        for (AttributeDefinition definition : dao.findAttributeDefinitionsByFamily(testFamily[0])) {
            boolean isFamilyAttribute = definition.getAttributeName().equals(testFamilyAttribute[0])
                                        || definition.getAttributeName().equals(testFamilyAttribute[1]);
            Assert.assertTrue(isFamilyAttribute == definition.isFamilyAttribute(), definition.getAttributeName());
            Assert.assertTrue(isFamilyAttribute != definition.isDisplayedInUi(), definition.getAttributeName());
        }
        Assert.assertEquals(dao.findAttributeDefinitionByFamily(testFamily[0], testFamilyAttribute[0]).
                getFamilyAttributeValue(), testPrefix);
        Assert.assertNull(dao.findAttributeDefinitionByFamily(testFamily[0], testFamilyAttribute[1]).
                getFamilyAttributeValue());

        Assert.assertNull(dao.findAttributeDefinitionByFamily(testFamily[0], testAttribute[0]).
                getFamilyAttributeValue());

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
        Assert.assertTrue(testAttribute.length > 2);
        for (int i = 1; i < 3; ++i) {
            Thread.sleep(500);
            versionDates.add(new Date());
            AttributeArchetype archetype = dao.findByName(family, archetypeName);
            Map<String, String> attributeMap = archetype.getAttributeMap();
            // Changes a different attributes each time.
            String attributeName = testAttribute[i];
            String attributeData = String.valueOf(i);
            attributeMap.put(attributeName, attributeData);
            AttributeArchetype newArchetype = dao.createArchetypeVersion(family, archetypeName, attributeMap);
            Assert.assertNotNull(newArchetype);
            if (i == 1) {
                // Marks the first new version so that it overrides the original version (i.e. hidden
                // in the search by version date).
                newArchetype.setOverridesEarlierVersions(true);
            }
        }
        // Bracket the last version with a final datetime.
        Thread.sleep(500);
        versionDates.add(new Date());

        // It should not make a new version from the latest attributes since there are no changes.
        Assert.assertNull(dao.createArchetypeVersion(family, archetypeName,
                dao.findByName(family, archetypeName).getAttributeMap()));

        // Checks the list of versions, already sorted by date, most recent first.
        List<AttributeArchetype> archetypeVersions = dao.findAllVersionsByFamilyAndName(family, archetypeName);
        Assert.assertEquals(archetypeVersions.size(), 3);

        // Checks the attributes.
        Date[] archetypeDates = new Date[archetypeVersions.size()];
        for (int i = 0; i < archetypeVersions.size(); ++i) {
            AttributeArchetype archetype = archetypeVersions.get(i);
            archetypeDates[i] = archetype.getCreatedDate();
            Map<String, String> map = archetype.getAttributeMap();
            Assert.assertEquals(map.size(), testAttribute.length);
            if (i == 0) {
                // latest
                Assert.assertEquals(map.get(testAttribute[0]), testAttribute[0] + "value");
                Assert.assertEquals(map.get(testAttribute[1]), "1");
                Assert.assertEquals(map.get(testAttribute[2]), "2");
                Assert.assertFalse(archetype.getOverridesEarlierVersions());
            } else if (i == 1) {
                // middle version
                Assert.assertEquals(map.get(testAttribute[0]), testAttribute[0] + "value");
                Assert.assertEquals(map.get(testAttribute[1]), "1");
                Assert.assertEquals(map.get(testAttribute[2]), testAttribute[2] + "value");
                Assert.assertTrue(archetype.getOverridesEarlierVersions());
            } else if (i == 2) {
                // earliest
                Assert.assertEquals(map.get(testAttribute[0]), testAttribute[0] + "value");
                Assert.assertEquals(map.get(testAttribute[1]), testAttribute[1] + "value");
                Assert.assertEquals(map.get(testAttribute[2]), testAttribute[2] + "value");
                Assert.assertFalse(archetype.getOverridesEarlierVersions());
            }
        }
        Assert.assertTrue(archetypeDates[2].before(archetypeDates[1]));
        Assert.assertTrue(archetypeDates[1].before(archetypeDates[0]));

        // Looking for archetype by effective date should only reveal two different versions, since the middle one
        // was marked to override the earlier version.
        for (int i = 0; i < versionDates.size(); ++i) {
            AttributeArchetype archetype = dao.findByName(family, archetypeName, versionDates.get(i));
            Assert.assertNotNull(archetype);
            switch (i) {
            case 0:
            case 1:
            case 2:
                Assert.assertEquals(archetype.getCreatedDate(), archetypeDates[1]);
                Assert.assertEquals(archetype.getAttributeMap().get(testAttribute[1]), "1");
                break;
            case 3:
                Assert.assertEquals(archetype.getCreatedDate(), archetypeDates[0]);
                break;
            default:
                Assert.fail();
            }
        }

        utx.rollback();
    }
}
