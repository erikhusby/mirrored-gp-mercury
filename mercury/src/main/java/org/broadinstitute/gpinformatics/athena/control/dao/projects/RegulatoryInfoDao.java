package org.broadinstitute.gpinformatics.athena.control.dao.projects;

import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
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

    public RegulatoryInfo findByIdentifierAndType(String identifier, RegulatoryInfo.Type type) {
        return findSingle(RegulatoryInfo.class, new GenericDaoCallback<RegulatoryInfo>() {
            @Override
            public void callback(CriteriaQuery<RegulatoryInfo> criteriaQuery, Root<RegulatoryInfo> root) {
                CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
                Predicate predicate = criteriaBuilder.and(
                    criteriaBuilder.equal(root.get(RegulatoryInfo_.identifier), identifier),
                    criteriaBuilder.equal(root.get(RegulatoryInfo_.type), type)
                );
                criteriaQuery.where(predicate);
            }
        });
    }

    public List<RegulatoryInfo> findByName(String name) {
        return findList(RegulatoryInfo.class, RegulatoryInfo_.name, name);
    }
}
