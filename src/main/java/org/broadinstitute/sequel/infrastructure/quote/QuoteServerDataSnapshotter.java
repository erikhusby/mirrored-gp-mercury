package org.broadinstitute.sequel.infrastructure.quote;

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

    public static final File PRICE_LIST_FILE = new File("src/test/data/quote/PriceList.xml");

    public static final File QUOTES_FILE = new File("src/test/data/quote/Quotes.xml");

    /**
     * Saves the {@link PriceList} generated from
     * the given service into #PRICE_LIST_FILE
     * @param service
     * @throws Exception
     */
    public static void saveAllPriceItemsToTestFile(QuoteService service) throws Exception {
        PriceList priceList = service.getAllPriceItems();
        JAXBContext jaxbContext = JAXBContext.newInstance(PriceList.class);

        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );

        if (!PRICE_LIST_FILE.exists()) {
            if (!PRICE_LIST_FILE.createNewFile()) {
                throw new RuntimeException("Could not create price list file " + PRICE_LIST_FILE.getAbsolutePath());
            }
        }
        OutputStream priceListOutputStream = new FileOutputStream(PRICE_LIST_FILE);
        marshaller.marshal(priceList,priceListOutputStream);
        priceListOutputStream.flush();
        priceListOutputStream.close();
    }

    /**
     * Reads all the {@link Quote} data from {@link #QUOTES_FILE}
     * from local disk and returns it
     * @return
     * @throws JAXBException
     * @throws IOException
     */
    public static Quotes readAllQuotesFromTestFile() throws JAXBException,IOException {
        return (Quotes)JAXBContext.newInstance(Quotes.class).createUnmarshaller().unmarshal(new FileInputStream(QUOTES_FILE));
    }

    /**
     * Reads all the {@link PriceList} data from {@link #PRICE_LIST_FILE}
     * from local disk and returns it.
     * @return
     * @throws JAXBException
     * @throws IOException
     */
    public static PriceList readPriceListFromTestFile()  throws JAXBException,IOException {
        return (PriceList)JAXBContext.newInstance(PriceList.class).createUnmarshaller().unmarshal(new FileInputStream(PRICE_LIST_FILE));
    }

    /**
     * Saves all the {@link Quote} objects from the given
     * service and saves it to local disk at {@link #QUOTES_FILE}
     * @param service
     * @throws Exception
     */
    public static void saveAllQuotesToTestFile(QuoteService service) throws Exception {
        Quotes quotes = service.getAllSequencingPlatformQuotes();
        JAXBContext jaxbContext = JAXBContext.newInstance(Quotes.class);

        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );

        if (!QUOTES_FILE.exists()) {
            if (!QUOTES_FILE.createNewFile()) {
                throw new RuntimeException("Could not quotes file " + QUOTES_FILE.getAbsolutePath());
            }
        }
        OutputStream quotesOutputStream = new FileOutputStream(QUOTES_FILE);
        marshaller.marshal(quotes,quotesOutputStream);
        quotesOutputStream.flush();
        quotesOutputStream.close();
    }
}
