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

package org.broadinstitute.gpinformatics.athena.boundary.products;

import com.lowagie.text.pdf.PRTokeniser;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.RandomAccessFileOrArray;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductPdfFactoryTest {
    Logger logger = Logger.getLogger(this.getClass().getName());
    private File tempFile;

    @BeforeMethod
    public void setUp() throws Exception {
        tempFile = File.createTempFile(this.getClass().getSimpleName() + System.currentTimeMillis(), ".pdf");
        logger.info("creating pdf file " + tempFile.getPath());
        tempFile.deleteOnExit();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    public void testTempFile() {
        long fileSize = FileUtils.sizeOf(tempFile);
        Assert.assertEquals(fileSize, 0, "file should be empty but is " + fileSize + " bytes.");
    }

    public void testFileWritten() throws Exception {
        Product dummyProduct = ProductTestFactory
                .createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "P-TEST-" + System.currentTimeMillis());
        ProductPdfFactory.toPdf(new FileOutputStream(tempFile), ProductTestFactory.createStandardExomeSequencing(), dummyProduct);
        long fileSize = FileUtils.sizeOf(tempFile);
        Assert.assertTrue(fileSize > 0, "file should not be empty but is " + fileSize + " bytes.");
    }

    public void testFileData() throws Exception {
        Product standardExomeSequencing = ProductTestFactory.createStandardExomeSequencing();
        ProductPdfFactory.toPdf(new FileOutputStream(tempFile), standardExomeSequencing);
        validatePdf(tempFile, standardExomeSequencing);
    }

    private void validatePdf(File pdfFile, Product product) throws IOException, ParseException {
        PdfReader reader = new PdfReader(pdfFile.getPath());
        // we can inspect the syntax of the imported page
        byte[] streamBytes = reader.getPageContent(1);
        PRTokeniser tokenizer = new PRTokeniser(new RandomAccessFileOrArray(streamBytes));
        List<String> pdfData = new ArrayList<>();
        while (tokenizer.nextToken()) {
            if (tokenizer.getTokenType() == PRTokeniser.TK_STRING) {
                pdfData.add(tokenizer.getStringValue());
            }
        }
        reader.close();

        Assert.assertEquals(nextString(pdfData), product.getProductFamily().getName().toUpperCase());
        Assert.assertEquals(nextString(pdfData), product.getProductName());
        Assert.assertEquals(nextString(pdfData), ProductPdfFactory.PART_NUMBER + ": " + product.getPartNumber());
        Assert.assertEquals(nextString(pdfData), ProductPdfFactory.PRODUCT_FAMILY + ": " + product.getProductFamily().getName());
        String[] dateParts = nextString(pdfData).split(":");
        Date expectedDate = DateUtils.convertStringToDate(dateParts[1]);
        Assert.assertEquals(expectedDate, product.getAvailabilityDate());
        Assert.assertEquals(nextString(pdfData), ProductPdfFactory.DESCRIPTION);
        String descriptionString = getMultiLine(pdfData, ProductPdfFactory.DELIVERABLES);
        Assert.assertEquals(descriptionString, product.getDescription());
        Assert.assertEquals(nextString(pdfData), ProductPdfFactory.DELIVERABLES);
        String deliverable = getMultiLine(pdfData, ProductPdfFactory.INPUT_REQUIREMENTS);
        Assert.assertEquals(deliverable, product.getDeliverables());
        Assert.assertEquals("Input Requirements", nextString(pdfData));
        String inputRequirements = getMultiLine(pdfData, null);
        Assert.assertEquals(inputRequirements, product.getInputRequirements());
    }

    public void testGetMultiLine() {
        List<String> testData = new ArrayList<>(Arrays.asList("a", "b", "c", ProductPdfFactory.DELIVERABLES, "e"));
        String result = getMultiLine(testData, ProductPdfFactory.DELIVERABLES);
        Assert.assertEquals(result, "a b c");
    }

    public void testGetMultiLineNullDelimiter() {
        List<String> testData = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        String result = getMultiLine(testData, null);
        Assert.assertEquals(result, "a b c d e");
    }

    private String getMultiLine(List<String> pdfData, String endingDelimiter) {
        String descriptionString = nextString(pdfData);
        if (!descriptionString.endsWith("-")) {
            descriptionString += " ";
        }
        while (!(descriptionString.matches(".*" + endingDelimiter + "\\s?$") || pdfData.isEmpty())) {
            descriptionString += nextString(pdfData);
            if (!descriptionString.endsWith("-")) {
                descriptionString += " ";
            }
        }
        if (StringUtils.isNotBlank(endingDelimiter)) {
            pdfData.add(0, endingDelimiter);
            descriptionString =
                    descriptionString.substring(0, descriptionString.length() - endingDelimiter.length() - 2);
        }
        return descriptionString.trim();
    }

    public void testNextString() {
        List<String> testData = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        String result = nextString(testData);
        Assert.assertEquals(result, "a");
    }


    private String nextString(List<String> pdfData) {
        String result = null;
        Iterator<String> iterator = pdfData.iterator();
        if (iterator.hasNext()) {
            result = iterator.next();
            iterator.remove();
        }
        return result;
    }
}
