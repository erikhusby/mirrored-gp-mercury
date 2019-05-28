package org.broadinstitute.gpinformatics.mercury.control.dao.infrastructure;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.infrastructure.Quarantined;
import org.broadinstitute.gpinformatics.mercury.entity.infrastructure.Quarantined_;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.Root;
import java.util.List;

@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class QuarantinedDao extends GenericDao {
    public List<Quarantined> findItems(Quarantined.ItemSource itemSource) {
        return findAll(Quarantined.class, (cq, root) ->
                cq.where(getCriteriaBuilder().equal(root.get(Quarantined_.itemSource), itemSource)));
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

    public boolean unQuarantine(Quarantined.ItemSource itemSource, Quarantined.ItemType itemType, String item) {
        CriteriaDelete<Quarantined> criteriaDelete = getCriteriaBuilder().createCriteriaDelete(Quarantined.class);
        Root<Quarantined> root = criteriaDelete.from(Quarantined.class);
        criteriaDelete.where(getCriteriaBuilder().and(
                getCriteriaBuilder().equal(root.get(Quarantined_.itemSource), itemSource),
                getCriteriaBuilder().equal(root.get(Quarantined_.itemType), itemType),
                getCriteriaBuilder().equal(root.get(Quarantined_.item), item)));
        int rowCount = getEntityManager().createQuery(criteriaDelete).executeUpdate();
        return rowCount > 0;
    }
}
