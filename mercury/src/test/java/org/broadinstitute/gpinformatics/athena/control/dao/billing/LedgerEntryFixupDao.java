package org.broadinstitute.gpinformatics.athena.control.dao.billing;

import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession_;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.hibernate.SQLQuery;
import org.hibernate.type.StandardBasicTypes;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.util.List;

/**
 * This is the holder of ledger entry dao methods that are just for fixup tests, so we do not want to muck
 * up the code with potential things that need to covered and are not because the fixups are off.
 *
 * @author hrafal
 */
@Stateful
@RequestScoped
public class LedgerEntryFixupDao extends GenericDao {

    /**
     * Fetch the billed LedgerEntries with null quote IDs, join fetching their associated {@link org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample}s
     * for performance purposes (i.e. avoiding singleton-selects of what are probably nearly unique
     * {@link javax.persistence.ManyToOne} ProductOrderSamples).
     *
     * @return List of billed Ledger Entries with null quote IDs.
     */
    public List<LedgerEntry> findSuccessfullyBilledLedgerEntriesWithoutQuoteId() {
        return findAll(LedgerEntry.class, new GenericDaoCallback<LedgerEntry>() {
            @Override
            public void callback(CriteriaQuery<LedgerEntry> criteriaQuery, Root<LedgerEntry> root) {

                CriteriaBuilder cb = getCriteriaBuilder();

//                Join<LedgerEntry, BillingSession> ledgerBillingSessionJoin = root.join(LedgerEntry_.billingSession);

                criteriaQuery.where(
                        // Filter out ledgers that already have a quote assigned.
                        cb.isNull(root.get(LedgerEntry_.quoteId)),
                        // A predicate for billed ledgers only as we want unbilled ledgers to keep their null quote IDs
                        // until billing.  The predicate for this purpose is that the ledgers' billing session must
                        // exist (this is an inner join) and have a non-null billed date.
//                        cb.isNotNull(ledgerBillingSessionJoin.get(BillingSession_.billedDate)),
                        // Only select ledger entries that were successfully billed.
                        cb.equal(root.get(LedgerEntry_.billingMessage), BillingSession.SUCCESS));

                // This runs very slowly without this fetch as we would singleton select thousands of ProductOrderSamples.
                root.fetch(LedgerEntry_.productOrderSample);
            }
        });
    }

    /**
     * Fetch the ledger entries that we know have a session id that does not have a session
     *
     * @return List of billed Ledger Entries with null quote IDs.
     */
    @SuppressWarnings("unchecked")
    public List<Long> getEntriesWithOrphanedSession(final long sessionId) {
        String queryString = String.format("select LEDGER_ID as id from athena.BILLING_LEDGER where BILLING_SESSION=%d", sessionId);
        Query query = getEntityManager().createNativeQuery(queryString);
        query.unwrap(SQLQuery.class).addScalar("id", StandardBasicTypes.LONG);
        return query.getResultList();
    }

    /**
     * Fetch the billed LedgerEntries so we can populate any new data.
     *
     * @return List of entries that were billed to the quote server
     */
    public List<LedgerEntry> findAllBilledEntries() {
        return findAll(LedgerEntry.class, new GenericDaoCallback<LedgerEntry>() {
            @Override
            public void callback(CriteriaQuery<LedgerEntry> criteriaQuery, Root<LedgerEntry> root) {

                CriteriaBuilder cb = getCriteriaBuilder();

//                Join<LedgerEntry, BillingSession> ledgerBillingSessionJoin = root.join(LedgerEntry_.billingSession);

                // If it has a session and there is a quote, then this was billed to the quote server
                criteriaQuery.where(
                        cb.isNotNull(root.get(LedgerEntry_.quoteId)));

                // This runs very slowly without this fetch as we would singleton select thousands of ProductOrderSamples.
                root.fetch(LedgerEntry_.productOrderSample);
            }
        });
    }

    /**
     * Find the two (!) LedgerEntry instances that have been billed but do not have a "Billed successfully" message.
     *
     * @return the billed ledger entries missing a billing message
     */
    public List<LedgerEntry> findBilledWithoutBillingMessage() {
        return findAll(LedgerEntry.class, new GenericDao.GenericDaoCallback<LedgerEntry>() {
            @Override
            public void callback(CriteriaQuery<LedgerEntry> criteriaQuery, Root<LedgerEntry> root) {
                CriteriaBuilder cb = getCriteriaBuilder();

                Join<LedgerEntry, BillingSession> billingSessionJoin = root.join(LedgerEntry_.billingSession);

                criteriaQuery.where(
                        cb.isNotNull(billingSessionJoin.get(BillingSession_.billedDate)),
                        cb.isNull(root.get(LedgerEntry_.billingMessage))
                );
            }
        });
    }
}
