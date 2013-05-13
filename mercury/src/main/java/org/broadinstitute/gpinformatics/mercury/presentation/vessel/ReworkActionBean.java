/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

@UrlBinding(value = "/view/rework.action")
public class ReworkActionBean extends CoreActionBean {

    private static final String VIEW_PAGE = "/container/rework.jsp";

    private String jqueryClass;
    private String colorStyle;
    private Boolean reverseOrder;

    public String getJqueryClass() {
        return jqueryClass;
    }

    public void setJqueryClass(String jqueryClass) {
        this.jqueryClass = jqueryClass;
    }

    public void setColorStyle(String colorStyle) {
        this.colorStyle = colorStyle;
    }

    public String getColorStyle() {
        return colorStyle;
    }

    @DefaultHandler
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(value = "applySettings")
    public Resolution applySettings() {
        return new RedirectResolution(VIEW_PAGE);
    }

    public void setReverseOrder(Boolean reverseOrder) {
        this.reverseOrder = reverseOrder;
    }

    public Boolean getReverseOrder() {
        return reverseOrder;
    }
}
