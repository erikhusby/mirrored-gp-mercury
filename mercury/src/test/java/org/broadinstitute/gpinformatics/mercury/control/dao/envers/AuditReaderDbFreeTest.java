package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.persistence.Id;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
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
                Field field = ReflectionUtil.getFieldHavingAnnotation(cls, Id.class);
                if (field == null) {
                    field = ReflectionUtil.getEntityIdField(cls);
                }
                if (field == null) {
                    failingClasses.add(cls.getName());
                }
            }
        }

        if (failingClasses.size() > 0) {
            Assert.fail("Entity definition error -- missing @Id on Long field which is the primary key in class " +
                        StringUtils.join(failingClasses, ", "));
        }
    }

}
