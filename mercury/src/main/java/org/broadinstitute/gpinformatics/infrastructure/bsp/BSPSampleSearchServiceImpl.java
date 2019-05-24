package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.mercury.boundary.zims.BSPLookupException;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJaxRsClientService;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Dependent
@Default
public class BSPSampleSearchServiceImpl extends AbstractJaxRsClientService implements BSPSampleSearchService {
    private static final long serialVersionUID = 3432255750259397293L;

    public static final String SEARCH_RUN_SAMPLE_SEARCH = "search/runSampleSearch";

    private BSPConfig bspConfig;

    /**
     * Container free constructor, need to initialize all dependencies explicitly.
     *
     * @param bspConfig The configuration for connecting with bsp.
     */
    @Inject
    public BSPSampleSearchServiceImpl(BSPConfig bspConfig) {
        this.bspConfig = bspConfig;
    }

    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, bspConfig);
    }

    @Override
    public List<Map<BSPSampleSearchColumn, String>> runSampleSearch(Collection<String> sampleIDs,
                                                                    final BSPSampleSearchColumn... queryColumns) {
        if (queryColumns == null || queryColumns.length == 0) {
            throw new IllegalArgumentException("No query columns supplied!");
        }

        if (sampleIDs == null) {
            return null;
        }

        // Check to see if BSP is supported before trying to get data.
        if (sampleIDs.isEmpty() || !AbstractConfig.isSupported(bspConfig)) {
            return Collections.emptyList();
        }

        final List<Map<BSPSampleSearchColumn, String>> ret = new ArrayList<>();

        String urlString = bspConfig.getWSUrl(SEARCH_RUN_SAMPLE_SEARCH);

        MultivaluedMap<String, String> parameters = new MultivaluedHashMap<>();

        try {
            for (BSPSampleSearchColumn column : queryColumns) {
                parameters.add("columns", column.columnName());
            }

            parameters.add("sample_ids", StringUtils.join(sampleIDs, ","));

            post(urlString, parameters, ExtraTab.TRUE, new PostCallback() {
                @Override
                public void callback(String[] bspData) {
                    Map<BSPSampleSearchColumn, String> newMap = new HashMap<>();

                    // It turns out that BSP truncates the rest of the columns, if there are no more values, which
                    // values "", once i >= the length of the bspData
                    int i = 0;
                    for (BSPSampleSearchColumn column : queryColumns) {
                        newMap.put(column, (i < bspData.length) ? bspData[i++] : "");
                    }

                    ret.add(newMap);
                }
            });
        } catch (WebApplicationException clientException) {
            throw new BSPLookupException("Error connecting to BSP", clientException);
        }

        return ret;
    }
}
