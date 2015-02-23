package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ParentVesselBean;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

/**
 * Tests the upload of a spreadsheet containing sample IDs and 2D tube barcodes.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class UploadSampleVesselsTest {

    @Test
    public void testParentChild() {
        InputStream testSpreadSheetInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "testdata/SampleVessel.xlsx");
        Assert.assertNotNull(testSpreadSheetInputStream);
        try {
            SampleParentChildVesselProcessor sampleParentChildVesselProcessor = new SampleParentChildVesselProcessor("Test");
            PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.<String, TableProcessor>emptyMap());

            parser.processRows(WorkbookFactory.create(testSpreadSheetInputStream).getSheetAt(0), sampleParentChildVesselProcessor);
            Assert.assertEquals(sampleParentChildVesselProcessor.getMapBarcodeToParentVessel().size(), 1);
            Assert.assertEquals(sampleParentChildVesselProcessor.getMapBarcodeToParentVessel().values().iterator().next().
                    getChildVesselBeans().size(), 16);
        } catch (ValidationException | IOException | InvalidFormatException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                testSpreadSheetInputStream.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Test
    public void testLoose() {
        InputStream testSpreadSheetInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "testdata/SampleVesselVacutainer.xlsx");
        Assert.assertNotNull(testSpreadSheetInputStream);
        try {
            SampleLooseVesselProcessor sampleLooseVesselProcessor = new SampleLooseVesselProcessor("Test");
            PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.<String, TableProcessor>emptyMap());

            parser.processRows(WorkbookFactory.create(testSpreadSheetInputStream).getSheetAt(0), sampleLooseVesselProcessor);
            Assert.assertEquals(sampleLooseVesselProcessor.getParentVesselBeans().size(), 16);
            ParentVesselBean parentVesselBean = sampleLooseVesselProcessor.getParentVesselBeans().get(0);
            Assert.assertEquals(parentVesselBean.getSampleId(), "SM-6X9JA");
            Assert.assertEquals(parentVesselBean.getManufacturerBarcode(), "SM-6X9JA");
            Assert.assertEquals(parentVesselBean.getVesselType(), "Vacutainer Blood Tube Yellow Top [10mL]");
        } catch (ValidationException | IOException | InvalidFormatException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                testSpreadSheetInputStream.close();
            } catch (IOException ignored) {
            }
        }
    }
}
