package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.RegulatoryInfoDao;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo_;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;

/**
 * Copy contents/changes to RegulatoryInfo (table REGULATORY_INFO) to warehouse
 * Data used to update denormalized records in PDO_REGULATORY_INFOS table
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class RegulatoryInfoEtl extends GenericEntityEtl<RegulatoryInfo,RegulatoryInfo> {

    public RegulatoryInfoEtl() {
    }

    @Inject
    public RegulatoryInfoEtl(RegulatoryInfoDao dao) {
        super(RegulatoryInfo.class, "regulatory_info", "athena.regulatory_info_aud", "regulatory_info_id", dao);
    }

    @Override
    Long entityId(RegulatoryInfo entity) {
        return entity.getRegulatoryInfoId();
    }

    @Override
    Path rootId(Root<RegulatoryInfo> root) {
        return root.get(RegulatoryInfo_.regulatoryInfoId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(RegulatoryInfo.class, entityId));
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, RegulatoryInfo entity) {
        // Nothing nullable in table
        return genericRecord(etlDateStr, isDelete,
                entity.getRegulatoryInfoId(),
                format(entity.getIdentifier()),
                format(entity.getType().getName()),
                format(entity.getName())
        );
    }
}
