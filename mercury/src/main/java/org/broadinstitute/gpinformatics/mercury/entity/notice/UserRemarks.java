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

    public void setUserStatus( Long user,String status){
        throw new RuntimeException("I haven't been written yet.");
    }

    public void getUserStatus( Long user){
        throw new RuntimeException("I haven't been written yet.");
    }

    public void addUserNote( Long user,String note){
        throw new RuntimeException("I haven't been written yet.");
    }

    public Collection<String> getUserNotes( Long user){
        throw new RuntimeException("I haven't been written yet.");
    }

    public boolean isUserFlagged( Long user){
        throw new RuntimeException("I haven't been written yet.");
    }

    public void setUserFlag( Long user,boolean isFlagged){
        throw new RuntimeException("I haven't been written yet.");
    }

    public INTERESTINGNESS getUserInterestLevel( Long user){
        throw new RuntimeException("I haven't been written yet.");
    }

    public void setUserInterestLevel( Long user,INTERESTINGNESS interestLevel){
        throw new RuntimeException("I haven't been written yet.");
    }

    public Collection<String> getAllNotes(){
        throw new RuntimeException("I haven't been written yet.");
    }

    /**
     * Kind of an "unread" flag
     */
    public void hasUserUpdate( Long user){
        throw new RuntimeException("I haven't been written yet.");
    }

    public void setUserUpdate( Long user,boolean isNew){
        throw new RuntimeException("I haven't been written yet.");
    }

    public Date getUserCheckbackDate( Long user){
        throw new RuntimeException("I haven't been written yet.");
    }

    public void setUserCheckbackDate( Long user,Date targetDate){
        throw new RuntimeException("I haven't been written yet.");
    }

    /**
     * Sets the category in which this thing should
     * appear for the given user .  Categories are
     * free form.  Could be things like "to rework",
     * "check up with Sheila about", "hold for new PASS",
     * but a thing can be in only one category at a time.
     * @param user
     * @param category
     */
    public void setUserCategory( Long user,String category){
        throw new RuntimeException("I haven't been written yet.");
    }

    /**
     * Gets the category in which this thing should
     * appear for the given user
     *
     * @param user
     * @return
     */
    public String getUserCategory( Long user){
        throw new RuntimeException("I haven't been written yet.");
    }
}
