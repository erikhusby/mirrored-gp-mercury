package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.*;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import java.util.Arrays;
import java.util.List;

@UrlBinding(value = "/view/heatMap.action")
public class HeatMapActionBean extends CoreActionBean {

    private static final String VIEW_PAGE = "/resources/container/heatMap.jsp";

    private String jqueryClass;
    private String colorStyle = "redtogreen";
    private Boolean reverseOrder = false;
    private List<String> heatMapFields = null;

    public List<String> getHeatMapFields() {
        if (heatMapFields == null) {
            String[] fields = jqueryClass.split(",");
            heatMapFields = Arrays.asList(fields);
            if (heatMapFields.size() > 0) {
                jqueryClass = "." + heatMapFields.get(0);
            }
        }
        return heatMapFields;
    }

    public void setHeatMapFields(List<String> heatMapFields) {
        this.heatMapFields = heatMapFields;
    }

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
