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
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This class is responsible for creating a pdf representation of one or more Products.
 */
public class ProductPdfFactory {
    @Inject
    private ProductDao productDao = new ProductDao();

    public static final String DESCRIPTION = "Description";
    public static final String DELIVERABLES = "Deliverables";
    public static final String INPUT_REQUIREMENTS = "Input Requirements";
    public static final String PART_NUMBER = "Part Number";
    public static final String PRODUCT_FAMILY = "Product Family";
    public static final String AVAILABILITY = "Availability";


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

        Font familyFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD);
        Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD);
        FastDateFormat dateFormat = FastDateFormat.getInstance("MM/dd/yyyy");
        String productFamily = "";
        for (Product product : products) {
            if (!productFamily.equals(product.getProductFamily().getName())) {
                productFamily = product.getProductFamily().getName();
                document.add(new Paragraph(productFamily.toUpperCase(), familyFont));
            }
            document.add(new Paragraph(product.getProductName(), titleFont));
            document.add(new Paragraph(PART_NUMBER + ": " + product.getPartNumber(), regularFont));
            document.add(new Paragraph(PRODUCT_FAMILY + ": " + productFamily, regularFont));
            document.add(
                    new Paragraph(AVAILABILITY + ": " + dateFormat.format(product.getAvailabilityDate()), regularFont));
            document.add(new Paragraph(DESCRIPTION, boldFont));
            document.add(new Paragraph(product.getDescription(), regularFont));
            document.add(new Paragraph(DELIVERABLES, boldFont));
            document.add(new Paragraph(product.getDeliverables(), regularFont));
            document.add(new Paragraph(INPUT_REQUIREMENTS, boldFont));
            document.add(new Paragraph(product.getInputRequirements(), regularFont));
            document.add(Chunk.NEWLINE);
        }
        document.addCreationDate();

        document.close();
    }
}
