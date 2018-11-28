package org.broadinstitute.gpinformatics.athena.control.dao.products;

import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
/**
 * Dao for {@link PriceItem}s.
 */
public class PriceItemDao extends GenericDao {


    /**
     * TODO This method is probably useless as it only serves up the PriceItems that have been attached to Products in
     * Mercury, not anything resembling the full list of PriceItems from the Quote Server.  It is only referenced from its
     * own test and a LedgerEntryDaoTest.
     *
     * TODO Delete.
     *
     * @return List of PriceItems.
     */
    public List<PriceItem> findAll() {
        return findList(PriceItem.class, PriceItem_.platform, PriceItem.PLATFORM_GENOMICS);
    }


    /**
     * Find a {@link PriceItem}s by its unique triplet.
     *
     * @param platform Non-null platform name.
     * @param categoryName Nullable category name.
     * @param priceItemName Non-null price item name.
     *
     * @return Matching PriceItem.
     */
    public PriceItem find(@Nonnull String platform, String categoryName, @Nonnull String priceItemName) {

        if (platform == null) {
            throw new NullPointerException("Null platform!");
        }

        if (priceItemName == null) {
            throw new NullPointerException("Null name!");
        }

        // Null category is okay.

        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<PriceItem> cq = cb.createQuery(PriceItem.class);
        List<Predicate> predicateList = new ArrayList<>();

        Root<PriceItem> priceItem = cq.from(PriceItem.class);

        predicateList.add(cb.equal(priceItem.get(PriceItem_.platform), platform));
        if (categoryName == null) {
            predicateList.add(cb.isNull(priceItem.get(PriceItem_.category)));
        } else {
            predicateList.add(cb.equal(priceItem.get(PriceItem_.category), categoryName));
        }
        predicateList.add(cb.equal(priceItem.get(PriceItem_.name), priceItemName));

        Predicate[] predicates = new Predicate[predicateList.size()];
        cq.where(predicateList.toArray(predicates));

        try {
            return getEntityManager().createQuery(cq).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
