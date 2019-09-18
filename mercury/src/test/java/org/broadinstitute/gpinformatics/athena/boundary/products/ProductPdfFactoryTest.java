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

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.ListItem;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.io.FileUtils;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductPdfFactoryTest {
    public static final String LIST_MATCHER = "[-|•|\\*|\u0095|·]";
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
//            The next lines are handy when debugging
//            if (tempFile.length()>0) {
//                FileUtils.copyFile(tempFile, new File("/tmp/test.pdf"));
//            }
            tempFile.delete();
        }
    }

    public void testLoadRegularFont() throws IOException, DocumentException {
        Font font = ProductPdfFactory.regularFont();
        Assert.assertEquals(font.getStyle(), Font.NORMAL);
    }
    public void testLoadBoldFont() throws IOException, DocumentException {
        Font font = ProductPdfFactory.boldFont();
        Assert.assertEquals(font.getStyle(), Font.BOLD);
    }

    public void testTempFile() {
        long fileSize = FileUtils.sizeOf(tempFile);
        Assert.assertEquals(fileSize, 0, "file should be empty but is " + fileSize + " bytes.");
    }

    public void testFileWritten() throws Exception {
        Product dummyProduct = ProductTestFactory
                .createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, ProductTestFactory.generateProductPartNumber());
        ProductPdfFactory.toPdf(new FileOutputStream(tempFile), ProductTestFactory.createStandardExomeSequencing(),
                dummyProduct);
        long fileSize = FileUtils.sizeOf(tempFile);
        Assert.assertTrue(fileSize > 0, "file should not be empty but is " + fileSize + " bytes.");
    }

    public void testFileData() throws Exception {
        Product standardExomeSequencing = ProductTestFactory.createStandardExomeSequencing();
        ProductPdfFactory.toPdf(new FileOutputStream(tempFile), standardExomeSequencing);
        ProductPdfFactoryUtils.validatePdf(tempFile, standardExomeSequencing);
    }


    public void testSplitTextIntoListElementsBasic() throws Exception {
        String testDescription = "some text.";
        Product standardExomeSequencing = ProductPdfFactoryUtils.writeProduct(tempFile, testDescription);
        ProductPdfFactoryUtils.validatePdf(tempFile, standardExomeSequencing);
    }

    public void testSplitTextIntoListElementsAllBullets() throws Exception {
        String testDescription = "* some text.\n* Some more.";
        Product standardExomeSequencing = ProductPdfFactoryUtils.writeProduct(tempFile, testDescription);
        ProductPdfFactoryUtils.validatePdf(tempFile, standardExomeSequencing);
    }

    public void testAddParagraph() throws Exception {
        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(tempFile));
        writer.setPdfVersion(PdfWriter.VERSION_1_7);
        document.open();

        String headerText = "IMA HEADER";
        String someText = "some text";
        String isAList = "is list";
        String listAndNonList = "also is list. *but I am not*";
        String paragraphText = String.format("%s\n*%s\n*%s", someText, isAList, listAndNonList);
        ProductPdfFactory.addParagraphWithHeader(document, headerText, paragraphText);
        document.close();
        ProductPdfFactoryUtils
                .validateExtractParagraph(tempFile, headerText, Arrays.asList(someText, isAList, listAndNonList));

    }

    public void testConvertTextFirstLineWithAsterisk() throws IOException, DocumentException {
        String description = "* line";
        List<Element> elements = ProductPdfFactory.convertText(description, ProductPdfFactory.LIST_DELIMITER);
        Assert.assertEquals(elements.size(), 1);
        Assert.assertEquals(elements.get(0).type(), Element.LIST);
    }

    public void testConvertTextSecondLineWithAsterisk() throws IOException, DocumentException {
        String description = "foo\n* line";
        List<Element> elements = ProductPdfFactory.convertText(description, ProductPdfFactory.LIST_DELIMITER);
        Assert.assertEquals(elements.size(), 2);
        Element chunk = elements.get(0);
        Assert.assertEquals(chunk.type(), Element.CHUNK);
        Element list = elements.get(1);
        Assert.assertEquals(list.type(), Element.LIST);
        Assert.assertEquals(list.getChunks().size(), 1);
        Element elementListItem = ((com.itextpdf.text.List) list).getItems().get(0);
        Assert.assertEquals(elementListItem.type(), Element.LISTITEM);
        Assert.assertTrue(elementListItem.isContent());
        Assert.assertEquals(((ListItem)elementListItem).getContent(), "line");
    }

    public void testConvertTextSecondLineWithNonListAsterisks() throws IOException, DocumentException {
        String someText = "some text";
        String isAList = "is list";
        String listAndNonList = "also is list. *but I am not*";
        String description = String.format("%s\n*%s\n*%s", someText, isAList, listAndNonList);
        List<Element> elements = ProductPdfFactory.convertText(description, ProductPdfFactory.LIST_DELIMITER);
        Assert.assertEquals(elements.size(), 2);
        Chunk chunk = (Chunk) elements.get(0);
        Assert.assertEquals(chunk.type(), Element.CHUNK);
        Assert.assertEquals(chunk.getContent(), someText);
        com.itextpdf.text.List list = (com.itextpdf.text.List) elements.get(1);
        Assert.assertEquals(list.type(), Element.LIST);
        Assert.assertEquals(list.getItems().size(), 2);
        Assert.assertTrue(list.getItems().get(0).isContent());
        Assert.assertEquals(((ListItem) list.getItems().get(0)).getContent(), isAList);
        Assert.assertTrue(list.getItems().get(1).isContent());
        Assert.assertEquals(((ListItem)list.getItems().get(1)).getContent(), listAndNonList);
    }
}
