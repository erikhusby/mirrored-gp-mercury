package org.broadinstitute.gpinformatics.mercury.entity.notice;


import javax.persistence.Embeddable;
import java.util.Collection;
import java.util.Date;

/**
 * Anything that is PMRemarkable is visible
 * in the PM dashboard.  In other words,
 * things that PMs want to keep tabs on
 * are PMRemarkable.
 */
@Embeddable
public class UserRemarks {
    // In the entity in which this component is @Embedded, use @AssociationOverride to indicate the JoinTable and
    // the foreign key column from that entity.
//    @ManyToMany
//    Set<Remark> remarks;

    public enum INTERESTINGNESS {
        /**
         * Very important.  Put it at the top
         * of the list.
         */
        EXTREMELY,
        /**
         * Whatever, not as important.
         */
        WHATEVER
    }
}
