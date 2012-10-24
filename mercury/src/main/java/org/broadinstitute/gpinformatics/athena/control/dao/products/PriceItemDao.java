package org.broadinstitute.gpinformatics.athena.control.dao.products;

import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

import static org.broadinstitute.gpinformatics.athena.entity.products.PriceItem.Platform.GP;

@Stateful
@RequestScoped
/**
 * Dao for {@link PriceItem}s
 */
public class PriceItemDao extends GenericDao {


    /**
     * The Product Details CRUD UI will want to enumerate all {@link PriceItem}s or at least all "eligible" {@link PriceItem}s for
     * attachment to {@link org.broadinstitute.gpinformatics.athena.entity.products.Product}s, where eligibility may be
     * determined by platform = 'GP' or some other criteria.  I am currently assuming that we have cached copies of
     * these {@link PriceItem}s in our DB and this is the source of the {@link PriceItem}s data that feeds the CRUD UI.
     *
     * @return
     */
    public List<PriceItem> findAll() {
        return findList(PriceItem.class, PriceItem_.platform, GP.getQuoteServerPlatform());
    }


    /**
     * Find method using strongly typed parameters.
     *
     * @param platform
     * @param category
     * @param name
     * @return
     */
    public PriceItem find(PriceItem.Platform platform, PriceItem.Category category, PriceItem.Name name) {

        if (platform == null) {
            throw new NullPointerException("Null platform!");
        }

        // it's possible category might be null

        if (name == null) {
            throw new NullPointerException("Null PriceItem name!");
        }

        return find(
                platform.getQuoteServerPlatform(),
                category != null ? category.getQuoteServerCategory() : null,
                name.getQuoteServerName());

    }


    /**
     * Find a {@link PriceItem}s by its unique triplet
     *
     * @param platform
     * @param categoryName
     * @param priceItemName
     * @return
     */
    public PriceItem find(String platform, String categoryName, String priceItemName) {

        if (platform == null) {
            throw new NullPointerException("Null platform!");
        }

        if (priceItemName == null) {
            throw new NullPointerException("Null name!");
        }

        // null category may be okay

        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<PriceItem> cq = cb.createQuery(PriceItem.class);
        List<Predicate> predicateList = new ArrayList<Predicate>();

        Root<PriceItem> priceItem = cq.from(PriceItem.class);

        predicateList.add(cb.equal(priceItem.get(PriceItem_.platform), platform));
        predicateList.add(cb.equal(priceItem.get(PriceItem_.category), categoryName));
        predicateList.add(cb.equal(priceItem.get(PriceItem_.name), priceItemName));

        Predicate[] predicates = new Predicate[predicateList.size()];
        cq.where(predicateList.toArray(predicates));

        return getEntityManager().createQuery(cq).getSingleResult();

    }
}
