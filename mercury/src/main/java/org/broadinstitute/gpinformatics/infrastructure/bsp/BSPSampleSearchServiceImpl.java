package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

@Impl
public class BSPSampleSearchServiceImpl extends AbstractJerseyClientService implements BSPSampleSearchService {
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
        if (sampleIDs.isEmpty() || AbstractConfig.isSupported(bspConfig)) {
            return Collections.emptyList();
        }

        final List<Map<BSPSampleSearchColumn, String>> ret = new ArrayList<Map<BSPSampleSearchColumn, String>>();

        String urlString = bspConfig.getWSUrl(SEARCH_RUN_SAMPLE_SEARCH);

        List<String> parameters = new ArrayList<String>();

        try {
            for (BSPSampleSearchColumn column : queryColumns) {
                parameters.add("columns=" + URLEncoder.encode(column.columnName(), "UTF-8"));
            }

            parameters.add("sample_ids=" + StringUtils.join(sampleIDs, ","));

            String parameterString = StringUtils.join(parameters, "&");

            post(urlString, parameterString, ExtraTab.TRUE, new PostCallback() {
                @Override
                public void callback(String[] bspData) {
                    Map<BSPSampleSearchColumn, String> newMap = new HashMap<BSPSampleSearchColumn, String>();

                    // It turns out that BSP truncates the rest of the columns, if there are no more values, which
                    // is consistent with what Excel does, so it probably comes from that. SO, need to make all
                    // values "", once i >= the length of the bspData
                    int i = 0;
                    for (BSPSampleSearchColumn column : queryColumns) {
                        newMap.put(column, (i < bspData.length) ? bspData[i++] : "");
                    }

                    ret.add(newMap);
                }
            });
        } catch (ClientHandlerException clientException) {
            throw new RuntimeException("Error connecting to BSP", clientException);
        } catch (UnsupportedEncodingException uex) {
            throw new RuntimeException(uex);
        }

        return ret;
    }
}
