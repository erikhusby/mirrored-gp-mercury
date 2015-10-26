package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.apache.commons.collections4.map.DefaultedMap;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderCompletionStatus;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Holds sample status counts for multiple PDOs. Provides convenience methods for retrieving counts and percentages for
 * individual PDOs. Returns zero rather than null when status counts for the PDO are not known by this holder.
 *
 * TODO: rename to remove the word "fetcher"
 */
public class CompletionStatusFetcher {

    private static final ProductOrderCompletionStatus DEFAULT = new ProductOrderCompletionStatus(0, 0, 0);

    @SuppressWarnings("unchecked")
    private Map<String, ProductOrderCompletionStatus> progressByBusinessKey = new DefaultedMap(DEFAULT);

    /**
     * Create a status object containing the sample status counts for a set of PDOs. This is designed to hold results
     * from a call to {@link ProductOrderDao#getProgress(Collection)} in order to provide some extra convenience methods
     * for accessing the data.
     *
     * @param progressByBusinessKey
     */
    public CompletionStatusFetcher(Map<String, ProductOrderCompletionStatus> progressByBusinessKey) {
        this.progressByBusinessKey = DefaultedMap.defaultedMap(progressByBusinessKey, DEFAULT);
    }

    @DaoFree
    public double getPercentAbandoned(String orderKey) {
        return progressByBusinessKey.get(orderKey).getPercentAbandoned();
    }

    @DaoFree
    public int getNumberAbandoned(String orderKey) {
        return progressByBusinessKey.get(orderKey).getNumberAbandoned();
    }

    @DaoFree
    public double getPercentCompleted(String orderKey) {
        return progressByBusinessKey.get(orderKey).getPercentCompleted();
    }

    @DaoFree
    public int getNumberCompleted(String orderKey) {
        return progressByBusinessKey.get(orderKey).getNumberCompleted();
    }

    @DaoFree
    public double getPercentInProgress(String orderKey) {
        return progressByBusinessKey.get(orderKey).getPercentInProgress();
    }

    @DaoFree
    public int getNumberInProgress(String orderKey) {
        return progressByBusinessKey.get(orderKey).getNumberInProgress();
    }

    @DaoFree
    public int getNumberOfSamples(String orderKey) {
        return progressByBusinessKey.get(orderKey).getTotal();
    }

    @DaoFree
    public Set<String> getKeys() {
        return progressByBusinessKey.keySet();
    }

    @DaoFree
    public ProductOrderCompletionStatus getStatus(String orderKey) {
        return progressByBusinessKey.get(orderKey);
    }
}
