package org.broadinstitute.gpinformatics.infrastructure.jira;

import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = TestGroups.DATABASE_FREE)
public class CustomFieldTest {

    @Test
    public void testNullValueIncludesFieldName() {
        CustomField field = null;
        String theFieldName = "My Field";
        try {
            field = new CustomField(new CustomFieldDefinition("id",theFieldName,false),null);
        }
        catch(NullPointerException e) {
            Assert.assertTrue(e.getMessage().contains(theFieldName),"Now I can't tell from the stack trace which field was left blank.");
        }
    }
}
