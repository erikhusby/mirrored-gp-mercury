/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2020 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.jmx.AbstractCacheControl;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Singleton
@Startup
public class DevQuotesCache extends AbstractCacheControl implements Serializable {
    private static final long serialVersionUID = 4310647434148494492L;
    private static final Log log = LogFactory.getLog(DevQuotesCache.class);
    private int maximumCacheSize = 100000;

    private QuoteService quoteService;
    private Map<String, Quote> quoteMap = null;

    public DevQuotesCache() {
    }

    @Inject
    public DevQuotesCache(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @Override
    @Schedule(minute = "30", hour = "4", persistent = false)
    public void invalidateCache() {
        try {
            quoteMap=null;
            refreshCache();
        }
        catch(Exception e) {
            log.error("Could not refresh cache " + getClass().getName(), e);
        }

    }

    @Override
    public int getMaximumCacheSize() {
        return maximumCacheSize;
    }

    @Override
    public void setMaximumCacheSize(int maximumCacheSize) {
        this.maximumCacheSize = maximumCacheSize;
    }

    public synchronized void refreshCache() {
        QuoteService.DEV_QUOTES.forEach(quoteId -> {
            Quote quote = null;
            try {
                quote = quoteService.getQuoteByAlphaId(quoteId, true);
                if (quote != null) {
                    if (quoteMap == null) {
                        quoteMap = new HashMap<>();
                    }

                    quoteMap.put(quote.getAlphanumericId(), quote);
                }
            } catch (QuoteServerException | QuoteNotFoundException ex) {
                log.error(String.format("Could not refresh %s.", QuoteService.DEV_QUOTES), ex);
            }
        });
    }

    public Quote getQuote(String quoteId) {
        if (quoteMap == null) {
            refreshCache();
        }
        return quoteMap.get(quoteId);
    }
}
