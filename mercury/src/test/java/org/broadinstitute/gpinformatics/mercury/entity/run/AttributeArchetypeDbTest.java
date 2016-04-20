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
import java.util.Set;

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

    final private String namespace = getClass().getCanonicalName();
    final private String testPrefix = String.format("%s", System.currentTimeMillis());
    final private String[] testGroup = {
            testPrefix + "group0",
            testPrefix + "group1"};
    final private String[] testArchetype = {
            testPrefix + "arch0",
            testPrefix + "arch1",
            testPrefix + "arch2",
            testPrefix + "arch3"};
    final private String[] testAttribute = {
            testPrefix + "attrib0",
            testPrefix + "attrib1",
            testPrefix + "attrib2"};
    final private String[] testGroupAttribute = {
            testPrefix + "famAttr0",
            testPrefix + "famAttr1"};
    final private String[] testGroupAttributeValue = {
            testPrefix + "groupValue0",
            null};

    @Test(groups = TestGroups.ALTERNATIVES)
    public void testArchetype() throws Exception {
        Assert.assertNotNull(utx);
        utx.begin();

        // Adds Group attributes on Group 0.
        for (int i = 0; i < testGroupAttribute.length; ++i) {
            AttributeDefinition attributeDefinition = new AttributeDefinition(namespace, testGroup[0],
                    testGroupAttribute[i], testGroupAttributeValue[i], false, true);
            dao.persist(attributeDefinition);
        }

        for (String group : testGroup) {
            Assert.assertTrue(CollectionUtils.isEmpty(dao.findByGroup(namespace, group)));
            // Adds attribute definitions.
            for (String attribute : testAttribute) {
                dao.persist(new AttributeDefinition(namespace, group, attribute, null, true, false));
            }
            // Adds archetype and attributes.
            for (String archetypeName : testArchetype) {
                AttributeArchetype archetype = new AttributeArchetype(namespace, group, archetypeName);
                for (String attributeName : testAttribute) {
                    ArchetypeAttribute attribute =
                            new ArchetypeAttribute(archetype, attributeName, attributeName + "value");
                    archetype.getAttributes().add(attribute);
                }
                dao.persist(archetype);
            }
        }
        dao.flush();

        // Tests lookup group.
        for (int i = 0; i < testGroup.length; ++i) {
            Assert.assertTrue(dao.findGroups(namespace).contains(testGroup[i]));

            // Checks the attribute definitions for each Group.
            Map<String, AttributeDefinition> definitionMap = dao.findAttributeDefinitions(namespace, testGroup[i]);
            if (i == 0) {
                Assert.assertEquals(definitionMap.size(), testGroupAttribute.length + testAttribute.length);
                for (int j = 0; j < testGroupAttribute.length; ++j) {
                    Assert.assertEquals(definitionMap.get(testGroupAttribute[j]).getNamespace(), namespace);
                    Assert.assertEquals(definitionMap.get(testGroupAttribute[j]).getGroupAttributeValue(),
                            testGroupAttributeValue[j]);
                    Assert.assertFalse(definitionMap.get(testGroupAttribute[j]).isDisplayable());
                    Assert.assertTrue(definitionMap.get(testGroupAttribute[j]).isGroupAttribute());
                }
            } else {
                for (int j = 0; j < testGroupAttribute.length; ++j) {
                    Assert.assertFalse(definitionMap.containsKey(testGroupAttribute[j]));
                }
            }
            for (int j = 0; j < testAttribute.length; ++j) {
                Assert.assertNull(definitionMap.get(testAttribute[j]).getGroupAttributeValue());
                Assert.assertTrue(definitionMap.get(testAttribute[j]).isDisplayable());
                Assert.assertFalse(definitionMap.get(testAttribute[j]).isGroupAttribute());
            }

            // Tests the archetypes and their attributes.

            Assert.assertEquals(dao.findByGroup(namespace, testGroup[i]).size(), testArchetype.length);
            for (String archetypeName : testArchetype) {
                AttributeArchetype archetype = dao.findByName(namespace, testGroup[i], archetypeName);
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
