package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import java.util.Arrays;
import java.util.List;

@UrlBinding(value = "/view/heatMap.action")
public class HeatMapActionBean extends CoreActionBean {

    private static final String VIEW_PAGE = "/container/heat_map.jsp";
    private static final String VIEW_HEAT_MAP = "viewHeatMap";

    private String heatMapFieldString;
    private String jqueryClass;
    private String colorStyle = "redtogreen";
    private Boolean reverseOrder = false;
    private List<String> heatMapFields = null;

    public String getHeatMapFieldString() {
        return heatMapFieldString;
    }

    public void setHeatMapFieldString(String heatMapFieldString) {
        this.heatMapFieldString = heatMapFieldString;
    }

    public List<String> getHeatMapFields() {
        if (heatMapFields == null) {
            String[] fields = heatMapFieldString.split(",");
            heatMapFields = Arrays.asList(fields);
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
    @HandlesEvent(VIEW_HEAT_MAP)
    public Resolution viewHeatMap() {
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
