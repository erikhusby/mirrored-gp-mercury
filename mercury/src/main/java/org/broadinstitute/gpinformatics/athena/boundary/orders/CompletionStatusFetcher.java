package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.apache.commons.collections.map.DefaultedMap;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderCompletionStatus;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * This is a utility class for setting up PDO completion status and retrieving all information.
 *
 * @author hrafal
 */
public class CompletionStatusFetcher {

    private static final ProductOrderCompletionStatus DEFAULT = new ProductOrderCompletionStatus(0, 0, 0);

    @SuppressWarnings("unchecked")
    private Map<String, ProductOrderCompletionStatus> progressByBusinessKey = new DefaultedMap(DEFAULT);


    @SuppressWarnings("unchecked")
    public void loadProgress(ProductOrderDao productOrderDao, Collection<Long> productOrderIds) {
        progressByBusinessKey = DefaultedMap.decorate(productOrderDao.getProgressByOrderId(productOrderIds), DEFAULT);
    }

    @SuppressWarnings("unchecked")
    public void loadProgress(ProductOrderDao productOrderDao) {
        progressByBusinessKey = DefaultedMap.decorate(productOrderDao.getAllProgressByBusinessKey(), DEFAULT);
    }

    @DaoFree
    public int getPercentAbandoned(String orderKey) {
        return progressByBusinessKey.get(orderKey).getPercentAbandoned();
    }

    @DaoFree
    public int getPercentCompleted(String orderKey) {
        return progressByBusinessKey.get(orderKey).getPercentCompleted();
    }

    @DaoFree
    public int getPercentInProgress(String orderKey) {
        return progressByBusinessKey.get(orderKey).getPercentInProgress();
    }

    @DaoFree
    public int getNumberOfSamples(String orderKey) {
        return progressByBusinessKey.get(orderKey).getTotal();
    }

    @DaoFree
    public Set<String> getKeys() {
        return progressByBusinessKey.keySet();
    }
}

