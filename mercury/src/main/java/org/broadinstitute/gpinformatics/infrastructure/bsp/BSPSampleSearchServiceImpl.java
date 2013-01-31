package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.config.ClientConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Impl
public class BSPSampleSearchServiceImpl extends AbstractJerseyClientService implements
                                                BSPSampleSearchService {

    private static Log logger = LogFactory.getLog(BSPSampleSearchServiceImpl.class);

    public static final String SEARCH_RUN_SAMPLE_SEARCH = "search/runSampleSearch";

    @Inject
    private BSPConfig bspConfig;


    /**
     * No arg constructor for CDI
     */
    public BSPSampleSearchServiceImpl() {
    }

    /**
     * Container free constructor, need to initialize all dependencies explicitly
     *
     * @param bspConfig
     */
    public BSPSampleSearchServiceImpl(BSPConfig bspConfig) {
        this.bspConfig = bspConfig;
        logger = LogFactory.getLog(BSPSampleSearchServiceImpl.class);
    }


    @Override
    protected void customizeConfig(ClientConfig clientConfig) {
        // noop
    }


    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, bspConfig);
    }


    @Override
    public List<String[]> runSampleSearch(Collection<String> sampleIDs, BSPSampleSearchColumn... queryColumns) {

        if (queryColumns == null || queryColumns.length == 0)
            throw new RuntimeException("No query columns supplied!");

        if (sampleIDs == null)
            return null;
        
        if (sampleIDs.size() == 0)
            return new ArrayList<String[]>();

        final List<String[]> ret = new ArrayList<String[]>();

        String urlString = bspConfig.getWSUrl(SEARCH_RUN_SAMPLE_SEARCH);

        List<String> parameters = new ArrayList<String>();

        try {

            for (BSPSampleSearchColumn column : queryColumns) {
                parameters.add("columns=" + URLEncoder.encode(column.columnName(), "UTF-8"));
            }

            for (String sampleID : sampleIDs) {
                parameters.add("sample_ids=" + sampleID);
            }

            String parameterString = "";
            if (parameters.size() > 0) {
                parameterString = StringUtils.join(parameters, "&");
            }

            post(urlString, parameterString, ExtraTab.TRUE, new PostCallback() {
                @Override
                public void callback(String[] bspData) {
                    ret.add(bspData);
                }
            });

        }
        catch (ClientHandlerException clientException) {
            throw new RuntimeException("Error connecting to BSP",clientException);
        }
        catch (UnsupportedEncodingException uex) {
            throw new RuntimeException(uex);
        }
        catch (IOException iox) {
            throw new RuntimeException(iox);
        }

        return ret;

    }

    @Override
    public List<String[]> runSampleSearch(Collection<String> sampleIDs, List<BSPSampleSearchColumn> resultColumns) {
        BSPSampleSearchColumn [] dummy = new BSPSampleSearchColumn[resultColumns.size()];
        return runSampleSearch(sampleIDs, resultColumns.toArray(dummy));
    }

}
