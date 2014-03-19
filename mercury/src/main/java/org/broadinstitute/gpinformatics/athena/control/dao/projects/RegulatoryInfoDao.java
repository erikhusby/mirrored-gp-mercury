package org.broadinstitute.gpinformatics.athena.control.dao.projects;

import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import java.util.List;

/**
 * Provides support to the application for querying RegulatoryInfo
 */
@Stateful
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@RequestScoped
public class RegulatoryInfoDao extends GenericDao {

    public List<RegulatoryInfo> findByIdentifier(String identifier) {
        return findList(RegulatoryInfo.class, RegulatoryInfo_.identifier, identifier);
    }

    public List<RegulatoryInfo> findByName(String name) {
        return findList(RegulatoryInfo.class, RegulatoryInfo_.name, name);
    }
}
