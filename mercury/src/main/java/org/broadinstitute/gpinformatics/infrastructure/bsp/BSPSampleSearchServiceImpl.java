package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.config.ClientConfig;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Impl
public class BSPSampleSearchServiceImpl extends AbstractJerseyClientService implements
                                                BSPSampleSearchService {

    public static final String SEARCH_RUN_SAMPLE_SEARCH = "search/runSampleSearch";

    private BSPConfig bspConfig;

    /**
     * Container free constructor, need to initialize all dependencies explicitly
     *
     * @param bspConfig
     */
    @Inject
    public BSPSampleSearchServiceImpl(BSPConfig bspConfig) {
        this.bspConfig = bspConfig;
    }

    @Override
    protected void customizeConfig(ClientConfig clientConfig) {
        // no-op
    }

    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, bspConfig);
    }

    @Override
    public List<String[]> runSampleSearch(Collection<String> sampleIDs, BSPSampleSearchColumn... queryColumns) {

        if (queryColumns == null || queryColumns.length == 0) {
            throw new IllegalArgumentException("No query columns supplied!");
        }

        if (sampleIDs == null) {
            return null;
        }
        
        if (sampleIDs.isEmpty()) {
            return Collections.emptyList();
        }

        final List<String[]> ret = new ArrayList<String[]>();

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
                    ret.add(bspData);
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
