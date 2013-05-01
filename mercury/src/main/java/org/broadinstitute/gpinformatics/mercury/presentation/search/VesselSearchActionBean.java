package org.broadinstitute.gpinformatics.mercury.presentation.search;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;

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
        return new ForwardResolution(SESSION_LIST_PAGE);
    }
}
