package org.broadinstitute.gpinformatics.infrastructure.common;

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

    /**
     * Create a new query string splitter for a URL with the given base and max lengths.
     *
     * @param baseUrlLength    the base length of the URL that the query string will be appended to
     * @param maxUrlLength     the maximum length that the URL should be allowed to be with the appended query string
     */
    public QueryStringSplitter(int baseUrlLength, int maxUrlLength) {
        this.baseUrlLength = baseUrlLength;
        this.maxUrlLength = maxUrlLength;
    }

    /**
     * Create a new query string splitter for a URL with the given base and max lengths and a fixed set of parameters
     * that need to be included with every request.
     *
     * TODO: not yet implemented, but declared here to give an idea of what the feature could look like
     *
     * @param baseUrlLength      the base length of the URL that the query string will be appended to
     * @param maxUrlLength       the maximum length that the URL should be allowed to be with the appended query string
     * @param fixedParameters    a fixed set of parameters to be included with every request
     */
    public QueryStringSplitter(int baseUrlLength, int maxUrlLength, Map<String, List<String>> fixedParameters) {
        throw new UnsupportedOperationException("Not yet implemented");
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
        int currentLength = baseUrlLength;
        for (String value : values) {
            int valueLength = "?".length() + name.length() + "=".length() + value.length();
            if (baseUrlLength + valueLength > maxUrlLength) {
                throw new RuntimeException(String.format("Cannot construct a small enough URL for value '%s'", value));
            }
            if (currentLength + valueLength > maxUrlLength) {
                parametersList.add(parameters);
                parameters = makeBaseParameterMap();
                currentLength = baseUrlLength;
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
        return new HashMap<>();
    }
}
