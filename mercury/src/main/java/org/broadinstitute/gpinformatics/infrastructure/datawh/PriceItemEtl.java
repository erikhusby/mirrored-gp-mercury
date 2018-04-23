package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem_;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;

@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class PriceItemEtl extends GenericEntityEtl<PriceItem, PriceItem> {
    public PriceItemEtl() {
    }

    @Inject
    public PriceItemEtl(PriceItemDao dao) {
        super(PriceItem.class, "price_item", "athena.price_item_aud", "price_item_id", dao);
    }

    @Override
    Long entityId(PriceItem entity) {
        return entity.getPriceItemId();
    }

    @Override
    Path rootId(Root<PriceItem> root) {
        return root.get(PriceItem_.priceItemId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(PriceItem.class, entityId));
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, PriceItem entity) {
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
}
