package org.broadinstitute.gpinformatics.athena.entity.infrastructure;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Extension of the AccessControl entity which will allow us to control access to the SAP integration service.
 *
 * The current Implementation is in 2 parts:
 * <ol>
 *     <li><b>Control access to the SAP service</b> -- A failsafe if something is just not right with the
 *     implementation for connecting to SAP.  Allows us to turn off saving orders and billing to SAP</li>
 *     <li><b>disallow certain price items</b> -- From Phase 1, and phase 2, there are going to be some Price items
 *     (which will translate into Products, which will translate into Materials).  Utilizing the 'disabledFeatures'
 *     field of AccessControl will allow us to select certain price items for which Product orders will not be able
 *     to create SAP orders</li>
 * </ol>
 */
@Entity
public class SAPAccessControl extends AccessControl {

    private static final long serialVersionUID = 7054820080520463342L;

    public SAPAccessControl() {

    }

    @Override
    public String getControlTitle() {
        return "SAP Access Controller";
    }

}