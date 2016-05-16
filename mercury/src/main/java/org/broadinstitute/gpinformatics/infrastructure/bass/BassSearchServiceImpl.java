/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.bass;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.broadinstitute.gpinformatics.infrastructure.common.QueryStringSplitter;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO.BassResultColumn;

/**
 * This class retrieves data from Bass. Bass can be queried on any of it's {@link BassResultColumn}s except for
 * {@BassResultColumn.id} which must not contain other BassResultColumn types. Other rules may apply,
 * so consult API documentation.
 *
 * @see <a href="https://confluence.broadinstitute.org/display/BASS/Home">Bass Overview</a>
 * @see <a href="https://confluence.broadinstitute.org/display/BASS/Application+Programming+Interface">Bass API Documentation</a>
 * @see <a href="https://bass.broadinstitute.org/list?rpid=RP-200">Example call to Bass WS</a>
 */
@Impl
public class BassSearchServiceImpl extends AbstractJerseyClientService implements BassSearchService {
    public static final String ONLY_IDS_MAY_BE_SPECIFIED = "If querying for IDs, only IDs may be specified.";
    private BassConfig bassConfig;

    public static String ACTION_LIST = "list";

    public BassSearchServiceImpl() {
    }

    @Inject
    public BassSearchServiceImpl(BassConfig bassConfig) {
        this.bassConfig = bassConfig;
    }

    /**
     * Search Bass for records with values matching parameters.
     *
     * @param parameters Map of terms and values to search with.
     * @param fileType
     *
     * @return List of {@link BassDTO} objects matching all criteria in the parameters map.
     */
    @Override
    public List<BassDTO> runSearch(Map<BassResultColumn, List<String>> parameters, BassFileType fileType) {
        String url = bassConfig.getWSUrl(ACTION_LIST);
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        List<String> sampleList = new ArrayList<>();

        for (Map.Entry<BassResultColumn, List<String>> queryPair : parameters.entrySet()) {
            if (queryPair.getKey() == BassResultColumn.sample) {
                sampleList.addAll(queryPair.getValue());
            } else {
                params.put(queryPair.getKey().name(), queryPair.getValue());
            }
        }
        params.putAll(getFileTypeParam(fileType));
        WebResource resource = getJerseyClient().resource(url);
        resource.accept(MediaType.TEXT_PLAIN);

        Set<MultivaluedMap<String, String>> splitParameterMapSet = new HashSet<>();

        // If we are searching for samples use the QueryStringSplitter to ensure the URL length is within limits.
        if (parameters.keySet().contains(BassResultColumn.sample)) {
            int maxUrlSize= BassConfig.BASS_MAX_URL_LENGTH - getParamSize(params);
            QueryStringSplitter splitter = new QueryStringSplitter(url.length(), maxUrlSize);
            for (Map<String, List<String>> splitParams : splitter.split(BassResultColumn.sample.name(), sampleList)) {
                MultivaluedMap<String, String> multiValueMap = new MultivaluedMapImpl(params);
                multiValueMap.putAll(splitParams);
                splitParameterMapSet.add(multiValueMap);
            }
        } else {
            splitParameterMapSet.add(params);
        }
        List<BassDTO> bassResults = new ArrayList<>();
        for (MultivaluedMap<String, String> splitParameterMap : splitParameterMapSet) {
            ClientResponse response = resource.queryParams(splitParameterMap).get(ClientResponse.class);
            if (!response.getClientResponseStatus().equals(ClientResponse.Status.OK)) {
                String errorString = String.format(
                        "Server returned a status code of %d while getting data from Bass. The error message was \"%s\"",
                        response.getClientResponseStatus().getStatusCode(), response.getEntity(String.class));
                throw new InformaticsServiceException(errorString);
            }

            bassResults.addAll(BassResultsParser.parse(response.getEntityInputStream()));
        }
        return bassResults;
    }

    private int getParamSize(MultivaluedMap<String, String> params) {
        List<NameValuePair> nameValuePairs = new ArrayList<>();
        for (Map.Entry<String, List<String>> paramEntry : params.entrySet()) {
            for (String paramValue : paramEntry.getValue()) {
                nameValuePairs.add(new BasicNameValuePair(paramEntry.getKey(), paramValue));
            }
        }
        return URLEncodedUtils.format(nameValuePairs, CharEncoding.UTF_8).length();
    }

    protected MultivaluedMap<String, String> getFileTypeParam(BassFileType fileType) {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        if (fileType == BassFileType.ALL) {
            return params;
        }
        params.put(BassDTO.FILETYPE, Collections.singletonList(fileType.getBassValue()));
        return params;
    }

    /**
     * Search bass for records containing the Research Project ID
     *
     * @param researchProjectId research project to search for.
     * @param fileType
     *
     * @return All records matching the supplied  Research Project ID.
     */
    @Override
    public List<BassDTO> runSearch(String researchProjectId, BassFileType fileType) {
        Map<BassResultColumn, List<String>> parameters = new HashMap<>();
        parameters.put(BassResultColumn.rpid, Collections.singletonList(researchProjectId));
        return runSearch(parameters, fileType);
    }

    /**
     * Search bass for records containing the Research Project ID.
     * The default search restricts the results to bam files.
     *
     * @param researchProjectId research project to search for.
     *
     * @return All records matching the supplied  Research Project ID.
     *
     * @see BassFileType#BAM
     */
    @Override
    public List<BassDTO> runSearch(String researchProjectId) {
        return runSearch(researchProjectId, BassFileType.BAM);
    }

    /**
     * Search bass for records containing the Research Project ID and Collaborator Sample IDs
     * The default search restricts the results to bam files.
     *
     * @param researchProjectId    Research Project ID to search for.
     * @param collaboratorSampleId Collaborator Sample ID to search for.
     *
     * @return All records matching the supplied  Research Project ID.
     */
    @Override
    public List<BassDTO> runSearch(String researchProjectId, String... collaboratorSampleId) {
        Map<BassResultColumn, List<String>> parameters = buildParameterMap(researchProjectId, collaboratorSampleId);
        return runSearch(parameters, BassFileType.BAM);
    }

    /**
     * Build parameter map containing research project ids and collaborator sample ids
     * which can be passed into runSearch.
     *
     * @return Map of search column to values.
     */
    protected Map<BassResultColumn, List<String>> buildParameterMap(String researchProjectId,
                                                                    String... collaboratorSampleId) {
        Map<BassResultColumn, List<String>> parameters = buildParameterMap(collaboratorSampleId);
        parameters.put(BassResultColumn.rpid, Arrays.asList(researchProjectId));
        return parameters;
    }

    /**
     * Build parameter map containing collaborator sample ids which can be passed into runSearch.
     *
     * @return Map of search column to values.
     */
    protected Map<BassResultColumn, List<String>> buildParameterMap(String[] collaboratorSampleId) {
        Map<BassResultColumn, List<String>> parameters = new HashMap<>();
        if (ArrayUtils.isNotEmpty(collaboratorSampleId)) {
            parameters.put(BassResultColumn.sample, Arrays.asList(collaboratorSampleId));
        }
        return parameters;
    }


    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, bassConfig);
    }
}
