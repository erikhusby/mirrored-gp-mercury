package org.broadinstitute.sequel.entity.notice;

import org.broadinstitute.sequel.entity.person.Person;

import java.util.Collection;
import java.util.Date;

/**
 * Anything that is PMRemarkable is visible
 * in the PM dashboard.  In other words,
 * things that PMs want to keep tabs on
 * are PMRemarkable.
 */
public interface UserRemarkable {

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

    public void setUserStatus(Person user,String status);

    public void getUserStatus(Person user);

    public void addUserNote(Person user,String note);

    public Collection<String> getUserNotes(Person user);

    public boolean isUserFlagged(Person user);

    public void setUserFlag(Person user,boolean isFlagged);

    public INTERESTINGNESS getUserInterestLevel(Person user);

    public void setUserInterestLevel(Person user,INTERESTINGNESS interestLevel);

    public Collection<String> getAllNotes();

    /**
     * Kind of an "unread" flag
     */
    public void hasUserUpdate(Person user);

    public void setUserUpdate(Person user,boolean isNew);

    public Date getUserCheckbackDate(Person user);

    public void setUserCheckbackDate(Person user,Date targetDate);

    /**
     * Sets the category in which this thing should
     * appear for the given user .  Categories are
     * free form.  Could be things like "to rework",
     * "check up with Sheila about", "hold for new PASS",
     * but a thing can be in only one category at a time.
     * @param user
     * @param category
     */
    public void setUserCategory(Person user,String category);

    /**
     * Gets the category in which this thing should
     * appear for the given user
     * @param user
     * @return
     */
    public String getUserCategory(Person user);
}
