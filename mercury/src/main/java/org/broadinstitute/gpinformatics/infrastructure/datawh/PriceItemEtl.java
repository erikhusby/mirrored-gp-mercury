package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Date;

@Stateless
public class PriceItemEtl  extends GenericEntityEtl {
    @Inject
    PriceItemDao dao;

    @Override
    Class getEntityClass() {
        return PriceItem.class;
    }

    @Override
    String getBaseFilename() {
        return "price_item";
    }

    @Override
    Long entityId(Object entity) {
        return ((PriceItem)entity).getPriceItemId();
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
        PriceItem entity = dao.findById(PriceItem.class, entityId);
        if (entity == null) {
            logger.info("Cannot export. PriceItem having id " + entityId + " no longer exists.");
            return null;
        }
        return genericRecord(etlDateStr, isDelete,
                entity.getPriceItemId(),
                format(entity.getPlatform()),
                format(entity.getCategory()),
                format(entity.getName()),
                format(entity.getQuoteServerId()),
                format(entity.getPrice()),
                format(entity.getUnits())
        );
    }

    /** This entity does not make status records. */
    @Override
    String entityStatusRecord(String etlDateStr, Date revDate, Object entity, boolean isDelete) {
        return null;
    }

    /** This entity does support add/modify records via primary key. */
    @Override
    boolean isEntityEtl() {
        return true;
    }
}
