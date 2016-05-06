package org.broadinstitute.gpinformatics.mercury.entity.run;

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
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Tests the GenotypingChip, its related entities, and its dao.
 */
@Test(groups = TestGroups.ALTERNATIVES, singleThreaded = true)
public class GenotypingChipDbTest extends Arquillian {

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

        // Adds Group attributes on Group 0 and 1.
        for (int i = 0; i < testGroupAttribute.length; ++i) {
            for (int j = 0; j < 2; ++j) {
                AttributeDefinition attributeDefinition = new AttributeDefinition(
                        AttributeDefinition.DefinitionType.GENOTYPING_CHIP, testGroup[j],
                        testGroupAttribute[i],
                        testGroupAttributeValue[i] != null ? testGroupAttributeValue[i] + j : null);
                dao.persist(attributeDefinition);
            }
        }


        for (String group : testGroup) {
            Assert.assertEquals(dao.findGenotypingChipAttributeDefinitions(group).size(), 0);
            // Adds attribute definitions.
            List<AttributeDefinition> definitions = new ArrayList<>();
            for (String attribute : testAttribute) {
                definitions.add(new AttributeDefinition(AttributeDefinition.DefinitionType.GENOTYPING_CHIP,
                        group, attribute, true));
            }
            dao.persistAll(definitions);
            // Adds archetype and attributes.
            for (String archetypeName : testArchetype) {
                AttributeArchetype chip = new GenotypingChip(group, archetypeName, definitions);
                for (String attributeName : testAttribute) {
                    chip.addOrSetAttribute(attributeName, attributeName + "value");
                }
                dao.persist(chip);
            }
        }
        dao.flush();

        // Tests lookup group.
        for (int i = 0; i < testGroup.length; ++i) {
            // Checks the attribute definitions for each Group.
            Map<String, AttributeDefinition> definitionMap = dao.findGenotypingChipAttributeDefinitions(testGroup[i]);
            if (i < 2) {
                Assert.assertEquals(definitionMap.size(), testGroupAttribute.length + testAttribute.length);
                for (int j = 0; j < testGroupAttribute.length; ++j) {
                    Assert.assertEquals(definitionMap.get(testGroupAttribute[j]).getDefinitionType(),
                            AttributeDefinition.DefinitionType.GENOTYPING_CHIP);
                    Assert.assertEquals(definitionMap.get(testGroupAttribute[j]).getGroupAttributeValue(),
                            (j == 0) ? testGroupAttributeValue[j] + i : null);
                    Assert.assertFalse(definitionMap.get(testGroupAttribute[j]).isDisplayable());
                    Assert.assertTrue(definitionMap.get(testGroupAttribute[j]).isGroupAttribute());
                }

                // Tests the alternative lookup method.
                Assert.assertEquals(dao.findChipFamilyAttribute(testGroup[i], testGroupAttribute[0]),
                        testGroupAttributeValue[0] + i);

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

            Assert.assertEquals(dao.findGenotypingChips(testGroup[i]).size(), testArchetype.length);
            for (String archetypeName : testArchetype) {
                GenotypingChip chip = dao.findGenotypingChip(testGroup[i], archetypeName);
                Assert.assertNotNull(chip);
                // Test the attributes.
                Assert.assertEquals(chip.getAttributes().size(), testAttribute.length);
                for (ArchetypeAttribute attribute : chip.getAttributes()) {
                    Assert.assertTrue(Arrays.asList(testAttribute).contains(attribute.getAttributeName()));
                    Assert.assertEquals(attribute.getArchetype(), chip);
                    Assert.assertEquals(attribute.getAttributeValue(), attribute.getAttributeName() + "value");
                }
            }
        }

        utx.rollback();
    }
}
