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

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PRTokeniser;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.RandomAccessFileOrArray;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class contains static methods useful for testing ProductPdfFactory (and tests for the tests)
 */
public class ProductPdfFactoryUtils {
    protected static List<String> extractStringsFromPdf(File pdfFile) throws IOException {
        PdfReader reader = new PdfReader(pdfFile.getPath());
        // we can inspect the syntax of the imported page
        byte[] streamBytes = reader.getPageContent(1);
        PRTokeniser tokenizer = new PRTokeniser(new RandomAccessFileOrArray(streamBytes));
        List<String> pdfData = new CopyOnWriteArrayList<>();
        while (tokenizer.nextToken()) {
            if (tokenizer.getTokenType() == PRTokeniser.TK_STRING) {
                String stringValue = tokenizer.getStringValue().trim();
                if (!stringValue.matches(ProductPdfFactoryTest.LIST_MATCHER) && StringUtils.isNotBlank(stringValue)) {
                    pdfData.add(stringValue);
                }
            }
        }
        reader.close();
        return pdfData;
    }

    protected static void validateExtractParagraph(File pdfFile, String headerText, List<String> expectedStrings) throws Exception {
        List<String> pdfData = extractStringsFromPdf(pdfFile);
        Assert.assertEquals(nextString(pdfData), headerText);
        for (String expected : expectedStrings) {
            Assert.assertEquals(nextString(pdfData), expected);
        }
    }

    protected static String nextString(List<String> pdfData) {
        String result = TestUtils.getFirst(pdfData);
        pdfData.remove(result);
        return result;
    }

    protected static Product writeProduct(File tempFile, String testDescription) throws IOException, DocumentException {
        Product standardExomeSequencing = ProductTestFactory.createStandardExomeSequencing();
        standardExomeSequencing.setDescription(testDescription);
        ProductPdfFactory.toPdf(new FileOutputStream(tempFile), standardExomeSequencing);
        return standardExomeSequencing;
    }

    protected static String getMultiLine(List<String> pdfData, String endingDelimiter) {
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
            if (descriptionString.trim().equals(endingDelimiter)) {
                pdfData.add(0, endingDelimiter);
            } else {
                pdfData.add(0, endingDelimiter);
                descriptionString =
                        descriptionString.substring(0, descriptionString.length() - endingDelimiter.length() - 2);
            }
        }
        return descriptionString.trim();
    }

    @Test
    public void testGetMultiLine() {
        List<String> testData = new ArrayList<>(Arrays.asList("a", "b", "c", ProductPdfFactory.DELIVERABLES, "e"));
        String result = ProductPdfFactoryUtils.getMultiLine(testData, ProductPdfFactory.DELIVERABLES);
        Assert.assertEquals(result, "a b c");
    }

    @Test
    public void testGetMultiLineNullDelimiter() {
        List<String> testData = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        String result = ProductPdfFactoryUtils.getMultiLine(testData, null);
        Assert.assertEquals(result, "a b c d e");
    }

    @Test
    public void testNextString() {
        List<String> testData = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        String result = ProductPdfFactoryUtils.nextString(testData);
        Assert.assertEquals(result, "a");
    }

    protected static void validatePdf(File pdfFile, Product product) throws IOException, ParseException {
         List<String> pdfData = ProductPdfFactoryUtils.extractStringsFromPdf(pdfFile);

         Assert.assertEquals(nextString(pdfData), product.getProductFamily().getName().toUpperCase());
         Assert.assertEquals(nextString(pdfData), product.getProductName());
         Assert.assertEquals(nextString(pdfData), ProductPdfFactory.PART_NUMBER + ": " + product.getPartNumber());
         Assert.assertEquals(nextString(pdfData),
                 ProductPdfFactory.PRODUCT_FAMILY + ": " + product.getProductFamily().getName());
         String[] dateParts = nextString(pdfData).split(":");
         Date expectedDate = DateUtils.convertStringToDate(dateParts[1]);
         Assert.assertEquals(expectedDate, product.getAvailabilityDate());
         Assert.assertEquals(nextString(pdfData), ProductPdfFactory.DESCRIPTION);
         String descriptionString = getMultiLine(pdfData, ProductPdfFactory.DELIVERABLES);
         String testDescriptionString = product.getDescription().replaceAll("[\\*|\n]", "").trim();
         Assert.assertEquals(descriptionString, testDescriptionString);
         Assert.assertEquals(nextString(pdfData), ProductPdfFactory.DELIVERABLES);
         String deliverable = getMultiLine(pdfData, ProductPdfFactory.INPUT_REQUIREMENTS);
         Assert.assertEquals(deliverable, product.getDeliverables());
         Assert.assertEquals("Input Requirements", nextString(pdfData));
         String inputRequirements = getMultiLine(pdfData, null);
         Assert.assertTrue(inputRequirements.startsWith(product.getInputRequirements().replaceAll("^\\*|\n\\*", "")));
     }

}
