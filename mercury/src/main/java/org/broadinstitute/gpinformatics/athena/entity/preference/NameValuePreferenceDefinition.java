package org.broadinstitute.gpinformatics.athena.entity.preference;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a simple name-value preference.
 */
@XmlRootElement(name = "nameValuePreferenceDefinition")
public class NameValuePreferenceDefinition extends PreferenceDefinition {

    private final JAXBContext context;

    NameValuePreferenceDefinition() throws Exception {
        context = JAXBContext.newInstance(NameValuePreferenceDefinition.class);
    }

    private HashMap<String, List<String>> dataMap = new HashMap<String, List<String>> ();

    @Override
    public void convertPreference(Preference preference) throws Exception {
        // populate the data from the converted preference data.
        dataMap = ((NameValuePreferenceDefinition) convertFromXml(preference.getData())).getDataMap();
    }

    @Override
    protected JAXBContext getContext() throws JAXBException {
        return context;
    }

    @XmlJavaTypeAdapter(NameValueAdapter.class)
    @XmlElement(name = "dataMap")
    public HashMap<String, List<String>> getDataMap() {
        return dataMap;
    }

    public void setDataMap(HashMap<String, List<String>> dataMap) {
        this.dataMap = dataMap;
    }

    public void put(String key, List<String> values) {
        dataMap.put(key, values);
    }

    public void put(String key, String value) {
        dataMap.put(key, Collections.singletonList(value));
    }

    public static class NameValuePreferenceDefinitionCreator implements PreferenceDefinitionCreator {
        @Override
        public PreferenceDefinition create() throws Exception {
            return new NameValuePreferenceDefinition();
        }
    }

    /**
     * This is an adapter to stream the map using the jaxb annotations above. This lets us marshall and unmarshall
     * the data to a string for storage in the preferences table.
     */
    public static class NameValueAdapter extends XmlAdapter<NameValueAdapter.AdaptedMap, HashMap<String, List<String>>> {

        public static class AdaptedMap {
            public List<Entry> entry = new ArrayList<Entry>();
        }

        public static class Entry {
            public String key;
            public List<String> value;
        }

        @Override
        public HashMap<String, List<String>> unmarshal(AdaptedMap adaptedMap) throws Exception {
            HashMap<String, List<String>> map = new HashMap<String, List<String>>();
            for (Entry entry : adaptedMap.entry) {
                map.put(entry.key, entry.value);
            }

            return map;
        }

        @Override
        public AdaptedMap marshal(HashMap<String, List<String>> map) throws Exception {
            AdaptedMap adaptedMap = new AdaptedMap();
            for (Map.Entry<String, List<String>> mapEntry : map.entrySet()) {
                Entry entry = new Entry();
                entry.key = mapEntry.getKey();
                entry.value = mapEntry.getValue();
                adaptedMap.entry.add(entry);
            }

            return adaptedMap;
        }
    }

}
