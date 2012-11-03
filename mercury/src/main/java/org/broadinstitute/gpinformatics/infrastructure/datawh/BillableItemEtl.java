package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.BillableItemDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.BillableItem;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Date;

@Stateless
public class BillableItemEtl extends GenericEntityEtl {
    @Inject
    BillableItemDao dao;

    @Override
    Class getEntityClass() {
        return BillableItem.class;
    }

    @Override
    String getBaseFilename() {
        return "billable_item";
    }

    @Override
    Long entityId(Object entity) {
        return ((BillableItem)entity).getBillableItemId();
    }

    /**
     * Makes a data record from selected entity fields, in a format that matches the corresponding
     * SqlLoader control file.
     * @param etlDateStr date
     * @param isDelete indicates deleted entity
     * @param entityId look up this entity
     * @return delimited SqlLoader record
     */
    @Override
    String entityRecord(String etlDateStr, boolean isDelete, Long entityId) {
        BillableItem entity = dao.findById(BillableItem.class, entityId);
        if (entity == null) {
            return null;
        } else {
            return genericRecord(etlDateStr, false,
                    entity.getBillableItemId(),
                    format(entity.getProductOrderSample() == null ? null : entity.getProductOrderSample().getProductOrderSampleId()),
                    format(entity.getPriceItem() == null ? null : entity.getPriceItem().getPriceItemId()),
                    format(entity.getCount()));
        }
    }

    /** This entity does not make status records. */
    @Override
    String entityStatusRecord(String etlDateStr, Date revDate, Object entity) {
        return null;
    }

    /** This entity does support add/modify records via primary key. */
    @Override
    boolean isEntityEtl() {
        return true;
    }
}
