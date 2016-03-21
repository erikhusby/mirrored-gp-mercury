package org.broadinstitute.gpinformatics.athena.entity.preference;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.util.Arrays;

/**
 * Test marshalling
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ColumnSetsPreferenceTest {
    @Test
    public void testMarshal() {
        try {
            ColumnSetsPreference.ColumnSet columnSet = new ColumnSetsPreference.ColumnSet("Default",
                    ColumnEntity.LAB_METRIC, Arrays.asList("Barcode", "Mercury Sample ID", "Metric Date",
                    "Metric Type", "Metric Value"));
            ColumnSetsPreference columnSetsPreference = new ColumnSetsPreference();
            columnSetsPreference.getColumnSets().add(columnSet);
            String marshal = columnSetsPreference.marshal();
            System.out.println(marshal);
            Assert.assertFalse(marshal.isEmpty());
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}