package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
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

        return entityRecord(etlDateStr, isDelete, entity);
    }

    /**
     * Returns data records for entity instances in the range of ids.
     * @return collection of sqlloader records for the entities
     */
    @Override
    Collection<String> entityRecordsInRange(long startId, long endId, String etlDateStr, boolean isDelete) {
        Collection<String> allRecords = new ArrayList<String>();
        if (startId == 0 && endId == Long.MAX_VALUE) {
            // Default case gets all entities.
            for (PriceItem entity : dao.findAll(PriceItem.class)) {
                allRecords.add(entityRecord(etlDateStr, isDelete, entity));
            }
        } else {
            // Spins through the ids one at a time.
            // TODO change this to specify the range in a GenericDaoCallback
            for (long entityId = startId; entityId <= endId; ++entityId) {
                PriceItem entity = dao.findById(PriceItem.class, entityId);
                if (entity != null) {
                    allRecords.add(entityRecord(etlDateStr, isDelete, entity));
                }
            }
        }
        return allRecords;
    }

    /**
     * Makes a data record from an entity, in a format that matches the corresponding SqlLoader control file.
     * @param entity Mercury Entity
     * @return delimited SqlLoader record
     */
    String entityRecord(String etlDateStr, boolean isDelete, PriceItem entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getPriceItemId(),
                format(entity.getPlatform()),
                format(entity.getCategory() != null ? entity.getCategory() : "none"),
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
