package org.broadinstitute.gpinformatics.infrastructure.quote;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;

/**
 * Utility class that writes out XML data
 * from quote server.  The resulting files
 * should be used to drive mock implementions
 * of {@link QuoteService}.
 */
public class QuoteServerDataSnapshotter {

    public static final String PRICE_LIST_FILE_PATH = "PriceList.xml";

    public static final String QUOTES_FILE_PATH = "quoteTestData.xml";


    public static InputStream getTestResource(String fileName) {
        InputStream testSpreadSheetInputStream = getResourceAsStream(fileName);
        if (testSpreadSheetInputStream == null) {
            testSpreadSheetInputStream = getResourceAsStream("testdata/" + fileName);
        }
        return testSpreadSheetInputStream;
    }

    public static InputStream getResourceAsStream(String fileName) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
    }

    public static InputStream getQuotesFile() {
          return getTestResource(QUOTES_FILE_PATH);
    }
    public static InputStream getPriceListFile() {
          return getTestResource(PRICE_LIST_FILE_PATH);
    }

    /**
     * Reads all the {@link Quote} data from {@link #QUOTES_FILE_PATH}
     * from local disk and returns it
     * @return
     * @throws JAXBException
     * @throws IOException
     */
    public static Quotes readAllQuotesFromTestFile() throws JAXBException,IOException {
        return (Quotes)JAXBContext.newInstance(Quotes.class).createUnmarshaller().unmarshal(getQuotesFile());
    }

    /**
     * Reads all the {@link PriceList} data from {@link #PRICE_LIST_FILE_PATH}
     * from local disk and returns it.
     * @return
     * @throws JAXBException
     * @throws IOException
     */
    public static PriceList readPriceListFromTestFile()  throws JAXBException,IOException {
        return (PriceList)JAXBContext.newInstance(PriceList.class).createUnmarshaller().unmarshal(getPriceListFile());
    }

}
