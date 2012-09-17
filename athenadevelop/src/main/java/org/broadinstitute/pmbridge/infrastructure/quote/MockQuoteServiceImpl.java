package org.broadinstitute.pmbridge.infrastructure.quote;

import javax.enterprise.inject.Default;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/17/12
 * Time: 11:26 AM
 */
@Default
public class MockQuoteServiceImpl extends QuoteServiceImpl {

    public static final String QUOTE_TEST_DATA_XML = "src/test/data/quote/quoteTestData.xml";
    public static final String PRICE_TEST_DATA_XML = "src/test/data/quote/priceTestData.xml";
    Quotes quotes;
    PriceList prices;

    public MockQuoteServiceImpl() {
        super();
        try {
            quotes = (Quotes) JAXBContext.newInstance(Quotes.class).createUnmarshaller().unmarshal(
                    new FileInputStream(QUOTE_TEST_DATA_XML) );
        } catch (Exception e) {
            throw new RuntimeException("Could not read quotes from files " + QUOTE_TEST_DATA_XML );
        }

        try {
            prices = (PriceList) JAXBContext.newInstance(PriceList.class).createUnmarshaller().unmarshal(
                    new FileInputStream(PRICE_TEST_DATA_XML) );
        } catch (Exception e) {
            throw new RuntimeException("Could not read priceItems from files " + PRICE_TEST_DATA_XML );
        }
    }

    @Override
    public Quote getQuoteByAlphaId(final String alphaId) throws QuoteServerException, QuoteNotFoundException {
        return super.getQuoteByAlphaId(alphaId);
    }


    @Override
    public Quote getQuoteByNumericId(final String numericId) throws QuoteServerException, QuoteNotFoundException {
       return super.getQuoteByNumericId(numericId);
    }

    /**
     * Mocked method to get the quote from the mocked quotes.
     * @param id
     * @param queryUrl
     * @return
     * @throws QuoteNotFoundException
     * @throws QuoteServerException
     */
    @Override
    protected Quote getSingleQuoteById(final String id, String queryUrl) throws QuoteNotFoundException, QuoteServerException {
        String numericRegex = "[\\d]+";
        boolean isNumeric = Pattern.matches(numericRegex, id);

        Quote result=null;
        for ( Quote quote :  this.quotes.getQuotes() ) {
            if ( isNumeric )  {
                if (quote.getId().equalsIgnoreCase( id )) {
                    result = quote;
                }
            } else {
                if (quote.getAlphanumericId().equalsIgnoreCase(id)) {
                    result = quote;
                }
            }
        }
        if ( result == null) {
            throw new QuoteNotFoundException("Could not find quote with Id : " + id );
        }
        return result;
    }

    @Override
    public Set<Funding> getAllFundingSources() throws QuoteServerException, QuoteNotFoundException {
        return super.getAllFundingSources();
     }

    /**
     * Mocked method to get the mocked quotes.
     */
    @Override
    public Quotes getAllQuotes() throws QuoteServerException, QuoteNotFoundException {
        return quotes;
    }

    @Override
    public Set<Quote> getQuotesInFundingSource(final Funding fundingSource) throws QuoteServerException, QuoteNotFoundException {
        return super.getQuotesInFundingSource(fundingSource);
     }

    @Override
    public PriceList getPlatformPriceItems(final QuotePlatformType quotePlatformType) throws QuoteServerException, QuoteNotFoundException {
        return super.getPlatformPriceItems(quotePlatformType);
    }

    /**
     * Mocked method to get the mocked priceItems.
     */
    @Override
    protected PriceList getAllPriceItems() throws QuoteServerException, QuoteNotFoundException {
        return prices;
    }


    public void generateQuoteFile( String quoteTestFileName ) throws QuoteServerException, QuoteNotFoundException, JAXBException, IOException {
        // Needed to recreate the data files in case quote API changes/evolves.
        // Not completed and tested yet.
        Quotes quotes = super.getAllQuotes();
        JAXBContext jaxbContext = JAXBContext.newInstance(Quotes.class);

        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
        File newQuoteFile = new File(quoteTestFileName);

        if (!newQuoteFile.exists()) {
            if (!newQuoteFile.createNewFile()) {
                throw new RuntimeException("Could not quotes file " + newQuoteFile.getAbsolutePath());
            }
        }
        OutputStream quotesOutputStream = new FileOutputStream(newQuoteFile);
        marshaller.marshal(quotes,quotesOutputStream);
        quotesOutputStream.flush();
        quotesOutputStream.close();

    }
}

