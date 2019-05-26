package org.broadinstitute.gpinformatics.mercury.control.dao.infrastructure;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.infrastructure.Quarantined;
import org.broadinstitute.gpinformatics.mercury.entity.infrastructure.Quarantined_;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Arrays;
import java.util.List;

@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class QuarantinedDao extends GenericDao {
    public static final List<String> RACK_REASONS = Arrays.asList(
            "Unreadable barcode"
    );
    public static final List<String> PACKAGE_REASONS = Arrays.asList(
            "Damaged"
    );
    // Additional rack quarantine reason, not user selectable.
    public static final String MISMATCH = "Wrong tube or position";
    // Additional package quarantine reason, not user selectable.
    public static final String MISSING_MANIFEST = "Missing manifest";

    public List<Quarantined> findItems(Quarantined.ItemSource itemSource, Quarantined.ItemType itemType) {
        return findAll(Quarantined.class, (cq, root) -> cq.where(getCriteriaBuilder().and(
                getCriteriaBuilder().equal(root.get(Quarantined_.itemSource), itemSource),
                getCriteriaBuilder().equal(root.get(Quarantined_.itemType), itemType))));
    }

    public Quarantined findItem(Quarantined.ItemSource itemSource, Quarantined.ItemType itemType, String item) {
        return findSingle(Quarantined.class, (cq, root) -> cq.where(getCriteriaBuilder().and(
                getCriteriaBuilder().equal(root.get(Quarantined_.itemSource), itemSource),
                getCriteriaBuilder().equal(root.get(Quarantined_.itemType), itemType),
                getCriteriaBuilder().equal(root.get(Quarantined_.item), item))));
    }

    public Quarantined addOrUpdate(Quarantined.ItemSource itemSource, Quarantined.ItemType itemType, String item,
            String reason) {
        Quarantined quarantined = findItem(itemSource, itemType, item);
        if (quarantined == null) {
            quarantined = new Quarantined(itemSource, itemType, item, reason);
            persist(quarantined);
        } else {
            quarantined.setReason(reason);
        }
        return quarantined;
    }

    public void unQuarantine(Quarantined.ItemSource itemSource, Quarantined.ItemType itemType, String item) {
        CriteriaDelete<Quarantined> criteriaDelete = getCriteriaBuilder().createCriteriaDelete(Quarantined.class);
        Root<Quarantined> root = criteriaDelete.from(Quarantined.class);
        criteriaDelete.where(getCriteriaBuilder().and(
                getCriteriaBuilder().equal(root.get(Quarantined_.itemSource), itemSource),
                getCriteriaBuilder().equal(root.get(Quarantined_.itemType), itemType),
                getCriteriaBuilder().equal(root.get(Quarantined_.item), item)));
        getEntityManager().createQuery(criteriaDelete).executeUpdate();
    }
}
