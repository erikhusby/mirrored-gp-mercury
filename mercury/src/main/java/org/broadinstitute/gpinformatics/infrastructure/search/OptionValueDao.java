package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;

import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.enterprise.context.RequestScoped;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Read only access to lookup search term values.
 */
@Stateful
@RequestScoped
public class OptionValueDao extends GenericDao implements Serializable {

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
     * Returns 11,000 products - not suitable for web selection
     * @return
     */
    @Deprecated
    public List<ConstrainedValue> getProductOrderOptionList(){
        List<ConstrainedValue> constrainedValues = new ArrayList<>();

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
        CriteriaQuery query = criteriaBuilder.createQuery();
        Root<ProductOrder> productOrder = query.from(ProductOrder.class);

        Path<Long> idPath = productOrder.get( ProductOrder_.productOrderId );
        Path<String> namePath = productOrder.get( ProductOrder_.jiraTicketKey );
        query.multiselect( idPath, namePath );
        List<Object[]> valueArray = getEntityManager().createQuery( query ).getResultList();
        for ( Object[] values : valueArray ) {
            constrainedValues.add( new ConstrainedValue( ((Long)values[0]).toString(), (String)values[1] ) );
        }

        return constrainedValues;

    }

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


}
