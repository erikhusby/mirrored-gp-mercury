package org.broadinstitute.gpinformatics.athena.infrastructure.quote;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.mercury.infrastructure.ObjectMarshaller;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/27/12
 * Time: 4:53 PM
 */
public class QuotesCacheTestUtil {


    // For Test purposes only
    public QuotesCache getLocalQuotes(String localFileName) throws Exception {
        if (StringUtils.isBlank(localFileName)) {
            throw new NullPointerException("Quotes filename cannot be null");
        }

        Quotes quoteData = null;
        BufferedReader rdr = null;

        try {
            String line = "";
            QuotesCache cache = null;
            StringBuilder sb = new StringBuilder();

            InputStream inStream = this.getClass().getResourceAsStream(localFileName);
            rdr = new BufferedReader(new InputStreamReader(inStream));

            while ((line = rdr.readLine()) != null) {
                sb.append(line);
            }
            quoteData = ObjectMarshaller.unmarshall(Quotes.class, sb.toString());
        } catch (Exception ue) {
            // Do nothing - Just return the message from the exp in it's original format.
        } finally {
            if (rdr != null) rdr.close();
        }

        return new QuotesCache(quoteData);
    }


}
