package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read only access to lookup search term values.
 */
@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class ConstrainedValueDao extends GenericDao implements Serializable {

    @Inject
    private BSPUserList bspUserList;

    public ConstrainedValueDao(){}

    /**
     * Builds a sorted unique list of event locations from all lab events for search term option list
     * @return List of ConstrainedValues with event location in the code and label
     */
    public List<ConstrainedValue> getLabEventLocationOptionList(){
        List<ConstrainedValue> constrainedValues = new ArrayList<>();

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
        CriteriaQuery query = criteriaBuilder.createQuery(String.class);
        query.distinct(true);
        Root<LabEvent> labEvent = query.from(LabEvent.class);
        query.select(labEvent.get(LabEvent_.eventLocation));
        query.where( criteriaBuilder.isNotNull(labEvent.get(LabEvent_.eventLocation)));

        query.orderBy( criteriaBuilder.asc( labEvent.get(LabEvent_.eventLocation) ) );

        TypedQuery<String> tq = getEntityManager().createQuery(query);

        for (String val : tq.getResultList() ) {
            constrainedValues.add( new ConstrainedValue( val, val ) );
        }
        return constrainedValues;

    }

    /**
     * Builds a sorted unique list of program names from all lab events for search term option list
     * @return List of ConstrainedValues with program name in the code and label
     */
    public List<ConstrainedValue> getLabEventProgramNameList(){
        List<ConstrainedValue> constrainedValues = new ArrayList<>();

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
        CriteriaQuery query = criteriaBuilder.createQuery(String.class);
        query.distinct(true);
        Root<LabEvent> labEvent = query.from(LabEvent.class);
        query.select(labEvent.get(LabEvent_.programName));

        // Ignore nulls
        query.where( criteriaBuilder.isNotNull(labEvent.get(LabEvent_.programName)));

        query.orderBy( criteriaBuilder.asc( labEvent.get(LabEvent_.programName) ) );

        TypedQuery<String> tq = getEntityManager().createQuery(query);

        for (String val : tq.getResultList() ) {
            constrainedValues.add( new ConstrainedValue( val, val) );
        }
        return constrainedValues;

    }

    /**
     * Builds a sorted unique list of user names from all lab events or search term option list
     * @return List of ConstrainedValues with userid in the code and full name in the label
     */
    public List<ConstrainedValue> getLabEventUserNameList(){
        List<ConstrainedValue> constrainedValues = new ArrayList<>();

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
        CriteriaQuery query = criteriaBuilder.createQuery(Long.class);
        query.distinct(true);
        Root<LabEvent> labEvent = query.from(LabEvent.class);
        query.select(labEvent.get(LabEvent_.eventOperator));

        // Ignore nulls
        query.where( criteriaBuilder.isNotNull(labEvent.get(LabEvent_.eventOperator)));

        TypedQuery<Long> tq = getEntityManager().createQuery(query);

        for (Long userId : tq.getResultList() ) {
            String userName = null;
            BspUser user = bspUserList.getById(userId);
            if( user != null ) {
                userName = user.getFullName();
                if (userName == null) {
                    userName = user.getUsername();
                }
            }
            constrainedValues.add( new ConstrainedValue( userId.toString(), userName==null?"Unknown user - ID: " + userId:userName ) );
        }
        Collections.sort(constrainedValues);
        return constrainedValues;
    }

}
