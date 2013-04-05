package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderCompletionStatus;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This is a utility class for setting up PDO completion status and retrieving all information.
 *
 * @author hrafal
 */
public class CompletionStatusFetcher {

    private Map<String, ProductOrderCompletionStatus> progressByBusinessKey =
            new HashMap<String, ProductOrderCompletionStatus>();


    public void loadProgress(ProductOrderDao productOrderDao, Collection<String> productOrderKeys) {
        progressByBusinessKey = productOrderDao.getProgressByBusinessKey(productOrderKeys);
    }

    public void loadProgress(ProductOrderDao productOrderDao) {
        progressByBusinessKey = productOrderDao.getAllProgressByBusinessKey();
    }

    @DaoFree
    public int getPercentAbandoned(String orderKey) {
        ProductOrderCompletionStatus counter = progressByBusinessKey.get(orderKey);
        if (counter == null) {
            return 0;
        }

        return counter.getPercentAbandoned();
    }

    @DaoFree
    public int getPercentCompleted(String orderKey) {
        ProductOrderCompletionStatus counter = progressByBusinessKey.get(orderKey);
        if (counter == null) {
            return 0;
        }

        return counter.getPercentCompleted();
    }

    @DaoFree
    public int getPercentInProgress(String orderKey) {
        ProductOrderCompletionStatus counter = progressByBusinessKey.get(orderKey);
        if (counter == null) {
            return 0;
        }

        return counter.getPercentInProgress();
    }

    @DaoFree
    public int getNumberOfSamples(String orderKey) {
        ProductOrderCompletionStatus counter = progressByBusinessKey.get(orderKey);
        if (counter == null) {
            return 0;
        }

        return counter.getTotal();
    }

    @DaoFree
    public Set<String> getKeys() {
        return progressByBusinessKey.keySet();
    }
}

