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

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.ListItem;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class is responsible for creating a pdf representation of one or more Products.
 */
public class ProductPdfFactory {
    private static Log log = LogFactory.getLog(ProductPdfFactory.class);
    @Inject
    private ProductDao productDao = new ProductDao();
    public static final String DESCRIPTION = "Description";

    public static final String DELIVERABLES = "Deliverables";
    public static final String INPUT_REQUIREMENTS = "Input Requirements";
    public static final String PART_NUMBER = "Part Number";
    public static final String PRODUCT_FAMILY = "Product Family";
    public static final String AVAILABILITY = "Availability";

    public static final String BULLET = "\u2022";
    public static final String ASTERISK = "[\\*]";
    public static final String ASTERISK_LINE = ASTERISK + "[\\s+]?";
    public static final String LIST_DELIMITER = String.format("\\n(?=(\\*[\\s]?))", ASTERISK_LINE);
    public static final String BASE_FONT_FILE = "/fonts/Arial.ttf";

    private static final float DEFAULT_LEADING = 1.5f;

    /**
     * Write specified products to an output stream.
     *
     * @param outputStream to write to
     * @param products     which products to include in the pdf.
     *
     * @throws IOException
     * @throws DocumentException
     */
    public static void toPdf(OutputStream outputStream, Product... products) throws IOException, DocumentException {
        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        writer.setPdfVersion(PdfWriter.VERSION_1_7);
        document.open();

        Font familyFont = new Font(baseFont(), 18, Font.BOLD);
        Font titleFont = new Font(baseFont(), 14, Font.BOLD);
        FastDateFormat dateFormat = FastDateFormat.getInstance("MM/dd/yyyy");
        String productFamily = "";
        for (Product product : products) {
            if (!productFamily.equals(product.getProductFamily().getName())) {
                productFamily = product.getProductFamily().getName();
                document.add(new Paragraph(productFamily.toUpperCase(), familyFont));
            }
            document.add(new Paragraph(product.getProductName(), titleFont));
            document.add(new Paragraph(PART_NUMBER + ": " + product.getPartNumber(), regularFont()));
            document.add(new Paragraph(PRODUCT_FAMILY + ": " + productFamily, regularFont()));
            document.add(
                    new Paragraph(AVAILABILITY + ": " + dateFormat.format(product.getAvailabilityDate()), regularFont()));
            addParagraphWithHeader(document, DESCRIPTION, product.getDescription());
            addParagraphWithHeader(document, DELIVERABLES, product.getDeliverables());
            addParagraphWithHeader(document, INPUT_REQUIREMENTS, product.getInputRequirements());
            document.add(Chunk.NEWLINE);
        }
        document.addCreationDate();

        document.close();
    }

    static void addParagraphWithHeader(Document document, String headerText, String text)
            throws DocumentException, IOException {
        Paragraph sectionParagraph=new Paragraph();
        sectionParagraph.setKeepTogether(true);
        sectionParagraph.add(new Paragraph(relativeLeading(boldFont()), headerText, boldFont()));
        Paragraph paragraph = new Paragraph(relativeLeading(regularFont()));
        paragraph.setFont(regularFont());
        List<Element> descriptionElements = convertText(text, LIST_DELIMITER);
        for (Element description : descriptionElements) {
            paragraph.add(description);
        }
        sectionParagraph.add(paragraph);
        document.add(sectionParagraph);
    }

    private static float relativeLeading(Font font) {
        return font.getSize() * DEFAULT_LEADING;
    }

    /**
     * Converts "\n*" to an unordered list.
     */
    static List<Element> convertText(String description, String delimiter) throws IOException, DocumentException {
        List<Element> result = new ArrayList<>();
        if (StringUtils.isEmpty(description)) {
            return result;
        }
        List<String> splitText = Arrays.asList(description.split(delimiter));

        com.lowagie.text.List list = new com.lowagie.text.List(com.lowagie.text.List.UNORDERED, 10);

        list.setListSymbol(BULLET);
        for (String text : splitText) {
            text = text.trim();
            if (!text.isEmpty()) {
                if (text.matches("^" + ASTERISK_LINE + ".*")) {
                    list.add(new ListItem(text.replaceFirst(ASTERISK, "").trim(), regularFont()));
                } else {
                    result.add(new Chunk(text));
                }
            }
        }
        if (!list.isEmpty()) {
            result.add(list);
        }
        return result;
    }

    private static BaseFont baseFont() throws IOException, DocumentException {
        return BaseFont.createFont(BASE_FONT_FILE, BaseFont.CP1252, BaseFont.EMBEDDED, true);
    }

    static Font regularFont() throws IOException, DocumentException {
        return new Font(baseFont(), 10, Font.NORMAL);
    }

    static Font boldFont() throws IOException, DocumentException {
        return new Font(baseFont(), 11, Font.BOLD);
    }
}
