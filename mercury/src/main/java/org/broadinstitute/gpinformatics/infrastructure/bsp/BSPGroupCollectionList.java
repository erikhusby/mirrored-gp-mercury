package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.bsp.client.collection.BspGroupCollectionManager;
import org.broadinstitute.bsp.client.collection.Group;
import org.broadinstitute.bsp.client.collection.SampleCollection;
import org.broadinstitute.bsp.client.response.AllCollectionsResponse;
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
 * The list of all BSP collections, cached because it can be very large and rarely changes.
 */
@ApplicationScoped
public class BSPGroupCollectionList extends AbstractCache implements Serializable {
    private final BSPManagerFactory bspManagerFactory;

    private Map<Long, SampleCollection> collections;
    private Map<Long, Group> groups;
    private Multimap<Long, SampleCollection> mapGroupIdToCollections;

    /**
     * @return map of bsp collections, keyed by collection ID.
     */
    public synchronized Map<Long, SampleCollection> getCollections() {
        if (collections == null) {
            refreshCache();
        }
        return collections;
    }

    public synchronized Map<Long, Group> getGroups() {
        if (groups == null) {
            refreshCache();
        }
        return groups;
    }

    /**
     * @param id key of site to look up
     * @return if found, the site, otherwise null
     */
    public SampleCollection getById(long id) {
        return getCollections().get(id);
    }

    /**
     * Returns a list of sites whose name, address, shipper or description match the given query.  If the query is
     * null then it will return an empty list.
     *
     * @param query the query string to match on
     * @return a list of matching users
     */
    @Nonnull
    public List<SampleCollection> find(String query) {
        if (StringUtils.isBlank(query)) {
            // no query string supplied
            return Collections.emptyList();
        }

        String[] lowerQueryItems = query.toLowerCase().split("\\s");
        List<SampleCollection> results = new ArrayList<>();
        for (SampleCollection collection : getCollections().values()) {
            boolean eachItemMatchesSomething = true;
            for (String lowerQuery : lowerQueryItems) {
                // If none of the fields match this item, then all items are not matched
                if (!anyFieldMatches(lowerQuery, collection)) {
                    eachItemMatchesSomething = false;
                }
            }

            if (eachItemMatchesSomething) {
                results.add(collection);
            }
        }

        return results;
    }

    public Collection<SampleCollection> collectionsForGroup(Long groupId) {
        return mapGroupIdToCollections.get(groupId);
    }

    private static boolean anyFieldMatches(String lowerQuery, SampleCollection collection) {
        return safeToLowerCase(collection.getCollectionName()).contains(lowerQuery) ||
               safeToLowerCase(collection.getGroup().getGroupName()).contains(lowerQuery);
    }

    private static String safeToLowerCase(String s) {
        if (s == null) {
            return "";
        } else {
            return s.toLowerCase();
        }
    }

    public BSPGroupCollectionList() {
        this(null);
    }

    // SuppressWarnings required due to intellij confusion about ambiguous injection.
    @Inject
    public BSPGroupCollectionList(@SuppressWarnings("CdiInjectionPointsInspection") BSPManagerFactory bspManagerFactory) {
        this.bspManagerFactory = bspManagerFactory;
    }

    @Override
    public synchronized void refreshCache() {
        BspGroupCollectionManager groupCollectionManager = bspManagerFactory.createGroupCollectionManager();
        AllCollectionsResponse response = groupCollectionManager.getAllCollections();
        if (!response.isSuccess()) {
            if (collections == null) {
                collections = new HashMap<>();
            }
            return;
        }

        List<SampleCollection> allCollections = response.getResult();

        Collections.sort(allCollections, new Comparator<SampleCollection>() {
            @Override
            public int compare(SampleCollection o1, SampleCollection o2) {
                // Sort by Group, then by collection within group.
                CompareToBuilder builder = new CompareToBuilder();
                builder.append(o1.getGroup().getGroupName(), o2.getGroup().getGroupName());
                builder.append(o1.getCollectionName(), o2.getCollectionName());
                return builder.build();
            }
        });

        // Use a LinkedHashMap since (1) it preserves the insertion order of its elements, so
        // our entries stay sorted and (2) it has lower overhead than a TreeMap.
        Map<Long, SampleCollection> collectionsMap = new LinkedHashMap<>(allCollections.size());
        Map<Long, Group> groupsMap = new LinkedHashMap<>();
        Multimap<Long, SampleCollection> mapGroupIdToCollectionsLocal = ArrayListMultimap.create();
        for (SampleCollection collection : allCollections) {
            collectionsMap.put(collection.getCollectionId(), collection);
            groupsMap.put(collection.getGroup().getGroupId(), collection.getGroup());
            mapGroupIdToCollectionsLocal.put(collection.getGroup().getGroupId(), collection);
        }

        // Create an immutable map since this is an application scoped object that's returned directly to the caller.
        // We don't want to allow one thread to affect another by modifying this.
        collections = ImmutableMap.copyOf(collectionsMap);
        groups = ImmutableMap.copyOf(groupsMap);
        mapGroupIdToCollections = ImmutableMultimap.copyOf(mapGroupIdToCollectionsLocal);
    }
}
