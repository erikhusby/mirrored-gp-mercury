package org.broadinstitute.gpinformatics.athena.entity.infrastructure;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


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