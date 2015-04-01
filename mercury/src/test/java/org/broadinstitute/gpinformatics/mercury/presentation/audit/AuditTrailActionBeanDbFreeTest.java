package org.broadinstitute.gpinformatics.mercury.presentation.audit;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.ReflectionUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

@Test(enabled = true, groups = TestGroups.DATABASE_FREE)
public class AuditTrailActionBeanDbFreeTest {
    AuditTrailActionBean auditTrailActionBean = new AuditTrailActionBean();

    // Tests that all persisted classnames are found in either the action bean's expected list or excluded list.
    @Test
    public void testEntityNames() throws Exception {
        List<String> entityDisplayNames = new ArrayList<>(auditTrailActionBean.getEntityDisplayNames());
        List<String> persistedClassnames = new ArrayList<>(ReflectionUtil.getMercuryAthenaEntityClassnames());
        persistedClassnames.removeAll(ReflectionUtil.getAbstractEntityClassnames());
        Assert.assertFalse(entityDisplayNames.isEmpty());
        Assert.assertFalse(persistedClassnames.isEmpty());

        for (String persistedClassname : persistedClassnames) {
            boolean found = false;
            for (String entityDisplayName : entityDisplayNames) {
                if (persistedClassname.endsWith(entityDisplayName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (String excludedClassname : auditTrailActionBean.getExcludedDisplayClassnames()) {
                    if (persistedClassname.endsWith(excludedClassname)) {
                        found = true;
                    }
                }
            }
            Assert.assertTrue(found, "Action bean does not account for " + persistedClassname);
        }
    }

}