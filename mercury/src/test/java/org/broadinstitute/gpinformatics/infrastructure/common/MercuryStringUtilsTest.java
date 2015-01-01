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

package org.broadinstitute.gpinformatics.infrastructure.common;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups = TestGroups.DATABASE_FREE)
public class MercuryStringUtilsTest {

    @DataProvider(name = "camelCaseDataProvider")
    public Object[][] camelCaseDataProvider(){
        return new Object[][]{
                {"lowercase", "lowercase"},
                {"lowerCase", "lower Case"},
                {"Class", "Class"},
                {"MyClass", "My Class"},
                {"HTML", "HTML"},
                {"PDFLoader", "PDF Loader"},
                {"AString", "A String"},
                {"SimpleXMLParser", "Simple XML Parser"},
                {"GL11Version", "GL 11 Version"},
                {"99Bottles", "99 Bottles"},
                {"May5", "May 5"},
                {"BFG9000", "BFG 9000"},
        };
    }

    @Test(groups = TestGroups.DATABASE_FREE, dataProvider = "camelCaseDataProvider")
    public void testSplitCamelCase(String inputString, String resultString){
           Assert.assertEquals(MercuryStringUtils.splitCamelCase(inputString), resultString);
    }
}
