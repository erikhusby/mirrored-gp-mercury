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

import javax.annotation.Nullable;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The purpose of this class is to cache QuoteServer Quotes which:
 * <ol>
 *     <li>Are large and therefore cause timeouts in Mercury and put undo burden on the Quote Server</li>
 *     <li>Are always valid, such as DEV quotes such as GP87U, CRSPEVR or GPSPGR7</li>
 *     <li>Are defined in QuoteService.DEV_QUOTES</li>
 * </ol>
 * Therefore, to add a quote to the cache it must be added QuoteService.DEV_QUOTES
 * <p></p>The quotes cache is updated once daily, or when getQuote(quoteId) is called before when the cache hasn't been
 * initialized.</p>
 *
 * @see QuoteService#DEV_QUOTES
 */
@Singleton
@Startup
public class QuotesCache extends AbstractCacheControl implements Serializable {
    private static final long serialVersionUID = 4310647434148494492L;
    private static final Log log = LogFactory.getLog(QuotesCache.class);
    private int maximumCacheSize = 100000;

    private QuoteService quoteService;

    private Map<String, Quote> quoteMap = null;

    public QuotesCache() {
    }

    @Inject
    public QuotesCache(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @Override
    @Schedule(minute = "30", hour = "1", persistent = false)
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

//    @Lock(LockType.WRITE)
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

    @Nullable
    public Quote getQuote(String quoteId) {
        if (quoteMap == null) {
            refreshCache();
        }
        return quoteMap.get(quoteId);
    }

    public void setQuoteService(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

}
