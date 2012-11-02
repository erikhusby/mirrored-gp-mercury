package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.BillableItemDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.BillableItem;

import javax.ejb.Stateless;
import javax.inject.Inject;

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
    String makeRecord(String etlDateStr, boolean isDelete, Long entityId) {
        BillableItem entity = dao.findById(BillableItem.class, entityId);
        if (entity == null) {
            return null;
        } else {
            return Util.makeRecord(etlDateStr, false,
                    entity.getBillableItemId(),
                    Util.format(entity.getProductOrderSample() == null ? null : entity.getProductOrderSample().getProductOrderSampleId()),
                    Util.format(entity.getPriceItem() == null ? null : entity.getPriceItem().getPriceItemId()),
                    Util.format(entity.getCount()));
        }
    }
}
