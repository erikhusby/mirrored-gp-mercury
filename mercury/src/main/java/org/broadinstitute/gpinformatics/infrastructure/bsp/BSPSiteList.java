package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.bsp.client.site.AllSitesResponse;
import org.broadinstitute.bsp.client.site.BspSiteManager;
import org.broadinstitute.bsp.client.site.Site;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;
import org.broadinstitute.gpinformatics.infrastructure.jmx.AbstractCache;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The list of all BSP Sites, cached because it can be very large and rarely changes.
 */
@ApplicationScoped
public class BSPSiteList extends AbstractCache implements Serializable {
    private final BSPManagerFactory bspManagerFactory;

    private Map<Long, Site> sites;

    private BspSiteManager bspSiteManager;

    /**
     * @return map of bsp sites, keyed by site ID.
     */
    public synchronized Map<Long, Site> getSites() {
        if (sites == null) {
            refreshCache();
        }
        return sites;
    }

    /**
     * @param id key of site to look up
     *
     * @return if found, the site, otherwise null
     */
    public Site getById(long id) {
        return getSites().get(id);
    }

    /**
     * Returns a list of sites whose name, address, shipper or description match the given query.  If the query is
     * null then it will return an empty list. If a collection id is passed then the results will be further filtered
     * by sites within the collection.
     *
     * @param query        the query string to match on
     * @param collectionId the id of the collection to filter the sites off
     *
     * @return a list of matching sites
     */
    @Nonnull
    public List<Site> find(String query, Long collectionId) {
        if (StringUtils.isBlank(query)) {
            // no query string supplied
            return Collections.emptyList();
        }

        if (bspSiteManager == null) {
            this.bspSiteManager = bspManagerFactory.createSiteManager();
        }

        Collection<Site> siteResults = null;
        // If there is a collection selected we will filter based off the collection.
        if (collectionId != null) {
            siteResults = bspSiteManager.getApplicableSites(collectionId).getResult();
        } else {
            siteResults = getSites().values();
        }

        String[] lowerQueryItems = query.toLowerCase().split("\\s");
        List<Site> results = new ArrayList<>();
        for (Site site : siteResults) {
            boolean eachItemMatchesSomething = true;
            for (String lowerQuery : lowerQueryItems) {
                // If none of the fields match this item, then all items are not matched
                if (!anyFieldMatches(lowerQuery, site)) {
                    eachItemMatchesSomething = false;
                }
            }

            if (eachItemMatchesSomething) {
                results.add(site);
            }
        }

        return results;
    }

    private static boolean anyFieldMatches(String lowerQuery, Site site) {
        return safeToLowerCase(site.getName()).contains(lowerQuery) ||
               safeToLowerCase(site.getAddress()).contains(lowerQuery) ||
               safeToLowerCase(site.getPrimaryShipper()).contains(lowerQuery) ||
               safeToLowerCase(site.getDescription()).contains(lowerQuery);
    }

    private static String safeToLowerCase(String s) {
        if (s == null) {
            return "";
        } else {
            return s.toLowerCase();
        }
    }

    public BSPSiteList() {
        this(null);
    }

    @Inject
    public BSPSiteList(@SuppressWarnings("CdiInjectionPointsInspection") BSPManagerFactory bspManagerFactory) {
        this.bspManagerFactory = bspManagerFactory;
    }

    @Override
    public synchronized void refreshCache() {
        if (bspSiteManager == null) {
            this.bspSiteManager = bspManagerFactory.createSiteManager();
        }

        AllSitesResponse response = bspSiteManager.getAllSites();
        if (!response.isSuccess()) {
            if (sites == null) {
                sites = new HashMap<>();
            }
            return;
        }

        List<Site> rawSites = response.getResult();

        Collections.sort(rawSites, new Comparator<Site>() {
            @Override
            public int compare(Site o1, Site o2) {
                // FIXME: need to figure out what the correct sort criteria are.
                CompareToBuilder builder = new CompareToBuilder();
                builder.append(o1.getName(), o2.getName());
                return builder.build();
            }
        });

        // Use a LinkedHashMap since (1) it preserves the insertion order of its elements, so
        // our entries stay sorted and (2) it has lower overhead than a TreeMap.
        Map<Long, Site> siteMap = new LinkedHashMap<>(rawSites.size());
        for (Site site : rawSites) {
            siteMap.put(site.getId(), site);
        }

        sites = ImmutableMap.copyOf(siteMap);
    }

    public BspSiteManager getBspSiteManager() {
        return bspSiteManager;
    }

    public void setBspSiteManager(BspSiteManager bspSiteManager) {
        this.bspSiteManager = bspSiteManager;
    }
}
