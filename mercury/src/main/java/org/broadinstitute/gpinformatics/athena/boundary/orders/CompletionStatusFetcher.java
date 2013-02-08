package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderCompletionStatus;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a utility class for setting up PDO completion status and retrieving all informaiton
 *
 * @author hrafal
 */
public class CompletionStatusFetcher {

    private Map<String, ProductOrderCompletionStatus> progressByBusinessKey =
            new HashMap<String, ProductOrderCompletionStatus>();


    public void setupProgress(ProductOrderDao productOrderDao, Collection<String> productKeys) {
        progressByBusinessKey = productOrderDao.getProgressByBusinessKey(productKeys);
    }

    public void setupProgress(ProductOrderDao productOrderDao) {
        setupProgress(productOrderDao, null);
    }

    public int getPercentAbandoned(String orderKey) {
        ProductOrderCompletionStatus counter = progressByBusinessKey.get(orderKey);
        if (counter == null) {
            return 0;
        }

        return counter.getPercentAbandoned();
    }

    public int getPercentComplete(String orderKey) {
        ProductOrderCompletionStatus counter = progressByBusinessKey.get(orderKey);
        if (counter == null) {
            return 0;
        }

        return counter.getPercentComplete();
    }

    public int getPercentCompleteAndAbandoned(String orderKey) {
        return getPercentAbandoned(orderKey) + getPercentComplete(orderKey);
    }

    public int getNumberOfSamples(String orderKey) {
        ProductOrderCompletionStatus counter = progressByBusinessKey.get(orderKey);
        if (counter == null) {
            return 0;
        }

        return counter.getTotal();
    }
}

