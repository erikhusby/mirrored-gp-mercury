package org.broadinstitute.gpinformatics.athena.presentation.search;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.broadinstitute.gpinformatics.athena.boundary.search.SearchEjb;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.security.SecurityActionBean;
import org.owasp.encoder.Encode;

import javax.inject.Inject;

/**
 * This handles all the needed interface processing elements.
 */
@UrlBinding(SearchActionBean.ACTIONBEAN_URL_BINDING)
public final class SearchActionBean extends CoreActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/search/search.action";

    /**
     * Action for handling when user enters search text in navigation form search textfield.
     */
    public static final String QUICK_SEARCH_ACTION = "quickSearch";

    @Inject
    private SearchEjb searchEjb;

    @Validate(required = true, on = {QUICK_SEARCH_ACTION})
    private String searchKey;

    @DefaultHandler
    @HandlesEvent(QUICK_SEARCH_ACTION)
    public Resolution quickSearch() throws Exception {
        SearchEjb.SearchResult searchResult = searchEjb.search(searchKey);

        if (searchResult != null) {
            return new RedirectResolution(searchResult.getActionBeanClass(), "view")
                    .addParameter(searchResult.getParameter(), searchResult.getBusinessObject().getBusinessKey());
        }

        // Did not find anything matching the search string so it could have been a bad prefix, bad business key or
        // whatever.  This happens when we've looked through all search types and nothing has been found.  Allowing
        // this to be null ensures we don't have to encode any prefix string in order determine what kind of search
        // it is, but keeps things generic.
        addGlobalValidationError("There were no matching items for ''{2}''.", Encode.forHtml(searchKey));

        // Can't just stay where you are and report back the error because page could have been submitting data and
        // reloading it can cause side effects (or not, it is unknown).  Add any known parameters back on.
        return new RedirectResolution(SecurityActionBean.HOME_PAGE)
                .addParameters(getContext().getRequest().getParameterMap()).flash(this);
    }

    public String getSearchKey() {
        return searchKey;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }
}
