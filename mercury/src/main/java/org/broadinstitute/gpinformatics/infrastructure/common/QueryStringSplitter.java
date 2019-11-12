package org.broadinstitute.gpinformatics.infrastructure.common;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Utility for splitting a list of parameters across multiple requests to avoid exceeding the maximum allowed URL
 * length of a web service.
 */
public class QueryStringSplitter implements Iterator<List<String>>, Iterable<List<String>> {

    private int baseUrlLength;
    private int maxUrlLength;
    private Map<String, List<String>> fixedParameters;

    /**
     * Create a new query string splitter for a URL with the given base and max lengths.
     *
     * @param baseUrlLength    the base length of the URL that the query string will be appended to
     * @param maxUrlLength     the maximum length that the URL should be allowed to be with the appended query string
     */
    public QueryStringSplitter(int baseUrlLength, int maxUrlLength) {
        this(baseUrlLength, maxUrlLength, new HashMap<String, List<String>>());
    }

    /**
     * Create a new query string splitter for a URL with the given base and max lengths and a fixed set of parameters
     * that need to be included with every request.
     *
     * @param baseUrlLength      the base length of the URL that the query string will be appended to
     * @param maxUrlLength       the maximum length that the URL should be allowed to be with the appended query string
     * @param fixedParameters    a fixed set of parameters to be included with every request
     */
    public QueryStringSplitter(int baseUrlLength, int maxUrlLength, Map<String, List<String>> fixedParameters) {
        this.baseUrlLength = baseUrlLength;
        this.maxUrlLength = maxUrlLength;
        this.fixedParameters = fixedParameters;
    }

    private int getParamSize(String name, String... values) {
        List<NameValuePair> nameValuePairs = new ArrayList<>();
        for (String value : values) {
            nameValuePairs.add(new BasicNameValuePair(name, value));
        }
        return URLEncodedUtils.format(nameValuePairs, StandardCharsets.UTF_8).length();
    }

    private int getParamSize(HashMap<String, List<String>> parameters) {
        int size = 0;

        for (Map.Entry<String, List<String>> parameterEntry : parameters.entrySet()) {
            size += getParamSize(parameterEntry.getKey(),
                    parameterEntry.getValue().toArray(new String[parameterEntry.getValue().size()]));
        }
        return size;
    }

    @Override
    public Iterator<List<String>> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public List<String> next() {
        return null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public List<Map<String, List<String>>> split(String name, List<String> values) {
        ArrayList<Map<String, List<String>>> parametersList = new ArrayList<>();
        HashMap<String, List<String>> parameters = makeBaseParameterMap();
        int baseParameterSize = getParamSize(parameters);

        // add in the number of '&'s
        if (baseParameterSize>0){
            baseParameterSize+=parameters.size()*"&".length();
        }

        int currentLength = baseUrlLength + baseParameterSize;
        for (String value : values) {
            // +1 because '?' or '&' should be counted.
            int valueLength = getParamSize(name, value)+"?".length();
            if (baseUrlLength + baseParameterSize + valueLength > maxUrlLength) {
                throw new RuntimeException(String.format("Cannot construct a small enough URL for value '%s'", value));
            }
            if (currentLength + valueLength > maxUrlLength) {
                parametersList.add(parameters);
                parameters = makeBaseParameterMap();
                currentLength = baseUrlLength + baseParameterSize;
            }
            List<String> valueList = parameters.get(name);
            if (valueList == null) {
                valueList = new ArrayList<>();
                parameters.put(name, valueList);
            }
            valueList.add(value);
            currentLength += valueLength;
        }
        parametersList.add(parameters);
        return parametersList;
    }

    private HashMap<String, List<String>> makeBaseParameterMap() {
        return new HashMap<>(fixedParameters);
    }
}
