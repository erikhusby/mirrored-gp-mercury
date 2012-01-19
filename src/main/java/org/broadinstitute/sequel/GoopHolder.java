package org.broadinstitute.sequel;

/**
 * Ability to get/set {@link Goop}.
 */
public interface GoopHolder {

    /**
     * What's the stuff in this thing?
     * @return
     */
    public Goop getGoop();
    
    public void setGoop(Goop goop);
}
