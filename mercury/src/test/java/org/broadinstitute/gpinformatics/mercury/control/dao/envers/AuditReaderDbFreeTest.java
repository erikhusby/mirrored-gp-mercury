package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.AggregationReadGroup;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.sample.BulkQueryParameter;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Test(enabled = true, groups = TestGroups.DATABASE_FREE)
public class AuditReaderDbFreeTest {

    // Exceptions to the Entity classes.  The Audit Trail UI requires a Long entity id in order to
    // show data audits that changed or deleted the entity.
    private final Collection<Class> unauditableClasses = new ArrayList<Class>() {{
        add(JiraTicket.class);   // todo make a Long primary key and remove this special case.
        add(BulkQueryParameter.class);
    }};

    @Test
    public void areAllEntitiesAuditable() throws Exception {
        List<String> failingClasses = new ArrayList<>();
        List<Class> entityClasses = new ArrayList<>(ReflectionUtil.getMercuryAthenaEntityClasses());

        Assert.assertTrue(CollectionUtils.isNotEmpty(entityClasses));
        Assert.assertTrue(entityClasses.contains(LabEvent.class));
        Assert.assertTrue(entityClasses.contains(VesselContainer.class));
        Assert.assertTrue(entityClasses.contains(Product.class));
        // Should not contain the gap metric db entity classes.
        Assert.assertFalse(entityClasses.contains(AggregationReadGroup.class));

        entityClasses.removeAll(ReflectionUtil.getEmbeddableEntities());
        entityClasses.removeAll(unauditableClasses);
        Assert.assertTrue(entityClasses.size() > 0);

        for (Class cls : entityClasses) {
            Field field = ReflectionUtil.getEntityIdField(cls);
            if (field == null || !field.getGenericType().equals(Long.class)) {
                failingClasses.add(cls.getSimpleName());
            }
        }

        if (failingClasses.size() > 0) {
            Assert.fail("Entity definition error. Primary key must have '@Id' and type must be 'Long' in class: " +
                        StringUtils.join(failingClasses, ", "));
        }
    }

    // Checks that all fields found on a mercury/athena generated class, which are the ones used by the
    // audit trail, are fields that are accessible on the Hibernate persisted class (i.e. the entity class).
    @Test
    public void persistentFieldsMissingFromEntity() throws Exception {
        List<Class> entityClasses = new ArrayList<>(ReflectionUtil.getMercuryAthenaEntityClasses());
        entityClasses.removeAll(unauditableClasses);
        Assert.assertTrue(entityClasses.size() > 0);

        for (Class aClass : entityClasses) {
            List<Field> fields = ReflectionUtil.getPersistedFieldsForClass(aClass);
            if (CollectionUtils.isNotEmpty(fields)) {
                Assert.assertTrue(CollectionUtils.isNotEmpty(fields), "Missing fields on class " + aClass.getName());
            }
        }
    }

    @Test
    public void testFormatFields() throws Exception {
        final String barcode = "A00000001";

        BarcodedTube barcodedTube = new BarcodedTube(barcode, BarcodedTube.BarcodedTubeType.VacutainerBloodTube10);
        barcodedTube.addSample(new MercurySample("SM-0", MercurySample.MetadataSource.BSP));
        barcodedTube.addSample(new MercurySample("SM-1", MercurySample.MetadataSource.BSP));

        List<EntityField> entityFields = ReflectionUtil.formatFields(barcodedTube, barcodedTube.getClass());
        Assert.assertTrue(CollectionUtils.isNotEmpty(entityFields));

        // entityId should be first in the list, but since value is null the canonicalClassname is null too.
        Assert.assertEquals(entityFields.get(0).getFieldName(), "labVesselId");
        Assert.assertNull(entityFields.get(0).getCanonicalClassname());

        boolean[] found = new boolean[]{false, false, false};
        for (EntityField entityField : entityFields) {
            if (entityField.getFieldName().equals("tubeType")) {
                found[0] = true;
                Assert.assertEquals(entityField.getValue(),
                        BarcodedTube.BarcodedTubeType.VacutainerBloodTube10.toString());
            }
            if (entityField.getFieldName().equals("mercurySamples")) {
                found[1] = true;
                Assert.assertNull(entityField.getValue());
                // Verifies the value list.
               Assert.assertNull(entityField.getCanonicalClassname());
                Assert.assertEquals(entityField.getEntityFieldList().size(), 2);
                EntityField sampleEntityField = entityField.getEntityFieldList().get(0);
                // null here because the entityId value is null.
                Assert.assertNull(sampleEntityField.getCanonicalClassname());
                // This value is for mercurySample entityId, which is null until object gets persisted.
                Assert.assertEquals(sampleEntityField.getValue(), ReflectionUtil.NULL_REPRESTATION);
            }
            if (entityField.getFieldName().equals("label")) {
                found[2] = true;
                Assert.assertEquals(entityField.getValue(), barcode);
                Assert.assertNull(entityField.getEntityFieldList());
                Assert.assertNull(entityField.getCanonicalClassname());
            }
        }
        for (boolean test : found) {
            Assert.assertTrue(test);
        }
    }

    @Test
    public void testAbstractClass() throws Exception {
        Assert.assertTrue(ReflectionUtil.getAbstractEntities().contains(LabVessel.class));
        Assert.assertTrue(ReflectionUtil.getAbstractEntityClassnames().contains(
                LabVessel.class.getCanonicalName()));
    }
}
