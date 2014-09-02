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

package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetValidator;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isOneOf;

@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestImporterTest {
    public static final String TEST_MANIFEST = "test-manifest.xls";
    private ManifestImportProcessor manifestImportProcessor;

    @BeforeMethod
    public void setUp() throws Exception {
        manifestImportProcessor = new ManifestImportProcessor();
    }

    public void testImport() throws InvalidFormatException, IOException, ValidationException {
        InputStream stream = new FileInputStream(TestUtils.getTestData(TEST_MANIFEST));
        PoiSpreadsheetParser.processSingleWorksheet(stream, manifestImportProcessor);

        for (Map<String, String> manifestRow : manifestImportProcessor.getManifestList()) {
            PoiSpreadsheetValidator.validateSpreadsheetRow(manifestRow, ManifestHeader.class);
            for (Map.Entry<String, String> manifestCell : manifestRow.entrySet()) {
                String header = manifestCell.getKey();
                String value = manifestCell.getValue();

                if (header.equals(ManifestHeader.TUMOR_OR_NORMAL.getText())){
                    assertThat(value, isOneOf("Tumor", "Normal"));
                }
            }
        }
    }
}
