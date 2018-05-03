package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.mercury.boundary.zims.BSPLookupException;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Dependent
@Default
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
    private static List<List<String>> chopped(List<String> list, final int maxLength) {
            List<List<String>> parts = new ArrayList<>();
            final int N = list.size();
            for (int i = 0; i < N; i += maxLength) {
                parts.add(new ArrayList<>(list.subList(i, Math.min(N, i + maxLength))));
            }
            return parts;
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

        List<String> parameters = new ArrayList<>();

        try {
            for (BSPSampleSearchColumn column : queryColumns) {
                parameters.add("columns=" + URLEncoder.encode(column.columnName(), "UTF-8"));
            }

            parameters.add("sample_ids=" + StringUtils.join(sampleIDs, ","));

            String parameterString = StringUtils.join(parameters, "&");

            post(urlString, parameterString, ExtraTab.TRUE, new PostCallback() {
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
        } catch (ClientHandlerException clientException) {
            throw new BSPLookupException("Error connecting to BSP", clientException);
        } catch (UnsupportedEncodingException uex) {
            throw new RuntimeException(uex);
        }

        // Sample IDs were provided, BSP service should at minimum return the same set of IDs with blank data.
        // An empty return set represents a service failure
        if(ret.isEmpty()) {
            throw new BSPLookupException("BSP sample service failed");
        }

        return ret;
    }
}
