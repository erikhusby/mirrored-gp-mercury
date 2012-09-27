package org.broadinstitute.gpinformatics.athena.entity.person;

/**
 * Access to a web page.  Either you
 * have read only access or you have
 * full access.
 *
 * This has interesting ramifications for
 * how pages are constructed.  Each widget
 * has to be tagged with some kind
 * of readable or full access annotation.
 * Or maybe each backing bean action method
 * needs an annotation
 * to say "I am a full access method"
 * vs. "I am read only".  Auto magically
 * page rendering would look at these
 * annotations to render things properly,
 * greying out update controls, etc.
 */
public interface PageAccess {

    public enum READ_WRITE_ACCESS {
        READ_ONLY,
        FULL_ACCESS
    }

    /**
     * Whatever the "page" notion is in JSF.
     * Hopefully not just a string name.
     * @return
     */
    public String getPage();

    /**
     * Can you just read data from this page,
     * or can you read and write it?
     * @return
     */
    public READ_WRITE_ACCESS getAccessLevel();

}
