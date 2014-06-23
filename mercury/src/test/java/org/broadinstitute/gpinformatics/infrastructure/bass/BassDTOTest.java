/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the 
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support 
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its 
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.bass;

import com.google.common.base.CaseFormat;
import org.apache.commons.beanutils.PropertyUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Test(groups = TestGroups.DATABASE_FREE)
public class BassDTOTest {
    public void testGetValue() throws Exception {
        final String value = "A1234";
        BassDTO bassDTO = new BassDTO(new HashMap<BassDTO.BassResultColumn, String>() {{
            put(BassDTO.BassResultColumn.id, value);
        }});
        Assert.assertEquals(bassDTO.getValue(BassDTO.BassResultColumn.id), value);
    }

    public void testGetColumnWithNull() throws Exception {
        BassDTO bassDTO = new BassDTO(new HashMap<BassDTO.BassResultColumn, String>() {{
            put(BassDTO.BassResultColumn.md5, BassDTO.BASS_NULL_VALUE);
        }});
        Assert.assertEquals(bassDTO.getValue(BassDTO.BassResultColumn.id), "");
    }

    public void testGetInvalidDate() throws Exception {
        final String value = "I AM NOT A DATE";

        BassDTO bassDTO = new BassDTO(new HashMap<BassDTO.BassResultColumn, String>() {{
            put(BassDTO.BassResultColumn.stored_on, value);
        }});
        Assert.assertEquals(bassDTO.getStoredOn(), null);
    }

    public void testGetValidDate() throws Exception {
        final String value = "2012_12_25_18_50_32_EDT";
        String expectedResult = "Tue Dec 25 17:50:32 EST 2012";

        BassDTO bassDTO = new BassDTO(new HashMap<BassDTO.BassResultColumn, String>() {{
            put(BassDTO.BassResultColumn.stored_on, value);
        }});
        Assert.assertEquals(bassDTO.getStoredOn().toString(), expectedResult);
    }

    public void testGetInt() throws Exception {
        final String value = "0";
        BassDTO bassDTO = new BassDTO(new HashMap<BassDTO.BassResultColumn, String>() {{
            put(BassDTO.BassResultColumn.version, value);
        }});
        Assert.assertEquals(bassDTO.getVersion(), (Integer) Integer.parseInt(value));
    }

    public void testGetNullInt() throws Exception {
        final String value = "";
        BassDTO bassDTO = new BassDTO(new HashMap<BassDTO.BassResultColumn, String>() {{
            put(BassDTO.BassResultColumn.version, value);
        }});
        Assert.assertNull(bassDTO.getVersion());
    }

    public void testGetters()
            throws IntrospectionException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        BassDTO bassDTO = new BassDTO(new HashMap<BassDTO.BassResultColumn, String>() {{
            put(BassDTO.BassResultColumn.id, "id");
        }});

        List<String> missingGetters = new ArrayList<>();
        for (BassDTO.BassResultColumn bassResultColumn : BassDTO.BassResultColumn.values()) {
            String propertyName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, bassResultColumn.name());

            try {
                PropertyUtils.getProperty(bassDTO, propertyName);
            } catch (NoSuchMethodException e) {
                missingGetters.add(propertyName);
            }
        }
        Assert.assertTrue(missingGetters.isEmpty(),
                String.format("Getters were missing for the following %d column headers: %s", missingGetters.size(),
                        missingGetters));

    }
}
