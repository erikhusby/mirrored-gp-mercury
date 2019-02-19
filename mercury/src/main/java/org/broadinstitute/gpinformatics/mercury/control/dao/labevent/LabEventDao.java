package org.broadinstitute.gpinformatics.mercury.control.dao.labevent;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Data Access Object for LabEvents
 */
@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class LabEventDao extends GenericDao {
    /**
     * Find lab events that were performed between a date range
     * @param beginDate when the date range begins
     * @param endDate when the date range ends
     * @return list of events
     */
    public List<LabEvent> findByDate(final Date beginDate, final Date endDate) {
        return findAll(LabEvent.class, new GenericDaoCallback<LabEvent>() {
            @Override
            public void callback(CriteriaQuery<LabEvent> criteriaQuery, Root<LabEvent> root) {
                criteriaQuery.where(getCriteriaBuilder().between(root.get(LabEvent_.eventDate), beginDate, endDate));
            }
        });
    }

    public List<LabEvent> findByDateAndType(final Date beginDate, final Date endDate, LabEventType labEventType) {
        return findAll(LabEvent.class, new GenericDaoCallback<LabEvent>() {
            @Override
            public void callback(CriteriaQuery<LabEvent> criteriaQuery, Root<LabEvent> root) {
                criteriaQuery.where(getCriteriaBuilder().between(root.get(LabEvent_.eventDate), beginDate, endDate),
                        getCriteriaBuilder().equal(root.get(LabEvent_.labEventType), labEventType));
            }
        });
    }

    public LabEvent findByLocationDateDisambiguator(final String location, final Date date, final Long disambiguator) {
        return findSingle(LabEvent.class, new GenericDaoCallback<LabEvent>() {
            @Override
            public void callback(CriteriaQuery<LabEvent> criteriaQuery, Root<LabEvent> root) {
                criteriaQuery.where(getCriteriaBuilder().equal(root.get(LabEvent_.eventLocation), location),
                        getCriteriaBuilder().equal(root.get(LabEvent_.eventDate), date),
                        getCriteriaBuilder().equal(root.get(LabEvent_.disambiguator), disambiguator));
            }
        });
    }

    /**
     * Racks of tubes are attached to in place events as ancillary vessels
     * Find any in-place events associated to a rack
     */
    public List<LabEvent> findInPlaceByAncillaryVessel(final LabVessel rackOfTubes ) {
        // Only valid for racks of tubes
        if(OrmUtil.proxySafeIsInstance(rackOfTubes, RackOfTubes.class)) {
            return findAll(LabEvent.class, new GenericDaoCallback<LabEvent>() {
                @Override
                public void callback(CriteriaQuery<LabEvent> criteriaQuery, Root<LabEvent> root) {
                    criteriaQuery
                            .where(getCriteriaBuilder().equal(root.get(LabEvent_.ancillaryInPlaceVessel), rackOfTubes));
                }
            });
        } else {
            return Collections.emptyList();
        }
    }
}
