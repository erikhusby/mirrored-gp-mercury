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
import java.util.Arrays;
import java.util.HashMap;
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
                    (i == 0) ? testPrefix : null, false, true);
            dao.persist(attributeDefinition);
        }
        for (String family : testFamily) {
            Assert.assertTrue(CollectionUtils.isEmpty(dao.findAllByFamily(family)));
            // Adds attribute definitions.
            for (String attribute : testAttribute) {
                dao.persist(new AttributeDefinition(family, attribute, null, true, false));
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
        Map<String, AttributeDefinition>[] definitionMaps = new HashMap[testFamily.length];
        for (int i = 0; i < definitionMaps.length; ++i) {
            definitionMaps[i] = dao.findAttributeDefinitionsByFamily(testFamily[i]);
        }
        Assert.assertEquals(definitionMaps[0].size(), testFamilyAttribute.length + testAttribute.length);
        Assert.assertEquals(definitionMaps[1].size(), testAttribute.length);
        Assert.assertEquals(definitionMaps[2].size(), testAttribute.length);

        // Verifies the family attributes.
        for (AttributeDefinition definition : definitionMaps[0].values()) {
            boolean isFamilyAttribute = definition.getAttributeName().equals(testFamilyAttribute[0])
                                        || definition.getAttributeName().equals(testFamilyAttribute[1]);
            Assert.assertTrue(isFamilyAttribute == definition.isFamilyAttribute(), definition.getAttributeName());
            Assert.assertTrue(isFamilyAttribute != definition.isDisplayable(), definition.getAttributeName());
        }
        Assert.assertEquals(definitionMaps[0].get(testFamilyAttribute[0]).getFamilyAttributeValue(), testPrefix);
        Assert.assertNull(definitionMaps[0].get(testFamilyAttribute[1]).getFamilyAttributeValue());
        Assert.assertNull(definitionMaps[0].get(testAttribute[0]).getFamilyAttributeValue());

        // Tests the archetypes and their attributes.
        for (String family : testFamily) {
            Assert.assertEquals(dao.findAllByFamily(family).size(), testAttribute.length);
            for (String archetypeName : testArchetype) {
                AttributeArchetype archetype = dao.findByName(family, archetypeName);
                Assert.assertNotNull(archetype);
                // Test the attributes.
                Assert.assertEquals(archetype.getAttributes().size(), testAttribute.length);
                for (ArchetypeAttribute attribute : archetype.getAttributes()) {
                    Assert.assertTrue(Arrays.asList(testAttribute).contains(attribute.getAttributeName()));
                    Assert.assertEquals(attribute.getArchetype(), archetype);
                    Assert.assertEquals(attribute.getAttributeValue(), attribute.getAttributeName() + "value");
                }
            }
        }
        utx.rollback();
    }
}
