package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import com.lowagie.text.pdf.Barcode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.audit.AuditEntity;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Test(enabled = true, groups = TestGroups.DATABASE_FREE)
public class AuditReaderDbFreeTest {

    // Exceptions to the Entity classes having Long entityId annotated with @Id.
    private final Collection<Class> unauditableClasses = new ArrayList<Class>() {{
        // todo make a Long primary key on JiraTicket and remove this special case.
        add(JiraTicket.class);
        // VesselContainer is embeddable.
        add(VesselContainer.class);
    }};


    @Test
    public void areAllEntitiesAuditable() throws Exception {
        List<String> failingClasses = new ArrayList<>();
        List<Class> classesFromPkg = ReflectionUtil.getMercuryAthenaClasses();
        List<String> entityClassnames = ReflectionUtil.getEntityClassnames(classesFromPkg);
        Assert.assertTrue(entityClassnames.size() > 0);

        for (Class cls : classesFromPkg) {
            if (entityClassnames.contains(cls.getCanonicalName()) && !unauditableClasses.contains(cls)) {
                if (ReflectionUtil.getEntityIdField(cls) == null) {
                    failingClasses.add(cls.getName());
                }
            }
        }

        if (failingClasses.size() > 0) {
            Assert.fail("Entity definition error -- missing @Id on Long field which is the primary key in class " +
                        StringUtils.join(failingClasses, ", "));
        }
    }

    // Checks that all fields found on a mercury/athena generated class, which are the ones used by the
    // audit trail, are fields that are accessible on the Hibernate persisted class (i.e. the entity class).
    @Test
    public void persistentFieldsMissingFromEntity() throws Exception {
        List<Class> classesFromPkg = ReflectionUtil.getMercuryAthenaClasses();
        List<String> entityClassnames = ReflectionUtil.getEntityClassnames(classesFromPkg);
        Assert.assertTrue(entityClassnames.size() > 0);
        for (String classname : entityClassnames) {
            List<Field> fields =
                    ReflectionUtil.getPersistedFieldsForClass(ReflectionUtil.getMercuryAthenaEntityClass(classname));
            Assert.assertTrue(CollectionUtils.isNotEmpty(fields), "Missing fields on class " + classname);
        }
    }

    @Test
    public void testFormatFields() throws Exception {
        final String barcode = "A00000001";

        BarcodedTube barcodedTube = new BarcodedTube(barcode, BarcodedTube.BarcodedTubeType.VacutainerBloodTube10);
        barcodedTube.addSample(new MercurySample("SM-0"));
        barcodedTube.addSample(new MercurySample("SM-1"));

        List<EntityField> entityFields = ReflectionUtil.formatFields(barcodedTube, barcodedTube.getClass());
        Assert.assertTrue(CollectionUtils.isNotEmpty(entityFields));

        // entityId should be first in the list
        Assert.assertEquals(entityFields.get(0).getFieldName(), "labVesselId");
        Assert.assertEquals(entityFields.get(0).getCanonicalClassname(), BarcodedTube.class.getCanonicalName());

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
                Assert.assertEquals(entityField.getValueList().size(), 2);
                EntityField sampleEntityField = entityField.getValueList().get(0);
                Assert.assertEquals(sampleEntityField.getCanonicalClassname(), MercurySample.class.getCanonicalName());
                // This value is for mercurySample entityId, which is null until object gets persisted.
                Assert.assertEquals(sampleEntityField.getValue(), ReflectionUtil.NULL_REPRESTATION);
            }
            if (entityField.getFieldName().equals("label")) {
                found[2] = true;
                Assert.assertEquals(entityField.getValue(), barcode);
                Assert.assertNull(entityField.getValueList());
                Assert.assertNull(entityField.getCanonicalClassname());
            }
        }
        for (boolean test : found) {
            Assert.assertTrue(test);
        }
    }
}
