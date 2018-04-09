package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.bsp.client.collection.SampleCollection;
import org.broadinstitute.bsp.client.site.BspSiteManager;
import org.broadinstitute.bsp.client.site.Site;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSiteList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONException;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.List;

/**
 * Token Input support for Shipping Locations (aka Sites).
 */
@Dependent
public class BspShippingLocationTokenInput extends TokenInput<Site> {

    private static final String ADDITIONAL_LINE_FORMAT = "<div class=\"ac-dropdown-multiline-subtext\">{0}</div>";

    @Inject
    private BSPManagerFactory bspManagerFactory;

    @Inject
    private BSPSiteList bspSiteList;

    private List<Site> sites;

    private long cachedCollectionId = -1;

    public BspShippingLocationTokenInput() {
        super(SINGLE_LINE_FORMAT);
    }

    public String getJsonString(String query, SampleCollection collection) throws JSONException {
        if (sites == null || cachedCollectionId != collection.getCollectionId()) {
            cachedCollectionId = collection.getCollectionId();
            BspSiteManager siteManager = bspManagerFactory.createSiteManager();
            sites = siteManager.getApplicableSites(cachedCollectionId).getResult();
        }

        return createItemListString(BSPSiteList.find(sites, query));
    }

    @Override
    protected String getTokenId(Site site) {
        return String.valueOf(site.getId());
    }

    @Override
    protected String getTokenName(Site site) {
        return site.getName();
    }

    /**
     * Reformat an address so its lines split into different menu lines, when shown in the HTML drop down menu.
     *
     * @param address the address, with embedded CRs
     *
     * @return the HTML formatted address text
     */
    private static String formatAddress(String address) {
        StringBuilder formattedAddress = new StringBuilder();
        for (String line : address.split("\n")) {
            formattedAddress.append(MessageFormat.format(ADDITIONAL_LINE_FORMAT, line));
        }
        return formattedAddress.toString();
    }

    @Override
    protected String formatMessage(String messageString, Site tokenObject) {
        return MessageFormat.format(messageString, tokenObject.getName()) +
               MessageFormat.format(ADDITIONAL_LINE_FORMAT, tokenObject.getPrimaryShipper()) +
               formatAddress(tokenObject.getAddress());
    }

    @Override
    protected Site getById(String key) {
        return bspSiteList.getById(Long.valueOf(key));
    }
}
