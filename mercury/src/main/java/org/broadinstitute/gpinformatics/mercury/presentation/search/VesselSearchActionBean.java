package org.broadinstitute.gpinformatics.mercury.presentation.search;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@UrlBinding(VesselSearchActionBean.ACTIONBEAN_URL_BINDING)
public class VesselSearchActionBean extends SearchActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/search/vessel.action";
    private static final String SESSION_LIST_PAGE = "/search/vessel_search.jsp";

    @Override
    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @HandlesEvent("vesselSearch")
    public Resolution vesselSearch() throws Exception {
        doSearch(SearchType.VESSELS_BY_BARCODE);
        orderResults();
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    /**
     * This method orders the results based on the order of strings passed in.
     */
    private void orderResults() {
        Map<String, LabVessel> labelToVessel = new HashMap<String, LabVessel>();
        for (LabVessel vessel : getFoundVessels()) {
            labelToVessel.put(vessel.getLabel(), vessel);
        }
        setFoundVessels(new LinkedHashSet<LabVessel>());
        List<String> searchOrder = cleanInputString(getSearchKey());
        for (String key : searchOrder) {
            LabVessel labVessel = labelToVessel.get(key);
            if (labVessel != null) {
                getFoundVessels().add(labVessel);
            }
        }
    }
}
